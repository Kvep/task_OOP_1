package model;

import java.time.LocalDate;

public record Receipt(Batch batch, LocalDate date) implements StockOperation {

    public Receipt {
        if (batch == null) throw new IllegalArgumentException("batch == null");
        if (date == null) throw new IllegalArgumentException("date == null");
    }

    @Override
    public String describe() {
        return "Приём партии " + batch.number().value() + " (" + batch.product().name() + "), "
                + batch.quantity() + " шт., срок до " + batch.expiresOn();
    }
}
