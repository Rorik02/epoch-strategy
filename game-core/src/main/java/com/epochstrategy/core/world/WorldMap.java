
package com.epochstrategy.core.world;

import java.util.Objects;

public class WorldMap {

    private final int width;
    private final int height;
    private final Tile[][] tiles;

    public WorldMap(int width, int height) {

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Map dimensions must be positive"
            );
        }

        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
    }

    public void setTile(Tile tile) {

        Objects.requireNonNull(tile);

        int x = tile.getX();
        int y = tile.getY();

        validateCoordinates(x, y);

        tiles[x][y] = tile;
    }

    public Tile getTile(int x, int y) {

        validateCoordinates(x, y);

        Tile tile = tiles[x][y];

        if (tile == null) {
            throw new IllegalStateException(
                    "Tile has not been initialized: "
                            + x + ", " + y
            );
        }

        return tile;
    }

    public boolean isInsideMap(int x, int y) {
        return x >= 0 && x < width
                && y >= 0 && y < height;
    }

    private void validateCoordinates(int x, int y) {

        if (!isInsideMap(x, y)) {
            throw new IndexOutOfBoundsException(
                    "Coordinates outside map: "
                            + x + ", " + y
            );
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
