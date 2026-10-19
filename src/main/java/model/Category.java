package model;

public enum Category {
    FROZEN("Мороженое", TemperatureMode.FROZEN, 180),
    CHILLED("Охлаждённое", TemperatureMode.CHILLED, 5),
    AMBIENT("Комнатное", TemperatureMode.AMBIENT, 365),
    PRODUCE("Овощи и фрукты", TemperatureMode.CHILLED, 14);

    private final String title;
    private final TemperatureMode tempMode;
    private final int defaultStoragePeriod;

    Category(String title, TemperatureMode tempMode, int defaultStoragePeriod) {
        this.title = title;
        this.tempMode = tempMode;
        this.defaultStoragePeriod = defaultStoragePeriod;
    }

    public TemperatureMode tempMode() {
        return  tempMode;
    }

    public int getDefaultStoragePeriod() {
        return  defaultStoragePeriod;
    }

    public String getTitle() {
        return  title;
    }
}
