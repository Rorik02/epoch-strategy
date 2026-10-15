
package com.epochstrategy.core.world;

public enum RiverVesselType {

    SMALL_BOAT(
            0.10f, 0.25f, 1,
            5, 10
    ),

    RIVER_BOAT(
            0.16f, 0.70f, 1,
            4, 40
    ),

    CARGO_BARGE(
            0.24f, 1.40f, 0,
            3, 100
    );

    private final float minWidth;
    private final float minDepth;
    private final int maxElevationDrop;

    private final int movementPointsPerTurn;
    private final int cargoCapacity;

    RiverVesselType(
            float minWidth,
            float minDepth,
            int maxElevationDrop,
            int movementPointsPerTurn,
            int cargoCapacity
    ) {
        this.minWidth = minWidth;
        this.minDepth = minDepth;
        this.maxElevationDrop = maxElevationDrop;
        this.movementPointsPerTurn = movementPointsPerTurn;
        this.cargoCapacity = cargoCapacity;
    }

    public float getMinWidth() {
        return minWidth;
    }

    public float getMinDepth() {
        return minDepth;
    }

    public int getMaxElevationDrop() {
        return maxElevationDrop;
    }

    public int getMovementPointsPerTurn() {
        return movementPointsPerTurn;
    }

    public int getCargoCapacity() {
        return cargoCapacity;
    }
}
