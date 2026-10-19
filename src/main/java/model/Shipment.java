package model;

import java.time.LocalDate;
import java.util.List;

public record Shipment(Product product, List<Batch> shipped, LocalDate date) implements StockOperation {
    public Shipment {
        if (product == null) throw new IllegalArgumentException("product == null");
        if (date == null) throw  new IllegalArgumentException("date == null");
        shipped = List.copyOf(shipped);
    }

    public int totalQuantity() {
        return shipped.stream().mapToInt(Batch::quantity).sum();
    }

    @Override
    public String describe() {
        return "Отгрузка " + product.name() + ": " + totalQuantity()
                + " шт. из " + shipped.size() + "партий(и)";
    }
}
