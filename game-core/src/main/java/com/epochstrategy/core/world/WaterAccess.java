package com.epochstrategy.core.world;

public enum WaterAccess {

    NONE(false, false),
    FRESHWATER(true, false),
    COASTAL(false, true),
    BOTH(true, true);

    private final boolean freshWater;
    private final boolean seaAccess;

    WaterAccess(
            boolean freshWater,
            boolean seaAccess
    ) {
        this.freshWater = freshWater;
        this.seaAccess = seaAccess;
    }

    public boolean hasFreshWater() {
        return freshWater;
    }

    public boolean hasSeaAccess() {
        return seaAccess;
    }
}
