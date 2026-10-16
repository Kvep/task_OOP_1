package model;

import service.StockOperation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Reservation(
        long id,
        Product product,
        int quantity,
        LocalDateTime created,
        LocalDateTime expires,
        LocalDate date
        ) implements StockOperation {

    public Reservation {
        if (product == null) throw new IllegalArgumentException("product == null");
        if (quantity <= 0) throw new IllegalArgumentException("quantity <= 0");
        if (created ==  null || expires == null) throw new IllegalArgumentException("time == null");
        if (expires.isBefore(created)) throw new IllegalArgumentException("expires < created");
        if (date == null) date = created.toLocalDate();
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expires);
    }

    @Override
    public String describe() {
        return "Резерв " + id + " на " + product.name() + ", "
                + quantity + "шт., до" + expires;
    }
}
