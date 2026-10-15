package com.epochstrategy.core.world;

public record WaterAccessDetails(
        WaterAccess type,
        int freshwaterValue,
        int coastalValue,
        int totalValue,
        int freshwaterDistance
) {
}
