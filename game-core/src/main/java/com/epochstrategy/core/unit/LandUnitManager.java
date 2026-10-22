
package com.epochstrategy.core.unit;

import com.epochstrategy.core.civilization.CivilizationManager;
import com.epochstrategy.core.world.WorldMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LandUnitManager {

    private final Map<Long, LandUnit> units =
            new LinkedHashMap<>();

    private long nextUnitId = 1L;

    public LandUnit createSettler(
            long civilizationId,
            int x,
            int y,
            WorldMap worldMap,
            CivilizationManager civilizationManager
    ) {

        Objects.requireNonNull(worldMap, "worldMap");
        Objects.requireNonNull(
                civilizationManager,
                "civilizationManager"
        );

        if (civilizationId <= 0
                || civilizationManager
                .findCivilization(civilizationId)
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown civilization: " + civilizationId
            );
        }

        if (!worldMap.isInsideMap(x, y)) {
            throw new IllegalArgumentException(
                    "Unit position outside map"
            );
        }

        if (!worldMap.getTile(x, y)
                .isPassableForLandUnits()) {

            throw new IllegalArgumentException(
                    "Terrain is not passable for land units"
            );
        }

        if (findUnitAt(x, y).isPresent()) {
            throw new IllegalStateException(
                    "Tile already contains a land unit"
            );
        }

        LandUnit unit = new LandUnit(
                nextUnitId,
                civilizationId,
                LandUnitType.SETTLER,
                x,
                y
        );

        units.put(unit.getId(), unit);

        nextUnitId++;

        return unit;
    }

    public Optional<LandUnit> findUnit(long unitId) {
        return Optional.ofNullable(units.get(unitId));
    }

    public Optional<LandUnit> findUnitAt(
            int x,
            int y
    ) {

        return units.values()
                .stream()
                .filter(unit ->
                        unit.getX() == x
                                && unit.getY() == y
                )
                .findFirst();
    }

    public List<LandUnit> getUnits() {
        return List.copyOf(units.values());
    }

    public List<LandUnit> getUnitsByCivilization(
            long civilizationId
    ) {

        List<LandUnit> result = new ArrayList<>();

        for (LandUnit unit : units.values()) {

            if (unit.getCivilizationId() == civilizationId) {
                result.add(unit);
            }
        }

        return List.copyOf(result);
    }

    public int getUnitCount() {
        return units.size();
    }

    public void restoreAllMovementPoints() {

        for (LandUnit unit : units.values()) {
            unit.restoreMovementPoints();
        }
    }

    public boolean removeUnit(long unitId) {
        return units.remove(unitId) != null;
    }
}
