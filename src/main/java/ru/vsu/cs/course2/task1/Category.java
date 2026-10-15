package ru.vsu.cs.course2.task1;

public enum Category {
    FROZEN("Мороженое", TemperatureMode.FROZEN, 180);

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
