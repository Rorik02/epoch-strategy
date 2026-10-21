
package com.epochstrategy.desktop.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import com.epochstrategy.core.civilization.Civilization;
import com.epochstrategy.core.civilization.CivilizationManager;
import com.epochstrategy.core.territory.TerritoryManager;
import com.epochstrategy.core.territory.TerritoryManager.TerritoryClaim;
import com.epochstrategy.core.world.WorldMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class TerritoryBorderRenderer
        implements Disposable {

    private static final float LINE_THICKNESS = 0.065f;
    private static final float HEIGHT_OFFSET = 0.10f;

    // Place the line slightly inside its owner's tile.
    private static final float BORDER_INSET = 0.065f;

    // Avoid floating-point ambiguity at tile corners.
    private static final float HALF_EDGE = 0.49f;

    // More segments help borders follow uneven terrain.
    private static final int SEGMENTS_PER_EDGE = 4;

    private final WorldMap worldMap;

    private final Model lineModel;

    private final Array<ModelInstance> instances =
            new Array<>();

    private final Map<Long, Color> civilizationColors =
            new HashMap<>();

    public TerritoryBorderRenderer(
            WorldMap worldMap
    ) {

        this.worldMap =
                Objects.requireNonNull(worldMap);

        ModelBuilder builder = new ModelBuilder();

        long attributes =
                VertexAttributes.Usage.Position
                        | VertexAttributes.Usage.Normal;

        lineModel = builder.createCylinder(
                1f,
                1f,
                1f,
                6,
                new Material(
                        ColorAttribute.createDiffuse(
                                Color.WHITE
                        )
                ),
                attributes
        );
    }

    public void refresh(
            TerritoryManager territoryManager,
            CivilizationManager civilizationManager
    ) {

        Objects.requireNonNull(
                territoryManager,
                "territoryManager"
        );

        Objects.requireNonNull(
                civilizationManager,
                "civilizationManager"
        );

        instances.clear();
        civilizationColors.clear();

        // Prepare colors once instead of looking them up
        // for every border segment.
        for (Civilization civilization :
                civilizationManager.getCivilizations()) {

            civilizationColors.put(
                    civilization.getId(),
                    convertRgb(
                            civilization.getColorRgb()
                    )
            );
        }

        for (TerritoryClaim claim :
                territoryManager.getClaims()) {

            if (!worldMap.isInsideMap(
                    claim.x(),
                    claim.y()
            )) {
                throw new IllegalStateException(
                        "Territory claim outside map"
                );
            }

            Color color = civilizationColors.get(
                    claim.civilizationId()
            );

            if (color == null) {
                throw new IllegalStateException(
                        "Missing civilization: "
                                + claim.civilizationId()
                );
            }

            int x = claim.x();
            int y = claim.y();

            // NORTH
            if (isBorder(
                    territoryManager,
                    claim,
                    x,
                    y - 1
            )) {

                addBorderEdge(
                        x, y,
                        Edge.NORTH,
                        color
                );
            }

            // SOUTH
            if (isBorder(
                    territoryManager,
                    claim,
                    x,
                    y + 1
            )) {

                addBorderEdge(
                        x, y,
                        Edge.SOUTH,
                        color
                );
            }

            // WEST
            if (isBorder(
                    territoryManager,
                    claim,
                    x - 1,
                    y
            )) {

                addBorderEdge(
                        x, y,
                        Edge.WEST,
                        color
                );
            }

            // EAST
            if (isBorder(
                    territoryManager,
                    claim,
                    x + 1,
                    y
            )) {

                addBorderEdge(
                        x, y,
                        Edge.EAST,
                        color
                );
            }
        }

        System.out.println(
                "Territory border segments: "
                        + instances.size
        );
    }

    private enum Edge {
        NORTH,
        SOUTH,
        WEST,
        EAST
    }

    private boolean isBorder(
            TerritoryManager territoryManager,
            TerritoryClaim current,
            int neighborX,
            int neighborY
    ) {

        return territoryManager
                .findClaim(
                        neighborX,
                        neighborY
                )
                .map(neighbor ->
                        neighbor.civilizationId()
                                != current.civilizationId()
                )
                .orElse(true);
    }

    private void addBorderEdge(
            int tileX,
            int tileY,
            Edge edge,
            Color color
    ) {

        float centerX =
                tileX - (worldMap.getWidth() - 1) / 2f;

        float centerZ =
                tileY - (worldMap.getHeight() - 1) / 2f;

        float startX;
        float startZ;
        float endX;
        float endZ;

        switch (edge) {

            case NORTH -> {

                startX = centerX - HALF_EDGE;
                endX = centerX + HALF_EDGE;

                startZ = centerZ - 0.5f + BORDER_INSET;
                endZ = startZ;
            }

            case SOUTH -> {

                startX = centerX - HALF_EDGE;
                endX = centerX + HALF_EDGE;

                startZ = centerZ + 0.5f - BORDER_INSET;
                endZ = startZ;
            }

            case WEST -> {

                startZ = centerZ - HALF_EDGE;
                endZ = centerZ + HALF_EDGE;

                startX = centerX - 0.5f + BORDER_INSET;
                endX = startX;
            }

            case EAST -> {

                startZ = centerZ - HALF_EDGE;
                endZ = centerZ + HALF_EDGE;

                startX = centerX + 0.5f - BORDER_INSET;
                endX = startX;
            }

            default -> throw new IllegalStateException(
                    "Unexpected edge: " + edge
            );
        }

        // Split every tile edge into small segments
        // to better follow the terrain surface.
        for (int i = 0;
             i < SEGMENTS_PER_EDGE;
             i++) {

            float t1 =
                    i / (float) SEGMENTS_PER_EDGE;

            float t2 =
                    (i + 1f) / SEGMENTS_PER_EDGE;

            Vector3 start = terrainPoint(
                    interpolate(startX, endX, t1),
                    interpolate(startZ, endZ, t1)
            );

            Vector3 end = terrainPoint(
                    interpolate(startX, endX, t2),
                    interpolate(startZ, endZ, t2)
            );

            addLine(start, end, color);
        }
    }

    private float interpolate(
            float start,
            float end,
            float t
    ) {
        return start + (end - start) * t;
    }

    private Vector3 terrainPoint(
            float x,
            float z
    ) {

        float height =
                TerrainMeshBuilder.surfaceHeightAt(
                        worldMap,
                        x,
                        z
                );

        return new Vector3(
                x,
                height + HEIGHT_OFFSET,
                z
        );
    }

    private void addLine(
            Vector3 start,
            Vector3 end,
            Color color
    ) {

        Vector3 direction =
                new Vector3(end).sub(start);

        float length = direction.len();

        if (length < 0.0001f) {
            return;
        }

        direction.nor();

        Vector3 midpoint =
                new Vector3(start)
                        .add(end)
                        .scl(0.5f);

        Quaternion rotation =
                new Quaternion()
                        .setFromCross(
                                Vector3.Y,
                                direction
                        );

        ModelInstance instance =
                new ModelInstance(lineModel);

        instance.transform.set(
                midpoint,
                rotation,
                new Vector3(
                        LINE_THICKNESS,
                        length,
                        LINE_THICKNESS
                )
        );

        // Material belongs to this instance.
        instance.materials.first().set(
                ColorAttribute.createDiffuse(color)
        );

        instances.add(instance);
    }

    private Color convertRgb(int rgb) {

        float red =
                ((rgb >> 16) & 0xFF) / 255f;

        float green =
                ((rgb >> 8) & 0xFF) / 255f;

        float blue =
                (rgb & 0xFF) / 255f;

        return new Color(
                red,
                green,
                blue,
                1f
        );
    }

    public void render(
            ModelBatch modelBatch,
            Environment environment
    ) {

        if (instances.size == 0) {
            return;
        }

        modelBatch.render(
                instances,
                environment
        );
    }

    @Override
    public void dispose() {

        instances.clear();
        civilizationColors.clear();

        lineModel.dispose();
    }
}
