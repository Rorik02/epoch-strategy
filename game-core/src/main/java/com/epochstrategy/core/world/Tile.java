
package com.epochstrategy.core.world;

import java.util.Objects;

public class Tile {

    private final int x;
    private final int y;

    private TerrainType terrain;
    private int elevation;

    public Tile(int x, int y,
                TerrainType terrain,
                int elevation) {

        this.x = x;
        this.y = y;
        this.terrain = Objects.requireNonNull(terrain);
        this.elevation = elevation;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TerrainType getTerrain() {
        return terrain;
    }

    public int getElevation() {
        return elevation;
    }

    public boolean isPassableForLandUnits() {
        return terrain.isLandPassable();
    }
}
