package model;

import java.util.Objects;

public record BatchNumber(String value) {
    public BatchNumber {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException("Отсутствует номер партии.");
    }
}
