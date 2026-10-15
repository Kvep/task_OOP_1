package ru.vsu.cs.course2.task1;

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

    //public boolean is
}
