package service;

import model.Receipt;
import model.Reservation;
import model.ReservationCancel;
import model.Shipment;

public sealed interface StockOperation
        permits Receipt, Shipment, Reservation, ReservationCancel {
    String describe();
}
