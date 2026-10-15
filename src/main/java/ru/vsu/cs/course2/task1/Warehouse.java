package ru.vsu.cs.course2.task1;

import java.util.*;

public final class Warehouse {
    private final String name;
    private final TemperatureMode zoneMode;
    private final Map<Product, List<Batch>> batches = new HashMap<>();
    private final List<StockOperation> journal = new ArrayList<>();

    public Warehouse(String name, TemperatureMode zoneMode) {
        this.name = Objects.requireNonNull(name, "name");
        this.zoneMode = Objects.requireNonNull(zoneMode, "zoneMode");
    }

    public String name() {
        return name;
    }

    public TemperatureMode getZoneMode() {
        return zoneMode;
    }

    public void receive(Batch batch) throws WarehouseException {
        Objects.requireNonNull(batch, "batch");
        TemperatureMode required = batch.product().category().tempMode();
        if (!zoneMode.isCompatibleWith(required)) {
            throw new IncompatibleTemperatureException(

            )
        }
    }
}
