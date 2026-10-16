package model;

import service.StockOperation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationCancel(
    long reservationId,
    LocalDateTime cancelledDate,
    LocalDate date
) implements StockOperation {

    public ReservationCancel {
        if (cancelledDate == null) throw new IllegalArgumentException("cancelledDate == null");
        if (date == null) date = cancelledDate.toLocalDate();
    }

    @Override
    public String describe() {
        return "Отмена резерва " + reservationId;
    }
}
