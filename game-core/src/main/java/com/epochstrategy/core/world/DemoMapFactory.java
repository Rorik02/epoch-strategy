
package com.epochstrategy.core.world;

public class DemoMapFactory {

    public static WorldMap createDemoMap() {

        WorldMap map = new WorldMap(10, 10);

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {

                TerrainType terrain;
                int elevation;

                boolean isWater =
                        x == 0 || (x == 1 && y < 7);

                boolean isMountain =
                        x >= 4 && x <= 8
                                && y >= 4 && y <= 7;

                boolean isHill =
                        x >= 4
                                && y >= 3 && y <= 6;

                if (isWater) {
                    terrain = TerrainType.OCEAN;
                    elevation = 0;

                } else if (isMountain) {
                    terrain = TerrainType.MOUNTAINS;
                    elevation = 4;

                } else if (isHill) {
                    terrain = TerrainType.HILLS;
                    elevation = 2;

                } else {
                    terrain = TerrainType.PLAINS;
                    elevation = 1;
                }

                Tile tile = new Tile(
                        x,
                        y,
                        terrain,
                        elevation
                );

                map.setTile(tile);
            }
        }

        return map;
    }
}
