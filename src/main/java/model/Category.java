package model;

/**
 * Категория товара. Содержит требуемый температурный режим и
 * срок хранения по умолчанию.
 */
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

    /**
     * @return требуемый температурный режим для товаров этой категории
     */
    public TemperatureMode getTempMode() {
        return  tempMode;
    }

    /**
     * @return срок хранения по умолчанию в днях
     */
    public int getDefaultStoragePeriod() {
        return  defaultStoragePeriod;
    }

    /**
     * @return человекочитаемое название категории
     */
    public String getTitle() {
        return  title;
    }
}
