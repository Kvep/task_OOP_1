package model;

public sealed interface StockOperation
        permits Receipt, Shipment, Reservation, ReservationCancel {
    String describe();
}
