package exception;

public class ReservationTimeoutException extends WarehouseException {
    public ReservationTimeoutException(String message) {
        super(message);
    }
}
