
package com.epochstrategy.core.world;

import java.util.Objects;
import java.util.Random;

public final class ProceduralMapGenerator {

    private ProceduralMapGenerator() {
    }

    public static WorldMap generate(
            int width,
            int height,
            long seed
    ) {
        return generate(
                width,
                height,
                seed,
                MapType.CONTINENTS
        );
    }

    public static WorldMap generate(
            int width,
            int height,
            long seed,
            MapType mapType
    ) {

        Objects.requireNonNull(mapType);

        WorldMap map = new WorldMap(width, height);
        Random random = new Random(seed);

        // Generate island positions for archipelago maps
        double[][] islands = new double[15][4];

        for (int i = 0; i < islands.length; i++) {
            islands[i][0] = 0.08 + random.nextDouble() * 0.84;
            islands[i][1] = 0.10 + random.nextDouble() * 0.80;
            islands[i][2] = 0.065 + random.nextDouble() * 0.065;
            islands[i][3] = 0.09 + random.nextDouble() * 0.075;
        }

        double offsetA = random.nextDouble() * 0.08;
        double offsetB = random.nextDouble() * 0.08;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                double nx = (x + 0.5) / width;
                double ny = (y + 0.5) / height;

                double landmass = switch (mapType) {

                    case PANGAEA -> ellipse(
                            nx, ny,
                            0.5, 0.5,
                            0.44, 0.42
                    );

                    case CONTINENTS -> Math.max(
                            ellipse(
                                    nx, ny,
                                    0.27 + offsetA,
                                    0.50,
                                    0.27, 0.38
                            ),
                            ellipse(
                                    nx, ny,
                                    0.70 + offsetB,
                                    0.50,
                                    0.24, 0.34
                            )
                    );

                    case ARCHIPELAGO ->
                            calculateIslandInfluence(
                                    nx, ny, islands
                            );
                };

                double coastlineNoise =
                        noise(x / 5.0, y / 5.0, seed + 100)
                                * 0.17
                                + noise(x / 2.5, y / 2.5, seed + 101)
                                * 0.07;

                double landValue = landmass + coastlineNoise;

                TerrainType terrain;
                int elevation;

                boolean mapEdge =
                        nx < 0.025 || nx > 0.975
                                || ny < 0.025 || ny > 0.975;

                if (landValue < 0.10 || mapEdge) {

                    terrain = TerrainType.OCEAN;
                    elevation = 0;

                } else {

                    double relief =
                            noise(x / 6.0, y / 6.0, seed + 200)
                                    * 0.7
                                    + noise(x / 2.5, y / 2.5, seed + 201)
                                    * 0.3;

                    double vegetation =
                            noise(x / 3.0, y / 3.0, seed + 300);

                    if (relief > 0.32) {
                        terrain = TerrainType.MOUNTAINS;
                        elevation = 4;

                    } else if (relief > 0.12) {
                        terrain = TerrainType.HILLS;
                        elevation = 2;

                    } else if (vegetation > 0.15) {
                        terrain = TerrainType.FOREST;
                        elevation = 1;

                    } else {
                        terrain = TerrainType.PLAINS;
                        elevation = 1;
                    }
                }

                map.setTile(
                        new Tile(x, y, terrain, elevation)
                );
            }
        }

        return map;
    }

    private static double ellipse(
            double x,
            double y,
            double centerX,
            double centerY,
            double radiusX,
            double radiusY
    ) {

        double dx = (x - centerX) / radiusX;
        double dy = (y - centerY) / radiusY;

        return 1.0 - Math.hypot(dx, dy);
    }

    private static double calculateIslandInfluence(
            double x,
            double y,
            double[][] islands
    ) {

        double influence = -1.0;

        for (double[] island : islands) {

            double value = ellipse(
                    x, y,
                    island[0],
                    island[1],
                    island[2],
                    island[3]
            );

            influence = Math.max(influence, value);
        }

        return influence;
    }

    private static double noise(
            double x,
            double y,
            long seed
    ) {

        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);

        double tx = x - x0;
        double ty = y - y0;

        tx = tx * tx * (3 - 2 * tx);
        ty = ty * ty * (3 - 2 * ty);

        double a = randomValue(x0, y0, seed);
        double b = randomValue(x0 + 1, y0, seed);
        double c = randomValue(x0, y0 + 1, seed);
        double d = randomValue(x0 + 1, y0 + 1, seed);

        double top = a + (b - a) * tx;
        double bottom = c + (d - c) * tx;

        return top + (bottom - top) * ty;
    }

    private static double randomValue(
            int x,
            int y,
            long seed
    ) {

        long value = seed
                ^ (x * 0x9E3779B97F4A7C15L)
                ^ (y * 0xC2B2AE3D27D4EB4FL);

        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;

        return (value >>> 11) * 0x1.0p-53 * 2 - 1;
    }
}
