package service;

import exception.ExpiredGoodsException;
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

    public TemperatureMode getZoneMode() {
        return zoneMode;
    }

    /* Принимает партию на склад */

    public void receive(Batch batch) throws WarehouseException {
        Objects.requireNonNull(batch, "batch");
        TemperatureMode required = batch.product().category().tempMode();
        if (!zoneMode.isCompatibleWith(required)) {
            throw new IncompatibleTemperatureException(
                "Партия " + batch.number() + "требует " + required + ", а зона склада '"
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

        int totalAvailable = available.stream().filter(b -> !b.isExpireON(onDate))
                .mapToInt(Batch::quantity).sum();

        if (totalAvailable == 0) {
            throw new InsufficientStockException(
                    "Товар '" + product.name() + "' отсутствует на " + onDate);
            )
        }
        if (totalAvailable < quantity) {
            throw new InsufficientStockException(
                    "Недостаточно товара '" + product.name() + "': запрошено "
            + quantity + ", доступно " + totalAvailable);
        }

        // Сортировка по сроку годности

        List<Batch> sorted = new ArrayList<>(available);
        sorted.sort(Comparator.naturalOrder());

        List<Batch> shipped = new ArrayList<>();
        int remaining = quantity;

        for (Batch batch : sorted) {
            if (remaining == 0) break;
            if (batch.isExpireON(onDate)) {
                throw new ExpiredGoodsException(
                        "Ближайшая партия " + batch.number().value() + " просрочена на "
                                + onDate + " (срок до " + batch.expiresOn() + ")" );
            }

            int take = Math.min(remaining, batch.quantity());

            shipped.add(new Batch(batch.number(), batch.product(), batch.producedOn(),
                    batch.expiresOn(), take));
            remaining -= take;
        }

        // Обновляем состояние: уменьшаем или удаляем партии
        applyShipment(product, shipped, onDate);

        journal.add(new Shipment(product, List.copyOf(shipped), onDate));
        return List.copyOf(shipped);
    }

    private void applyShipment(Product product, List<Batch> shipped, LocalDate onDate) {
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
        return batches.getOrDefault(product, List.of()).stream().filter(b -> !b.isExpireON(date))
                .mapToInt(Batch::quantity).sum();
    }

    //Список партий, просроченных на заданную дату

    public List<Batch> expiredOn(LocalDate date) {
        Objects.requireNonNull(date,"date");
        return batches.values().stream().flatMap(List::stream).filter(b -> b.isExpireON(date))
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
    public Reservation reserve(Product product, int quantity, LocalDate now, Duration ttl) throws WarehouseException {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(ttl, "ttl");
        if (quantity <= 0) {
            throw new WarehouseException("Количество резерва должно быть > 0: " + quantity);
        }
        if (ttl.isNegative() || ttl.isZero()) {
            throw new WarehouseException("TTL должен быть положительным: " + ttl);
        }

        int reserved = reservations.stream().filter(r -> r.product().equals(product))
                .mapToInt(Reservation::quantity).sum();
        int free = stockOn(product, now) - reserved;
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
                created.plus(ttl),
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
            journal.add(new ReservationCancel(r.id(), moment, "истёк таймаут", now));
        }
        return expired.size();
    }

    //Снимает конкретный резерв вручную
    public boolean cancelReservation(long reservationId, LocalDate now) {
        Objects.requireNonNull(now, "now");
        Optional<Reservation> found = reservations.stream().filter(r -> r.id() == reservationId)
                .findFirst();
        if (found.isEmpty()) {
            return false;
        }
        reservations.remove(found.get());
        journal.add(new ReservationCancel(reservationId, now.atStartOfDay(), now));
        return true;
    }

    //Доступ ко всем партиям склада в виде списка
    public List<Batch> allBatches() {
        return batches.values().stream().flatMap(List::stream).sorted().toList();
    }

    //Доступ ко всем активным разервам в виде списка
    public List<Reservation> activeReservation() {
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
