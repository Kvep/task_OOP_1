package ru.vsu.cs.course2.task1;

public sealed interface StockOperation
        permits Receipt, Shipment, Reservation, ReservationCancel {
    String describe();
}
