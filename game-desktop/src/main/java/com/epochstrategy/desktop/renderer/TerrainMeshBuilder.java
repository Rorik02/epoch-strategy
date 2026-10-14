
package com.epochstrategy.desktop.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;

public final class TerrainMeshBuilder {

    private TerrainMeshBuilder() {
    }

    public static float surfaceHeight(Tile tile) {

        if (tile.getTerrain() == TerrainType.OCEAN
                || tile.getTerrain() == TerrainType.LAKE) {
            return 0.12f;
        }

        return 0.2f
                + Math.max(0, tile.getElevation() - 1) * 0.35f;
    }


    public static Model buildChunk(
            WorldMap map,
            int startX,
            int startY,
            int chunkSize
    ) {

        if (chunkSize <= 0) {
            throw new IllegalArgumentException(
                    "Chunk size must be positive"
            );
        }

        if (!map.isInsideMap(startX, startY)) {
            throw new IllegalArgumentException(
                    "Chunk starts outside the map"
            );
        }

        int endX = Math.min(
                startX + chunkSize,
                map.getWidth()
        );

        int endY = Math.min(
                startY + chunkSize,
                map.getHeight()
        );

        ModelBuilder builder = new ModelBuilder();
        builder.begin();

        long attributes =
                VertexAttributes.Usage.Position |
                        VertexAttributes.Usage.Normal |
                        VertexAttributes.Usage.ColorUnpacked;

        MeshPartBuilder mesh = builder.part(
                "terrain_" + startX + "_" + startY,
                GL20.GL_TRIANGLES,
                attributes,
                new Material(
                        ColorAttribute.createDiffuse(Color.WHITE)
                )
        );

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {

                Tile tile = map.getTile(x, y);

                Vector3[] vertices =
                        getTileVertices(map, tile);

                Color color = terrainColor(
                        tile.getTerrain()
                );

                addTriangle(
                        mesh,
                        vertices[0],
                        vertices[4],
                        vertices[1],
                        color
                );

                addTriangle(
                        mesh,
                        vertices[1],
                        vertices[4],
                        vertices[2],
                        color
                );

                addTriangle(
                        mesh,
                        vertices[2],
                        vertices[4],
                        vertices[3],
                        color
                );

                addTriangle(
                        mesh,
                        vertices[3],
                        vertices[4],
                        vertices[0],
                        color
                );
            }
        }

        return builder.end();
    }


    private static float cornerHeight(
            WorldMap map,
            int cornerX,
            int cornerY
    ) {

        float sum = 0f;
        int count = 0;

        for (int x = cornerX - 1; x <= cornerX; x++) {
            for (int y = cornerY - 1; y <= cornerY; y++) {

                if (map.isInsideMap(x, y)) {

                    sum += surfaceHeight(map.getTile(x, y));
                    count++;
                }
            }
        }

        return count == 0 ? 0f : sum / count;
    }

    private static void addTriangle(
            MeshPartBuilder mesh,
            Vector3 a,
            Vector3 b,
            Vector3 c,
            Color color
    ) {

        Vector3 normal = new Vector3(b)
                .sub(a)
                .crs(new Vector3(c).sub(a))
                .nor();

        MeshPartBuilder.VertexInfo v1 =
                new MeshPartBuilder.VertexInfo()
                        .setPos(a)
                        .setNor(normal)
                        .setCol(color);

        MeshPartBuilder.VertexInfo v2 =
                new MeshPartBuilder.VertexInfo()
                        .setPos(b)
                        .setNor(normal)
                        .setCol(color);

        MeshPartBuilder.VertexInfo v3 =
                new MeshPartBuilder.VertexInfo()
                        .setPos(c)
                        .setNor(normal)
                        .setCol(color);

        mesh.triangle(v1, v2, v3);
    }

    private static Color terrainColor(TerrainType terrain) {

        return switch (terrain) {

            case PLAINS ->
                    new Color(0.30f, 0.58f, 0.25f, 1f);

            case FOREST ->
                    new Color(0.11f, 0.35f, 0.17f, 1f);

            case HILLS ->
                    new Color(0.47f, 0.50f, 0.29f, 1f);

            case MOUNTAINS ->
                    new Color(0.46f, 0.44f, 0.40f, 1f);

            case OCEAN, LAKE ->
                    new Color(0.12f, 0.34f, 0.72f, 1f);
        };
    }


    public static Vector3[] getTileVertices(
            WorldMap map,
            Tile tile
    ) {

        int x = tile.getX();
        int y = tile.getY();

        float worldX =
                x - (map.getWidth() - 1) / 2f;

        float worldZ =
                y - (map.getHeight() - 1) / 2f;

        Vector3 nw = new Vector3(
                worldX - 0.5f,
                cornerHeight(map, x, y),
                worldZ - 0.5f
        );

        Vector3 ne = new Vector3(
                worldX + 0.5f,
                cornerHeight(map, x + 1, y),
                worldZ - 0.5f
        );

        Vector3 se = new Vector3(
                worldX + 0.5f,
                cornerHeight(map, x + 1, y + 1),
                worldZ + 0.5f
        );

        Vector3 sw = new Vector3(
                worldX - 0.5f,
                cornerHeight(map, x, y + 1),
                worldZ + 0.5f
        );

        float centerHeight = surfaceHeight(tile);

        if (tile.getTerrain() == TerrainType.MOUNTAINS) {
            centerHeight += 0.55f;
        }

        Vector3 center = new Vector3(
                worldX,
                centerHeight,
                worldZ
        );

        return new Vector3[]{
                nw, ne, se, sw, center
        };
    }

    public static float surfaceHeightAt(
            WorldMap map,
            float worldX,
            float worldZ
    ) {

        int tileX = Math.round(
                worldX + (map.getWidth() - 1) / 2f
        );

        int tileY = Math.round(
                worldZ + (map.getHeight() - 1) / 2f
        );

        tileX = Math.max(
                0,
                Math.min(tileX, map.getWidth() - 1)
        );

        tileY = Math.max(
                0,
                Math.min(tileY, map.getHeight() - 1)
        );

        Tile tile = map.getTile(tileX, tileY);

        Vector3[] vertices = getTileVertices(map, tile);

        int[][] triangles = {
                {0, 4, 1},
                {1, 4, 2},
                {2, 4, 3},
                {3, 4, 0}
        };

        for (int[] triangle : triangles) {

            Vector3 a = vertices[triangle[0]];
            Vector3 b = vertices[triangle[1]];
            Vector3 c = vertices[triangle[2]];

            float denominator =
                    (b.z - c.z) * (a.x - c.x)
                            + (c.x - b.x) * (a.z - c.z);

            if (Math.abs(denominator) < 0.000001f) {
                continue;
            }

            float weightA = (
                    (b.z - c.z) * (worldX - c.x)
                            + (c.x - b.x) * (worldZ - c.z)
            ) / denominator;

            float weightB = (
                    (c.z - a.z) * (worldX - c.x)
                            + (a.x - c.x) * (worldZ - c.z)
            ) / denominator;

            float weightC = 1f - weightA - weightB;

            if (weightA >= -0.0001f
                    && weightB >= -0.0001f
                    && weightC >= -0.0001f) {

                return weightA * a.y
                        + weightB * b.y
                        + weightC * c.y;
            }
        }

        return surfaceHeight(tile);
    }

}
