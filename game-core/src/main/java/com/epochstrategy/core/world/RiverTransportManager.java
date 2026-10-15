
package com.epochstrategy.core.world;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.HashMap;

public final class RiverTransportManager {

    private final Map<Long, RiverTransportUnit> units =
            new LinkedHashMap<>();

    public void registerUnit(RiverTransportUnit unit) {

        Objects.requireNonNull(unit, "unit");

        if (units.containsKey(unit.getId())) {
            throw new IllegalArgumentException(
                    "Transport unit ID already exists: "
                            + unit.getId()
            );
        }

        units.put(unit.getId(), unit);
    }

    public Optional<RiverTransportUnit> findUnit(long id) {
        return Optional.ofNullable(units.get(id));
    }

    public List<RiverTransportUnit> getUnits() {
        return List.copyOf(units.values());
    }

    public boolean removeUnit(long id) {
        return units.remove(id) != null;
    }

    public int getUnitCount() {
        return units.size();
    }


    public Map<Long, RiverTransportService.MovementResult>
    advanceTurn(
            WorldMap map,
            RiverNetwork network
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(network, "network");

        Map<Long, RiverTransportService.MovementResult>
                results = new LinkedHashMap<>();

        // Cache navigation graphs for this turn.
        Map<RiverVesselType, WaterwayGraph> graphs =
                new HashMap<>();

        for (RiverTransportUnit unit : getUnits()) {

            // Idle units do not need a navigation graph.
            if (!unit.hasActiveRoute()) {

                results.put(
                        unit.getId(),
                        RiverTransportService.advanceTurn(
                                map,
                                network,
                                unit
                        )
                );

                continue;
            }

            // Build only one graph per vessel type.
            WaterwayGraph graph =
                    graphs.computeIfAbsent(
                            unit.getVesselType(),
                            vesselType -> WaterwayGraph.build(
                                    map,
                                    network,
                                    vesselType
                            )
                    );

            RiverTransportService.MovementResult result =
                    RiverTransportService.advanceTurn(
                            unit,
                            graph
                    );

            results.put(unit.getId(), result);
        }

        return Collections.unmodifiableMap(results);
    }

}
