
package com.epochstrategy.core.world;

public final class DrainageMap {

    private final int[][] distanceToWater;
    private final int[][] downstreamX;
    private final int[][] downstreamY;
    private final int[][] catchmentArea;

    DrainageMap(
            int[][] distanceToWater,
            int[][] downstreamX,
            int[][] downstreamY,
            int[][] catchmentArea
    ) {
        this.distanceToWater = distanceToWater;
        this.downstreamX = downstreamX;
        this.downstreamY = downstreamY;
        this.catchmentArea = catchmentArea;
    }

    public int getDistanceToWater(int x, int y) {
        return distanceToWater[x][y];
    }

    public int getDownstreamX(int x, int y) {
        return downstreamX[x][y];
    }

    public int getDownstreamY(int x, int y) {
        return downstreamY[x][y];
    }

    public int getCatchmentArea(int x, int y) {
        return catchmentArea[x][y];
    }

    public boolean hasDrainagePath(int x, int y) {
        return distanceToWater[x][y] > 0;
    }
}
