package service;

import model.Product;

import java.time.LocalDate;
import java.util.Objects;

public record BatchNumber(String value) {
    public BatchNumber {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException("Отсутствует номер партии.");
    }
}

public record Batch (
    BatchNumber number,
    Product product,
    LocalDate producedOn,
    LocalDate expiresOn,
    int quantity
) implements Comparable<Batch> {

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

    public boolean isExpireON(LocalDate date) {
        return expiresOn.isBefore(date);
    }

    public boolean isExpiringWithin(LocalDate date, int days) {
        LocalDate limit = date.plusDays(days);
        return !expiresOn.isBefore(date) && !expiresOn.isAfter(limit);
    }

    @Override
    public int compareTo(Batch other) {
        int byExpiry = expiresOn.compareTo(other.expiresOn);
        return byExpiry != 0 ? byExpiry : number.value().compareTo(other.number.value());
    }
}
