
package com.epochstrategy.core.world;

import java.util.Objects;

public final class RiverNavigationService {

    private RiverNavigationService() {
    }

    public static boolean canNavigate(
            float riverWidth,
            float riverDepth,
            int elevationDrop,
            RiverVesselType vesselType
    ) {

        Objects.requireNonNull(
                vesselType,
                "vesselType"
        );

        // Uphill flow indicates invalid river data
        if (elevationDrop < 0) {
            return false;
        }

        boolean enoughWidth =
                riverWidth >= vesselType.getMinWidth();

        boolean enoughDepth =
                riverDepth >= vesselType.getMinDepth();

        boolean acceptableDrop =
                elevationDrop
                        <= vesselType.getMaxElevationDrop();

        return enoughWidth
                && enoughDepth
                && acceptableDrop;
    }

    public static boolean canNavigate(
            WorldMap map,
            RiverNetwork network,
            RiverNetwork.RiverSegment segment,
            RiverVesselType vesselType
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(network, "network");
        Objects.requireNonNull(segment, "segment");

        if (!map.isInsideMap(
                segment.fromX(),
                segment.fromY()
        ) || !map.isInsideMap(
                segment.toX(),
                segment.toY()
        )) {
            return false;
        }

        int distance =
                Math.abs(segment.toX() - segment.fromX())
                        + Math.abs(segment.toY() - segment.fromY());

        if (distance != 1) {
            return false;
        }

        RiverProperties properties =
                network.getProperties(segment);

        int fromElevation = map.getTile(
                segment.fromX(),
                segment.fromY()
        ).getElevation();

        int toElevation = map.getTile(
                segment.toX(),
                segment.toY()
        ).getElevation();

        int elevationDrop =
                fromElevation - toElevation;

        return canNavigate(
                properties.width(),
                properties.depth(),
                elevationDrop,
                vesselType
        );
    }
}
