
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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.epochstrategy.core.world.*;

import java.util.List;
import java.util.Objects;

public final class RiverTransportRenderer implements Disposable {

    private final WorldMap worldMap;

    private final Model hullModel;
    private final Model deckModel;
    private final Model bowModel;

    private final ModelInstance hull;
    private final ModelInstance deck;
    private final ModelInstance bow;

    private List<RiverRouteFinder.Point> route = List.of();

    private boolean visible = false;

    public RiverTransportRenderer(WorldMap worldMap) {

        this.worldMap = Objects.requireNonNull(worldMap);

        ModelBuilder builder = new ModelBuilder();

        long attributes =
                VertexAttributes.Usage.Position |
                        VertexAttributes.Usage.Normal;

        // Main boat hull
        hullModel = builder.createBox(
                0.26f, 0.12f, 0.52f,
                new Material(
                        ColorAttribute.createDiffuse(
                                new Color(0.38f, 0.20f, 0.09f, 1f)
                        )
                ),
                attributes
        );

        // Upper deck
        deckModel = builder.createBox(
                0.20f, 0.06f, 0.28f,
                new Material(
                        ColorAttribute.createDiffuse(
                                new Color(0.78f, 0.60f, 0.35f, 1f)
                        )
                ),
                attributes
        );

        // Marker indicating the bow
        bowModel = builder.createBox(
                0.08f, 0.06f, 0.07f,
                new Material(
                        ColorAttribute.createDiffuse(
                                new Color(1f, 0.80f, 0.12f, 1f)
                        )
                ),
                attributes
        );

        hull = new ModelInstance(hullModel);
        deck = new ModelInstance(deckModel);
        bow = new ModelInstance(bowModel);
    }

    public void showUnit(
            RiverTransportUnit unit,
            RiverRouteFinder.RiverRoute route
    ) {
        Objects.requireNonNull(route, "route");
        showUnit(unit, route.points());
    }

    public void showUnit(
            RiverTransportUnit unit,
            WaterwayFastestRouteFinder.WaterwayRoute route
    ) {
        Objects.requireNonNull(route, "route");
        showUnit(unit, route.points());
    }

    public void showUnit(
            RiverTransportUnit unit,
            List<RiverRouteFinder.Point> points
    ) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(points, "points");

        if (points.isEmpty()) {
            throw new IllegalArgumentException(
                    "Transport route cannot be empty"
            );
        }

        this.route = List.copyOf(points);
        visible = true;

        updateUnit(unit);
    }

    public void updateUnit(RiverTransportUnit unit) {

        Objects.requireNonNull(unit);

        if (!visible || route.isEmpty()) {
            return;
        }

        List<RiverRouteFinder.Point> points = route;

        RiverRouteFinder.Point current =
                new RiverRouteFinder.Point(
                        unit.getX(),
                        unit.getY()
                );

        int index = points.indexOf(current);

        if (index < 0) {
            throw new IllegalStateException(
                    "Unit position is outside its route"
            );
        }

        // Face the next point, or the last traveled
        // direction when the destination is reached.
        int fromIndex;
        int toIndex;

        if (index < points.size() - 1) {
            fromIndex = index;
            toIndex = index + 1;
        } else {
            fromIndex = Math.max(0, index - 1);
            toIndex = index;
        }

        RiverRouteFinder.Point from =
                points.get(fromIndex);

        RiverRouteFinder.Point to =
                points.get(toIndex);

        float dx = to.x() - from.x();
        float dz = to.y() - from.y();

        float angle = MathUtils.atan2(dx, dz)
                * MathUtils.radiansToDegrees;

        float worldX = unit.getX()
                - (worldMap.getWidth() - 1) / 2f;

        float worldZ = unit.getY()
                - (worldMap.getHeight() - 1) / 2f;

        float terrainHeight =
                TerrainMeshBuilder.surfaceHeightAt(
                        worldMap,
                        worldX,
                        worldZ
                );

        float hullY = terrainHeight + 0.115f;

        hull.transform
                .setToTranslation(
                        worldX,
                        hullY,
                        worldZ
                )
                .rotate(Vector3.Y, angle);

        deck.transform
                .setToTranslation(
                        worldX,
                        hullY + 0.09f,
                        worldZ
                )
                .rotate(Vector3.Y, angle);

        Vector3 bowPosition = new Vector3(
                0f, 0.09f, 0.20f
        );

        bowPosition.rotate(Vector3.Y, angle);
        bowPosition.add(worldX, hullY, worldZ);

        bow.transform
                .setToTranslation(bowPosition)
                .rotate(Vector3.Y, angle);
    }

    public void clear() {
        visible = false;
        route = List.of();
    }

    public void render(
            ModelBatch modelBatch,
            Environment environment
    ) {

        if (!visible) {
            return;
        }

        modelBatch.render(hull, environment);
        modelBatch.render(deck, environment);
        modelBatch.render(bow, environment);
    }

    @Override
    public void dispose() {

        hullModel.dispose();
        deckModel.dispose();
        bowModel.dispose();
    }
}
