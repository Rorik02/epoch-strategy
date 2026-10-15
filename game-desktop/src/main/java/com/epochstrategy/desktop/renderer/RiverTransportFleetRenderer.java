
package com.epochstrategy.desktop.renderer;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.utils.Disposable;

import com.epochstrategy.core.world.RiverRouteFinder;
import com.epochstrategy.core.world.RiverTransportManager;
import com.epochstrategy.core.world.RiverTransportUnit;
import com.epochstrategy.core.world.WorldMap;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.epochstrategy.core.world.WaterwayFastestRouteFinder;
import java.util.List;

public final class RiverTransportFleetRenderer
        implements Disposable {

    private final WorldMap worldMap;

    private final Map<Long, RiverTransportRenderer> renderers =
            new LinkedHashMap<>();

    public RiverTransportFleetRenderer(WorldMap worldMap) {
        this.worldMap = Objects.requireNonNull(worldMap);
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

        RiverTransportRenderer renderer =
                renderers.get(unit.getId());

        if (renderer == null) {

            renderer =
                    new RiverTransportRenderer(worldMap);

            renderers.put(
                    unit.getId(),
                    renderer
            );
        }

        renderer.showUnit(unit, points);
    }


    public void updateUnits(
            RiverTransportManager manager
    ) {

        Objects.requireNonNull(manager);

        for (RiverTransportUnit unit : manager.getUnits()) {

            RiverTransportRenderer renderer =
                    renderers.get(unit.getId());

            if (renderer != null) {
                renderer.updateUnit(unit);
            }
        }
    }

    public void removeUnit(long unitId) {

        RiverTransportRenderer renderer =
                renderers.remove(unitId);

        if (renderer != null) {
            renderer.dispose();
        }
    }

    public int getVisibleUnitCount() {
        return renderers.size();
    }

    public void render(
            ModelBatch modelBatch,
            Environment environment
    ) {

        for (RiverTransportRenderer renderer :
                renderers.values()) {

            renderer.render(
                    modelBatch,
                    environment
            );
        }
    }

    public void clear() {

        for (RiverTransportRenderer renderer :
                renderers.values()) {

            renderer.dispose();
        }

        renderers.clear();
    }

    @Override
    public void dispose() {
        clear();
    }
}
