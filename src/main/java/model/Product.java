package model;

import java.util.Objects;

/**
 * Товар на складе. Неизменяемый объект (record).
 *
 * @param number       уникальный артикул или штрихкод
 * @param name         название товара
 * @param category     категория товара (определяет режим хранения)
 * @param priceKopecks цена в копейках (целое число)
 */
public record Product (
    String number,
    String name,
    Category category,
    long priceKopecks
) {
    /**
     * Компактный конструктор с валидацией.
     */
    public Product {
        Objects.requireNonNull(number);
        Objects.requireNonNull(name);
        Objects.requireNonNull(category);
        if (priceKopecks < 0) throw new IllegalArgumentException("Отрицательная цена.");
    }
}
