package com.epochstrategy.core.world;

public record RiverProperties(
        int flow,
        float width,
        float depth,
        RiverType type,
        boolean navigable
) {
}
