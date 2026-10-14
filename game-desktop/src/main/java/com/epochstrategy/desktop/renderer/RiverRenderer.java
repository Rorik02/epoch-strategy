
package com.epochstrategy.desktop.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import com.epochstrategy.core.world.RiverNetwork;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.WorldMap;

public final class RiverRenderer implements Disposable {

    private static final float RIVER_WIDTH = 0.16f;
    private static final float SURFACE_OFFSET = 0.035f;

    private final Model model;
    private final ModelInstance instance;

    public RiverRenderer(
            WorldMap worldMap,
            RiverNetwork riverNetwork
    ) {

        if (riverNetwork.getSegments().isEmpty()) {
            model = null;
            instance = null;
            return;
        }

        model = buildModel(worldMap, riverNetwork);
        instance = new ModelInstance(model);
    }

    private Model buildModel(
            WorldMap map,
            RiverNetwork network
    ) {

        ModelBuilder builder = new ModelBuilder();
        builder.begin();

        long attributes =
                VertexAttributes.Usage.Position |
                        VertexAttributes.Usage.Normal;

        Material riverMaterial = new Material(
                ColorAttribute.createDiffuse(
                        new Color(0.10f, 0.53f, 0.88f, 1f)
                )
        );

        MeshPartBuilder mesh = builder.part(
                "rivers",
                GL20.GL_TRIANGLES,
                attributes,
                riverMaterial
        );

        for (RiverNetwork.RiverSegment segment
                : network.getSegments()) {

            int fromX = segment.fromX();
            int fromY = segment.fromY();

            int toX = segment.toX();
            int toY = segment.toY();

            if (!map.isInsideMap(fromX, fromY)
                    || !map.isInsideMap(toX, toY)) {
                continue;
            }

            if (Math.abs(toX - fromX)
                    + Math.abs(toY - fromY) != 1) {
                continue;
            }

            float x1 = worldX(map, fromX);
            float z1 = worldZ(map, fromY);

            float x2 = worldX(map, toX);
            float z2 = worldZ(map, toY);

            float midX = (x1 + x2) * 0.5f;
            float midZ = (z1 + z2) * 0.5f;

            // First half: source tile center to shared edge
            addRibbon(
                    mesh,
                    map,
                    x1, z1,
                    midX, midZ
            );

            TerrainType destinationTerrain =
                    map.getTile(toX, toY).getTerrain();

            boolean destinationIsWater =
                    destinationTerrain == TerrainType.OCEAN
                            || destinationTerrain == TerrainType.LAKE;

            // Stop at the shoreline when entering water
            if (!destinationIsWater) {

                addRibbon(
                        mesh,
                        map,
                        midX, midZ,
                        x2, z2
                );
            }
        }

        return builder.end();
    }

    private void addRibbon(
            MeshPartBuilder mesh,
            WorldMap map,
            float startX,
            float startZ,
            float endX,
            float endZ
    ) {

        float dx = endX - startX;
        float dz = endZ - startZ;

        float length = (float) Math.sqrt(
                dx * dx + dz * dz
        );

        if (length < 0.0001f) {
            return;
        }

        float halfWidth = RIVER_WIDTH * 0.5f;

        float sideX = -dz / length * halfWidth;
        float sideZ = dx / length * halfWidth;

        Vector3 a = terrainVertex(
                map,
                startX - sideX,
                startZ - sideZ
        );

        Vector3 b = terrainVertex(
                map,
                startX + sideX,
                startZ + sideZ
        );

        Vector3 c = terrainVertex(
                map,
                endX - sideX,
                endZ - sideZ
        );

        Vector3 d = terrainVertex(
                map,
                endX + sideX,
                endZ + sideZ
        );

        addTriangle(mesh, a, b, c);
        addTriangle(mesh, b, d, c);
    }

    private Vector3 terrainVertex(
            WorldMap map,
            float worldX,
            float worldZ
    ) {

        float height = TerrainMeshBuilder.surfaceHeightAt(
                map,
                worldX,
                worldZ
        );

        return new Vector3(
                worldX,
                height + SURFACE_OFFSET,
                worldZ
        );
    }

    private void addTriangle(
            MeshPartBuilder mesh,
            Vector3 a,
            Vector3 b,
            Vector3 c
    ) {

        MeshPartBuilder.VertexInfo v1 =
                new MeshPartBuilder.VertexInfo()
                        .setPos(a)
                        .setNor(Vector3.Y);

        MeshPartBuilder.VertexInfo v2 =
                new MeshPartBuilder.VertexInfo()
                        .setPos(b)
                        .setNor(Vector3.Y);

        MeshPartBuilder.VertexInfo v3 =
                new MeshPartBuilder.VertexInfo()
                        .setPos(c)
                        .setNor(Vector3.Y);

        mesh.triangle(v1, v2, v3);
    }

    private float worldX(WorldMap map, int x) {
        return x - (map.getWidth() - 1) / 2f;
    }

    private float worldZ(WorldMap map, int y) {
        return y - (map.getHeight() - 1) / 2f;
    }

    public void render(
            ModelBatch modelBatch,
            Environment environment
    ) {

        if (instance != null) {
            modelBatch.render(instance, environment);
        }
    }

    @Override
    public void dispose() {

        if (model != null) {
            model.dispose();
        }
    }
}
