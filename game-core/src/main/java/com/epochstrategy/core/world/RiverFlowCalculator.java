
package com.epochstrategy.core.world;

import java.util.Objects;

public final class RiverFlowCalculator {

    private RiverFlowCalculator() {
    }

    public static void calculate(
            WorldMap map,
            RiverNetwork network,
            DrainageMap drainageMap
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(network, "network");
        Objects.requireNonNull(drainageMap, "drainageMap");

        for (RiverNetwork.RiverSegment segment
                : network.getSegments()) {

            int fromX = segment.fromX();
            int fromY = segment.fromY();

            int toX = segment.toX();
            int toY = segment.toY();

            // Catchment area determines water flow
            int flow = Math.max(
                    1,
                    drainageMap.getCatchmentArea(fromX, fromY)
            );

            RiverType type = classifyRiver(flow);

            float width = Math.min(
                    0.36f,
                    0.07f
                            + (float) Math.sqrt(flow) * 0.045f
            );

            float depth = Math.min(
                    2.5f,
                    0.20f + flow * 0.12f
            );

            int fromElevation =
                    map.getTile(fromX, fromY).getElevation();

            int toElevation =
                    map.getTile(toX, toY).getElevation();

            int elevationDrop =
                    fromElevation - toElevation;


            boolean navigable =
                    RiverNavigationService.canNavigate(
                            width,
                            depth,
                            elevationDrop,
                            RiverVesselType.CARGO_BARGE
                    );


            RiverProperties properties =
                    new RiverProperties(
                            flow,
                            width,
                            depth,
                            type,
                            navigable
                    );

            network.setProperties(
                    segment,
                    properties
            );
        }
    }


    private static RiverType classifyRiver(int flow) {

        if (flow >= 15) {
            return RiverType.MAJOR_RIVER;
        }

        if (flow >= 8) {
            return RiverType.SMALL_RIVER;
        }

        return RiverType.STREAM;
    }

}
