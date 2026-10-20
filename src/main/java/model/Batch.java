package model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Партия товара с определенным сроком годности. Неизменяемый объект.
 */

public record Batch (
    BatchNumber number,
    Product product,
    LocalDate producedOn,
    LocalDate expiresOn,
    int quantity
) implements Comparable<Batch> {

    /**
     * Компактный конструктор с валидацией.
     */
    public Batch {
        Objects.requireNonNull(number);
        Objects.requireNonNull(product);
        Objects.requireNonNull(producedOn);
        Objects.requireNonNull(expiresOn);
        if (expiresOn.isBefore(producedOn))
            throw new IllegalArgumentException("Дата истечения срока годности ранее даты изготовления.");
        if (quantity <= 0)
            throw new IllegalArgumentException("Количество должно быть положительным.");
    }

    /**
     * Проверяет, просрочена ли партия на указанную дату.
     *
     * @param date дата проверки
     * @return true, если срок годности истек строго до указанной даты
     */
    public boolean isExpiredOn(LocalDate date) {
        return expiresOn.isBefore(date);
    }

    /**
     * Проверяет, истекает ли срок годности в ближайшие N дней.
     *
     * @param date дата отсчета
     * @param days количество дней (окно)
     * @return true, если срок годности попадает в окно [date; date + days]
     */
    public boolean isExpiringWithin(LocalDate date, int days) {
        LocalDate limit = date.plusDays(days);
        return !expiresOn.isBefore(date) && !expiresOn.isAfter(limit);
    }

    /**
     * Сравнивает партии для сортировки по правилу FEFO.
     * Сначала по сроку годности, затем по номеру партии.
     */
    @Override
    public int compareTo(Batch other) {
        int byExpiry = expiresOn.compareTo(other.expiresOn);
        return byExpiry != 0 ? byExpiry : number.value().compareTo(other.number.value());
    }
}
