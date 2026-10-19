package app;

import exception.WarehouseException;
import model.*;
import service.Warehouse;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Работа склада\n");

        Warehouse chilledWarehouse = new Warehouse("Охлаждаемый склад", TemperatureMode.CHILLED);
        Warehouse frozenWarehouse = new Warehouse("Морозильный склад", TemperatureMode.FROZEN);

        Product milk = new Product("P-01", "Молоко 3.5%", Category.CHILLED, 9000);
        Product meat = new Product("P-02", "Говядина", Category.CHILLED, 90000);
        Product apple = new Product("P-03", "Яблоки", Category.PRODUCE, 11000);
        Product pelmeni = new Product("P-04", "Пельмени", Category.FROZEN, 40000);

        LocalDate today = LocalDate.now();

        // 1. Приёмка партий, 32
        try {
            System.out.println("1.Приёмка партий");
            for (int i = 1; i <= 10; i++) {
                frozenWarehouse.receive(new Batch(new BatchNumber("PELM-" + i), pelmeni, today.minusDays(i), today.plusDays(7), 100));
            }
            for (int i = 1; i <= 10; i++) {
                chilledWarehouse.receive(new Batch(new BatchNumber("MEAT-" + i), meat, today.minusDays(i), today.plusDays(i * 2), 50));
            }
            for (int i = 1; i <= 10; i++) {
                chilledWarehouse.receive(new Batch(new BatchNumber("MILK-" + i), milk, today.minusDays(10), today.plusDays(i - 5), 20));
            }
            for (int i = 1; i <= 2; i++) {
                chilledWarehouse.receive(new Batch(new BatchNumber("APPL-" + i), apple, today.minusDays(i), today.plusDays(14), 200));
            }
            System.out.println("Принято: " + frozenWarehouse.allBatches().size() + " партий на морозильный склад, "
                    + chilledWarehouse.allBatches().size() + " партий на охлаждаемый.");
            System.out.println();

            // 2. Остаток на дату
            System.out.println("2. Остаток на дату (" + today + ")");
            System.out.println("Мясо: " + chilledWarehouse.stockOn(meat, today) + " шт.");
            System.out.println("Молоко: " + chilledWarehouse.stockOn(milk, today) + " шт.");
            System.out.println();

            // 3. Список просроченного
            System.out.println("3. Список просроченного");
            List<Batch> expired = chilledWarehouse.expiredOn(today);
            System.out.println("Просроченных партий: " + expired.size() + " шт.");
            for (Batch batch : expired) {
                System.out.println(" - " + batch.product().name() + " (партия " + batch.number().value()
                        + ") до " + batch.expiresOn());
            }
            System.out.println();

            // 4. Отгрузка по FEFO
            System.out.println("4. Отгрузка по FEFO (Молоко, 12 шт.)");
            System.out.println("Остаток до отгрузки: " + chilledWarehouse.stockOn(milk, today) + "шт.");
            List<Batch> shipped = chilledWarehouse.shipFefo(milk, 12, today);
            System.out.println("Списано из партий:");
            for (Batch batch : shipped) {
                System.out.println(" - Партия " + batch.number().value() + ": " + batch.quantity()
                        + " шт. (срок до " + batch.expiresOn() + ")");
            }
            System.out.println("Остаток после отгрузки: " + chilledWarehouse.stockOn(milk, today) + "шт.");
            System.out.println();

            // 5. Отчёт "сгорит в ближайшие N дней"
            System.out.println("5. Отчёт \"сгорит в ближайшие 7 дней\"");
            long loss = chilledWarehouse.potentialLossKopecks(today, 7);
            System.out.println("Сумма потенциальных потерь: " + loss + "коп. (" + (loss / 100.0) + " руб.)");
            System.out.println();

            // 6. Резервирование и отгрузка по резерву
            System.out.println("6. Резервирование и отгрузка по резерву");
            Reservation res = chilledWarehouse.reserve(apple, 50, today, Duration.ofDays(2));
            System.out.println("Создан резерв №" + res.id() + " на 50 шт. яблок");
            chilledWarehouse.shipReserved(res.id(), today);
            System.out.println("Резерв успешно отгружен. Активных резервов: " + chilledWarehouse.activeReservations().size());
            System.out.println();

            // 7. Обработка исключений
            System.out.println("7. Обработка исключений");
            try {
                System.out.println("Попытка отгрузить 1000 шт. молока (превышение остатка)");
                chilledWarehouse.shipFefo(milk, 1000, today);
            } catch (WarehouseException e) {
                System.out.println("Ошибка " + e.getMessage());
            }

            try {
                System.out.println("Попытка положить яблоки в морозильник (несовместимость температур)");
                frozenWarehouse.receive(new Batch(new BatchNumber("BAD-1"), apple, today, today.plusDays(5), 10));
            } catch (WarehouseException e) {
                System.out.println("Ошибка " + e.getMessage());
            }

            try {
                System.out.println("Попытка отгрузить резерв после истечения таймаута");
                Reservation res2 = chilledWarehouse.reserve(milk, 10, today, Duration.ofHours(1));
                chilledWarehouse.shipReserved(res2.id(), today.plusDays(2));
            } catch (WarehouseException e) {
                System.out.println("Ошибка " + e.getMessage());
            }

        } catch (WarehouseException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}
