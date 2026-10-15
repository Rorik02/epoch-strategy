
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
import com.epochstrategy.core.world.*;

import java.util.List;
import java.util.Objects;

public final class RiverRouteRenderer implements Disposable {

    private static final int SAMPLES_PER_TILE = 6;
    private static final float HEIGHT_OFFSET = 0.14f;
    private static final float LINE_THICKNESS = 0.065f;

    private final WorldMap worldMap;

    private final Model lineModel;
    private final Model startMarkerModel;
    private final Model endMarkerModel;

    private final Array<ModelInstance> instances =
            new Array<>();

    public RiverRouteRenderer(WorldMap worldMap) {

        this.worldMap = Objects.requireNonNull(worldMap);

        ModelBuilder builder = new ModelBuilder();

        long attributes =
                VertexAttributes.Usage.Position |
                        VertexAttributes.Usage.Normal;

        lineModel = builder.createCylinder(
                1f, 1f, 1f, 8,
                new Material(
                        ColorAttribute.createDiffuse(
                                new Color(1f, 0.75f, 0.12f, 1f)
                        )
                ),
                attributes
        );

        startMarkerModel = builder.createSphere(
                0.30f, 0.30f, 0.30f,
                12, 8,
                new Material(
                        ColorAttribute.createDiffuse(
                                new Color(0.10f, 0.85f, 0.25f, 1f)
                        )
                ),
                attributes
        );

        endMarkerModel = builder.createSphere(
                0.30f, 0.30f, 0.30f,
                12, 8,
                new Material(
                        ColorAttribute.createDiffuse(
                                new Color(1f, 0.36f, 0.10f, 1f)
                        )
                ),
                attributes
        );
    }

    public void clear() {
        instances.clear();
    }

    public void showStart(int x, int y) {

        clear();

        addMarker(startMarkerModel, x, y);
    }

    public void showEndpoints(
            int startX,
            int startY,
            int endX,
            int endY
    ) {

        clear();

        addMarker(startMarkerModel, startX, startY);
        addMarker(endMarkerModel, endX, endY);
    }

    public void showRoute(
            RiverRouteFinder.RiverRoute route
    ) {
        Objects.requireNonNull(route, "route");
        showRoute(route.points());
    }

    public void showRoute(
            WaterwayFastestRouteFinder.WaterwayRoute route
    ) {
        Objects.requireNonNull(route, "route");
        showRoute(route.points());
    }

    public void showRoute(
            List<RiverRouteFinder.Point> points
    ) {
        Objects.requireNonNull(points, "points");

        clear();

        if (points.isEmpty()) {
            return;
        }

        for (int i = 0; i < points.size() - 1; i++) {

            RiverRouteFinder.Point from = points.get(i);
            RiverRouteFinder.Point to = points.get(i + 1);

            for (int step = 0;
                 step < SAMPLES_PER_TILE;
                 step++) {

                float t1 =
                        step / (float) SAMPLES_PER_TILE;

                float t2 =
                        (step + 1f) / SAMPLES_PER_TILE;

                Vector3 start = interpolate(
                        from, to, t1
                );

                Vector3 end = interpolate(
                        from, to, t2
                );

                addLine(start, end);
            }
        }

        RiverRouteFinder.Point start = points.getFirst();
        RiverRouteFinder.Point end = points.getLast();

        addMarker(
                startMarkerModel,
                start.x(),
                start.y()
        );

        addMarker(
                endMarkerModel,
                end.x(),
                end.y()
        );
    }

    private Vector3 interpolate(
            RiverRouteFinder.Point from,
            RiverRouteFinder.Point to,
            float t
    ) {

        float x = from.x()
                + (to.x() - from.x()) * t;

        float y = from.y()
                + (to.y() - from.y()) * t;

        return terrainPosition(x, y);
    }

    private Vector3 terrainPosition(
            float tileX,
            float tileY
    ) {

        float worldX =
                tileX - (worldMap.getWidth() - 1) / 2f;

        float worldZ =
                tileY - (worldMap.getHeight() - 1) / 2f;

        float height =
                TerrainMeshBuilder.surfaceHeightAt(
                        worldMap,
                        worldX,
                        worldZ
                );

        return new Vector3(
                worldX,
                height + HEIGHT_OFFSET,
                worldZ
        );
    }

    private void addLine(
            Vector3 start,
            Vector3 end
    ) {

        Vector3 direction =
                new Vector3(end).sub(start);

        float length = direction.len();

        if (length < 0.0001f) {
            return;
        }

        direction.nor();

        Vector3 midpoint = new Vector3(start)
                .add(end)
                .scl(0.5f);

        Quaternion rotation = new Quaternion()
                .setFromCross(Vector3.Y, direction);

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

        instances.add(instance);
    }

    private void addMarker(
            Model markerModel,
            int x,
            int y
    ) {

        Vector3 position = terrainPosition(x, y);

        position.y += 0.10f;

        ModelInstance marker =
                new ModelInstance(markerModel);

        marker.transform.setToTranslation(position);

        instances.add(marker);
    }

    public void render(
            ModelBatch modelBatch,
            Environment environment
    ) {

        if (instances.size > 0) {
            modelBatch.render(
                    instances,
                    environment
            );
        }
    }

    @Override
    public void dispose() {

        instances.clear();

        lineModel.dispose();
        startMarkerModel.dispose();
        endMarkerModel.dispose();
    }
}
