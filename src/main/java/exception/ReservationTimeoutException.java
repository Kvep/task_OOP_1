package exception;

public class ReservationTimeoutException extends WarehouseException {
    private ReservationTimeoutException(String message) {
        super(message);
    }
}
