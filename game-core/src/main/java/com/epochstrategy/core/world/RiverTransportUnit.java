
package com.epochstrategy.core.world;

import com.epochstrategy.core.economy.ResourceInventory;
import com.epochstrategy.core.economy.ResourceType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RiverTransportUnit {

    private final long id;
    private final RiverVesselType vesselType;

    private int x;
    private int y;

    private final ResourceInventory cargo;

    private List<RiverRouteFinder.Point> route = List.of();
    private int nextPointIndex;

    public RiverTransportUnit(
            long id,
            RiverVesselType vesselType,
            int x,
            int y
    ) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Unit ID must be positive"
            );
        }

        this.id = id;

        this.vesselType =
                Objects.requireNonNull(vesselType);

        this.x = x;
        this.y = y;

        this.cargo = new ResourceInventory(
                vesselType.getCargoCapacity()
        );
    }


    public void assignRoute(
            RiverRouteFinder.RiverRoute newRoute
    ) {
        Objects.requireNonNull(newRoute, "newRoute");
        assignRoutePoints(newRoute.points());
    }

    public void assignRoute(
            WaterwayFastestRouteFinder.WaterwayRoute newRoute
    ) {
        Objects.requireNonNull(newRoute, "newRoute");
        assignRoutePoints(newRoute.points());
    }

    private void assignRoutePoints(
            List<RiverRouteFinder.Point> points
    ) {
        Objects.requireNonNull(points, "points");

        if (points.isEmpty()) {
            throw new IllegalArgumentException(
                    "Route cannot be empty"
            );
        }

        RiverRouteFinder.Point start = points.getFirst();

        if (start.x() != x || start.y() != y) {
            throw new IllegalArgumentException(
                    "Route must start at unit position"
            );
        }

        route = List.copyOf(points);
        nextPointIndex = 1;
    }

    List<RiverRouteFinder.Point> getAssignedRoutePoints() {
        return route;
    }

    int getNextPointIndex() {
        return nextPointIndex;
    }

    public boolean hasActiveRoute() {
        return nextPointIndex < route.size();
    }

    RiverRouteFinder.Point nextPoint() {

        if (!hasActiveRoute()) {
            throw new IllegalStateException(
                    "Unit has no active route"
            );
        }

        return route.get(nextPointIndex);
    }

    void moveToNextPoint() {

        RiverRouteFinder.Point next = nextPoint();

        x = next.x();
        y = next.y();

        nextPointIndex++;

        if (!hasActiveRoute()) {
            cancelRoute();
        }
    }

    public void cancelRoute() {

        route = List.of();
        nextPointIndex = 0;
    }

    public int getRemainingSegments() {

        if (!hasActiveRoute()) {
            return 0;
        }

        return route.size() - nextPointIndex;
    }

    // Load resources from a warehouse
    public void loadFrom(
            ResourceInventory source,
            ResourceType type,
            int amount
    ) {

        Objects.requireNonNull(source);

        source.transferTo(
                cargo,
                type,
                amount
        );
    }

    // Unload resources into a warehouse
    public void unloadInto(
            ResourceInventory destination,
            ResourceType type,
            int amount
    ) {

        Objects.requireNonNull(destination);

        cargo.transferTo(
                destination,
                type,
                amount
        );
    }

    public int getCargoAmount(ResourceType type) {
        return cargo.getAmount(type);
    }

    public int getCargoUnits() {
        return cargo.getTotalUnits();
    }

    public int getFreeCargoCapacity() {
        return cargo.getFreeCapacity();
    }

    public Map<ResourceType, Integer> getCargoContents() {
        return cargo.getContents();
    }

    public long getId() {
        return id;
    }

    public RiverVesselType getVesselType() {
        return vesselType;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
