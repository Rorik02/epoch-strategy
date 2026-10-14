
package com.epochstrategy.core.technology;

public enum Era {

    ANCIENT("era.ancient", 20),
    MEDIEVAL("era.medieval", 10),
    AGE_OF_DISCOVERY("era.discovery", 5),
    INDUSTRIAL("era.industrial", 2),
    MODERN("era.modern", 1),
    FUTURE("era.future", 1);

    private final String translationKey;
    private final int yearsPerTurn;

    Era(String translationKey, int yearsPerTurn) {
        this.translationKey = translationKey;
        this.yearsPerTurn = yearsPerTurn;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public int getYearsPerTurn() {
        return yearsPerTurn;
    }
}
