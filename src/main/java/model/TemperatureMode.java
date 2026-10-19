package model;

public enum TemperatureMode {
    FROZEN("Морозильный", -18),
    CHILLED("Охлаждаемый", 5),
    AMBIENT("Обычный", 20);

    private final String title;
    private final int temperatureCelsius;

    TemperatureMode(String title, int temperatureCelsius) {
        this.title = title;
        this.temperatureCelsius = temperatureCelsius;
    }

    public String getTitle() {
        return title;
    }

    public int getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public boolean isCompatibleWith(TemperatureMode required) {
        return this == required;
    }

    @Override
    public String toString() {
        return title + " (" + temperatureCelsius + "°C)";
    }
}
