
package com.epochstrategy.core.unit;

public enum LandUnitType {

    SETTLER(2);

    private final int maxMovementPoints;

    LandUnitType(int maxMovementPoints) {
        this.maxMovementPoints = maxMovementPoints;
    }

    public int getMaxMovementPoints() {
        return maxMovementPoints;
    }
}
