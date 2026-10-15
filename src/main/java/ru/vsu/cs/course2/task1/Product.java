package ru.vsu.cs.course2.task1;

import java.util.Objects;

public record Product (
    String number,
    String name,
    Category category,
    long priceKopecks
) {
    public Product {
        Objects.requireNonNull(number);
        Objects.requireNonNull(name);
        Objects.requireNonNull(category);
        if (priceKopecks < 0) throw new IllegalArgumentException("Отрицательная цена.");
    }
}
