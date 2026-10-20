package service;

import exception.IncompatibleTemperatureException;
import exception.InsufficientStockException;
import exception.WarehouseException;
import model.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public final class Warehouse {
    private final String name;
    private final TemperatureMode zoneMode;
    private final Map<Product, List<Batch>> batches = new HashMap<>();
    private final List<StockOperation> journal = new ArrayList<>();
    private final List<Reservation> reservations =  new ArrayList<>();
    private long nextReservationId = 1L;

    public Warehouse(String name, TemperatureMode zoneMode) {
        this.name = Objects.requireNonNull(name, "name");
        this.zoneMode = Objects.requireNonNull(zoneMode, "zoneMode");
    }

    public String name() {
        return name;
    }

    public TemperatureMode zoneMode() {
        return zoneMode;
    }

    /* Принимает партию на склад */

    public void receive(Batch batch) throws WarehouseException {
        Objects.requireNonNull(batch, "batch");
        TemperatureMode required = batch.product().category().getTempMode();
        if (!zoneMode.isCompatibleWith(required)) {
            throw new IncompatibleTemperatureException(
                "Партия " + batch.number().value() + " требует " + required + ", а зона склада '"
                        + name + "' - " + zoneMode);
        }

        batches.computeIfAbsent(batch.product(), p -> new ArrayList<>()).add(batch);
        journal.add(new Receipt(batch, LocalDate.now()));
    }

    /* Отгрузка по FEFO */
    public List<Batch> shipFefo(Product product, int quantity, LocalDate onDate) throws WarehouseException {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(onDate, "onDate");
        if (quantity <= 0) {
            throw new WarehouseException("Количество отгрузки должно быть > 0: " + quantity);
        }

        List<Batch> available = batches.getOrDefault(product, List.of());

        int freshProducts = available.stream().filter(b -> !b.isExpiredOn(onDate))
                .mapToInt(Batch::quantity).sum();

        if (freshProducts == 0) { // Товар отсутствует
            throw new InsufficientStockException(
                    "Товар '" + product.name() + "' отсутствует на " + onDate);
        }
        if (freshProducts < quantity) {
            throw new InsufficientStockException(
                    "Недостаточно товара '" + product.name() + "': запрошено "
            + quantity + ", доступно " + freshProducts);
        }

        // Сортировка по сроку годности

        List<Batch> sorted = new ArrayList<>(available);
        sorted.sort(Comparator.naturalOrder());

        List<Batch> shipped = new ArrayList<>();
        int remaining = quantity;

        for (Batch batch : sorted) {
            if (remaining == 0) break;
            if (batch.isExpiredOn(onDate)) {
                continue; // просроченное
            }

            int take = Math.min(remaining, batch.quantity());

            shipped.add(new Batch(batch.number(), batch.product(), batch.producedOn(),
                    batch.expiresOn(), take));
            remaining -= take;
        }

        // Обновляем состояние: уменьшаем или удаляем партии
        applyShipment(product, shipped);

        journal.add(new Shipment(product, List.copyOf(shipped), onDate));
        return List.copyOf(shipped);
    }

    // Метод отгрузки по резерву
    public void shipReserved(long reservationId, LocalDate now) throws WarehouseException {
        Reservation r = reservations.stream().filter(x -> x.id() == reservationId)
                .findFirst().orElseThrow(() -> new WarehouseException("Резерв #" + reservationId + " не найден"));

        if (r.isExpired(now.atStartOfDay())) {
            throw new exception.ReservationTimeoutException("Резерв #" + reservationId + " истёк " + r.expires());
        }

        if (stockOn(r.product(), now) < r.quantity()) {
            reservations.remove(r); // Резерв больше не действителен
            throw new InsufficientStockException("Невозможно выполнить резерв #" + reservationId + ": на складе не хватает товара.");
        }

        // Снимаем резерв и отгружаем
        reservations.remove(r);
        shipFefo(r.product(), r.quantity(), now);
    }

    private void applyShipment(Product product, List<Batch> shipped) {
        List<Batch> stock = batches.get(product);
        for (Batch taken : shipped) {
            Batch source = stock.stream().filter(b -> b.number().equals(taken.number()))
                    .findFirst().orElseThrow();
            int left = source.quantity() - taken.quantity();
            stock.remove(source);
            if (left > 0) {
                stock.add(new Batch(source.number(), source.product(),
                        source.producedOn(), source.expiresOn(), left));
            }
        }
        if (stock.isEmpty()) {
            batches.remove(product);
        }
    }

    //Остаток товара на заданную дату (без учёта просроченных)
    public int stockOn(Product product, LocalDate date) {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(date, "date");
        return batches.getOrDefault(product, List.of()).stream().filter(b -> !b.isExpiredOn(date))
                .mapToInt(Batch::quantity).sum();
    }

    //Список партий, просроченных на заданную дату

    public List<Batch> expiredOn(LocalDate date) {
        Objects.requireNonNull(date,"date");
        return batches.values().stream().flatMap(List::stream).filter(b -> b.isExpiredOn(date))
                .sorted().toList();
    }

    // Партии товара, срок годности которых истекает в ближайшие дни
    public List<Batch> expiringWithin(Product product, LocalDate date, int days) {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(date, "date");
        if (days < 0) {
            throw new IllegalArgumentException("days < 0: " + days);
        }
        return batches.getOrDefault(product, List.of()).stream()
                .filter(b -> b.isExpiringWithin(date, days))
                .sorted().toList();
    }

    // Отчёт «сгорит в ближайшие N дней»: сумма потерь в копейках по всем товарам склада
    public long potentialLossKopecks(LocalDate date, int days) {
        Objects.requireNonNull(date, "date");
        if (days < 0) {
            throw new IllegalArgumentException("days < 0: " + days);
        }
        long total = 0L;
        for (Batch batch : batches.values().stream().flatMap(List::stream).toList()) {
            if (batch.isExpiringWithin(date, days)) {
                total += (long) batch.quantity() * batch.product().priceKopecks();
            }
        }
        return total;
    }

    //Резервирует указанное количество товара под заказ с таймаутом
    public Reservation reserve(Product product, int quantity, LocalDate now, Duration time) throws WarehouseException {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(time, "time");
        if (quantity <= 0) {
            throw new WarehouseException("Количество резерва должно быть > 0: " + quantity);
        }
        if (time.isNegative() || time.isZero()) {
            throw new WarehouseException("Время должно быть положительным: " + time);
        }

        int reserved = reservations.stream().filter(r -> r.product().equals(product))
                .mapToInt(Reservation::quantity).sum(); // сколько этого товара уже зарезервировано другими клиентами
        int free = stockOn(product, now) - reserved; // свободный остаток, который можно зарезервировать
        if (free < quantity) {
            throw new InsufficientStockException(
                    "Недостаточно свободного остатка '" + product.name()
                            + "': запрошено " + quantity + ", доступно " + free);
        }

        LocalDateTime created = now.atStartOfDay();
        Reservation r = new Reservation(
                nextReservationId++,
                product,
                quantity,
                created,
                created.plus(time),
                now);
        reservations.add(r);
        journal.add(r);
        return r;
    }

    //Снимает резервы, срок которых истёк
    public int cancelExpiredReservations(LocalDate now) {
        Objects.requireNonNull(now, "now");
        LocalDateTime moment = now.atStartOfDay();
        List<Reservation> expired = reservations.stream().filter(r -> r.isExpired(moment)).toList();

        for (Reservation r : expired) {
            reservations.remove(r);
            journal.add(new ReservationCancel(r.id(), moment, now));
        }
        return expired.size();
    }

    //Снимает конкретный резерв вручную
    public void cancelReservation(long reservationId, LocalDate now) throws WarehouseException {
        Objects.requireNonNull(now, "now");
        Optional<Reservation> found = reservations.stream().filter(r -> r.id() == reservationId)
                .findFirst();
        if (found.isEmpty()) {
            throw new WarehouseException("Резерв #" + reservationId + " не найден");
        }
        reservations.remove(found.get());
        journal.add(new ReservationCancel(reservationId, now.atStartOfDay(), now));
    }

    //Доступ ко всем партиям склада в виде списка
    public List<Batch> allBatches() {
        return batches.values().stream().flatMap(List::stream).sorted().toList();
    }

    //Доступ ко всем активным разервам в виде списка
    public List<Reservation> activeReservations() {
        return List.copyOf(reservations);
    }

    //журнал операций в виде списка
    public List<StockOperation> journal() {
        return List.copyOf(journal);
    }

    @Override
    public String toString() {
        return "Warehouse{" + name + ", " + zoneMode + ", партий: " + allBatches().size() + "}";
    }
}
