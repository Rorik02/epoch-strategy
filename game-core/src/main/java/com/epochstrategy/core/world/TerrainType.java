package com.epochstrategy.core.world;

public enum TerrainType {

    PLAINS(1, true),
    FOREST(2, true),
    HILLS(2, true),
    MOUNTAINS(-1, false),
    LAKE(-1, false),
    OCEAN(-1, false);

    private final int movementCost;
    private final boolean landPassable;

    TerrainType(int movementCost, boolean landPassable) {
        this.movementCost = movementCost;
        this.landPassable = landPassable;
    }

    public int getMovementCost() {
        return movementCost;
    }

    public boolean isLandPassable() {
        return landPassable;
    }
}
