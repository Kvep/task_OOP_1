package service;

import exception.*;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WarehouseTest {

    private Warehouse warehouse;
    private Product product;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse("Тестовый", TemperatureMode.CHILLED);
        product = new Product("P1", "Йогурт", Category.CHILLED, 10000);
        today = LocalDate.of(2023, 10, 1);
    }

    // 1. Приёмка
    @Test
    void testReceiveBatch() throws WarehouseException {
        Batch batch = new Batch(new BatchNumber("B1"), product, today, today.plusDays(10), 50);
        warehouse.receive(batch);
        assertEquals(50, warehouse.stockOn(product, today));
    }

    // 2. Несовместимость температур (исключение)
    @Test
    void testReceiveIncompatibleTemperature() {
        Warehouse frozenWh = new Warehouse("Мороз", TemperatureMode.FROZEN);
        Batch batch = new Batch(new BatchNumber("B1"), product, today, today.plusDays(10), 50);
        assertThrows(IncompatibleTemperatureException.class, () -> frozenWh.receive(batch));
    }

    // 3. Отгрузка FEFO
    @Test
    void testShipFefo() throws WarehouseException {
        Batch early = new Batch(new BatchNumber("B1"), product, today, today.plusDays(5), 30);
        Batch late = new Batch(new BatchNumber("B2"), product, today, today.plusDays(10), 50);
        warehouse.receive(early);
        warehouse.receive(late);

        List<Batch> shipped = warehouse.shipFefo(product, 40, today);
        assertEquals(2, shipped.size());
        assertEquals("B1", shipped.get(0).number().value()); // Сначала ушла та, что портится раньше
        assertEquals(30, shipped.get(0).quantity());
        assertEquals(10, shipped.get(1).quantity());
        assertEquals(40, warehouse.stockOn(product, today)); // 80 - 40 = 40
    }

    // 4. Отгрузка сверх остатка (исключение)
    @Test
    void testShipFefoInsufficientStock() throws WarehouseException {
        Batch batch = new Batch(new BatchNumber("B1"), product, today, today.plusDays(10), 10);
        warehouse.receive(batch);
        assertThrows(InsufficientStockException.class, () -> warehouse.shipFefo(product, 20, today));
    }

    // 5. Остаток не считает просроченное
    @Test
    void testStockOnDateIgnoresExpired() throws WarehouseException {
        Batch expired = new Batch(new BatchNumber("B1"), product, today.minusDays(20), today.minusDays(1), 50);
        Batch fresh = new Batch(new BatchNumber("B2"), product, today, today.plusDays(10), 30);
        warehouse.receive(expired);
        warehouse.receive(fresh);
        assertEquals(30, warehouse.stockOn(product, today));
    }

    // 6. Поиск просроченного
    @Test
    void testExpiredOn() throws WarehouseException {
        Batch b1 = new Batch(new BatchNumber("B1"), product, today.minusDays(10), today.minusDays(1), 50);
        Batch b2 = new Batch(new BatchNumber("B2"), product, today, today.plusDays(5), 30);
        warehouse.receive(b1);
        warehouse.receive(b2);

        List<Batch> expired = warehouse.expiredOn(today);
        assertEquals(1, expired.size());
        assertEquals("B1", expired.get(0).number().value());
    }

    // 7. Отчет "сгорит в ближайшие N дней"
    @Test
    void testExpiringWithin() throws WarehouseException {
        Batch b1 = new Batch(new BatchNumber("B1"), product, today, today.plusDays(3), 10);
        Batch b2 = new Batch(new BatchNumber("B2"), product, today, today.plusDays(10), 20);
        warehouse.receive(b1);
        warehouse.receive(b2);

        List<Batch> expiring = warehouse.expiringWithin(product, today, 5);
        assertEquals(1, expiring.size());
        assertEquals("B1", expiring.get(0).number().value());
    }

    // 8. Резервирование уменьшает свободный остаток
    @Test
    void testReserveAndCancel() throws WarehouseException {
        Batch batch = new Batch(new BatchNumber("B1"), product, today, today.plusDays(10), 50);
        warehouse.receive(batch);

        Reservation res = warehouse.reserve(product, 20, today, Duration.ofDays(2));
        assertEquals(1, warehouse.activeReservations().size());

        // Пытаемся зарезервировать больше, чем осталось свободного (50 - 20 = 30)
        assertThrows(InsufficientStockException.class, () -> warehouse.reserve(product, 40, today, Duration.ofDays(2)));

        warehouse.cancelReservation(res.id(), today);
        assertEquals(0, warehouse.activeReservations().size());
    }

    // 9. Отмена резерва по таймауту
    @Test
    void testCancelExpiredReservations() throws WarehouseException {
        Batch batch = new Batch(new BatchNumber("B1"), product, today, today.plusDays(10), 50);
        warehouse.receive(batch);

        warehouse.reserve(product, 20, today, Duration.ofDays(2));

        LocalDate future = today.plusDays(3); // Сдвигаем время вперед
        int cancelled = warehouse.cancelExpiredReservations(future);

        assertEquals(1, cancelled);
        assertEquals(0, warehouse.activeReservations().size());
    }

    // 10. Исключение при попытке отгрузить просроченный резерв
    @Test
    void testShipReservedTimeout() throws WarehouseException {
        Batch batch = new Batch(new BatchNumber("B1"), product, today, today.plusDays(10), 50);
        warehouse.receive(batch);

        Reservation res = warehouse.reserve(product, 20, today, Duration.ofDays(1));

        // Пытаемся отгрузить через 2 дня (таймаут 1 день)
        assertThrows(ReservationTimeoutException.class, () -> warehouse.shipReserved(res.id(), today.plusDays(2)));
    }

    // 11. Контракт equals/hashCode
    @Test
    void testBatchEqualsAndHashCode() {
        BatchNumber num1 = new BatchNumber("A-1");
        BatchNumber num2 = new BatchNumber("A-1");
        BatchNumber num3 = new BatchNumber("A-2");

        assertEquals(num1, num2);
        assertEquals(num1.hashCode(), num2.hashCode());
        assertNotEquals(num1, num3);
    }

    // 12. Граничные случаи (создание невалидных объектов)
    @Test
    void testInvalidBatchCreation() {
        // Срок годности раньше даты производства
        assertThrows(IllegalArgumentException.class,
                () -> new Batch(new BatchNumber("B1"), product, today, today.minusDays(1), 10));

        // Отрицательное количество
        assertThrows(IllegalArgumentException.class,
                () -> new Batch(new BatchNumber("B1"), product, today, today.plusDays(1), -5));
    }

    // 13. Инкапсуляция (нельзя изменить внутренний список)
    @Test
    void testEncapsulation() throws WarehouseException {
        Batch batch = new Batch(new BatchNumber("B1"), product, today, today.plusDays(10), 50);
        warehouse.receive(batch);

        List<Batch> batches = warehouse.allBatches();
        assertThrows(UnsupportedOperationException.class, () -> batches.add(batch));
    }
}