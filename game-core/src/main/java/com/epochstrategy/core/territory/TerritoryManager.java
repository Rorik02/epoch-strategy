
package com.epochstrategy.core.territory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TerritoryManager {

    private record TilePosition(int x, int y) {
    }

    public record TerritoryClaim(
            int x,
            int y,
            long civilizationId,
            long cityId
    ) {

        public TerritoryClaim {

            if (x < 0 || y < 0) {
                throw new IllegalArgumentException(
                        "Territory coordinates cannot be negative"
                );
            }

            if (civilizationId <= 0 || cityId <= 0) {
                throw new IllegalArgumentException(
                        "Civilization and city IDs must be positive"
                );
            }
        }
    }

    private final Map<TilePosition, TerritoryClaim> claims =
            new LinkedHashMap<>();

    public void claimTile(
            int x,
            int y,
            long civilizationId,
            long cityId
    ) {

        TerritoryClaim claim = new TerritoryClaim(
                x,
                y,
                civilizationId,
                cityId
        );

        TilePosition position = new TilePosition(x, y);

        if (claims.containsKey(position)) {
            throw new IllegalStateException(
                    "Tile already claimed: " + x + ", " + y
            );
        }

        claims.put(position, claim);
    }

    public Optional<TerritoryClaim> findClaim(
            int x,
            int y
    ) {

        return Optional.ofNullable(
                claims.get(new TilePosition(x, y))
        );
    }

    public boolean isControlledBy(
            int x,
            int y,
            long civilizationId
    ) {

        return findClaim(x, y)
                .map(claim ->
                        claim.civilizationId() == civilizationId
                )
                .orElse(false);
    }

    public List<TerritoryClaim> getClaims() {
        return List.copyOf(claims.values());
    }

    public List<TerritoryClaim> getClaimsByCivilization(
            long civilizationId
    ) {

        List<TerritoryClaim> result =
                new ArrayList<>();

        for (TerritoryClaim claim : claims.values()) {

            if (claim.civilizationId() == civilizationId) {
                result.add(claim);
            }
        }

        return List.copyOf(result);
    }

    public int getClaimedTileCount() {
        return claims.size();
    }

    public int getClaimedTileCount(
            long civilizationId
    ) {

        return getClaimsByCivilization(
                civilizationId
        ).size();
    }

    public boolean isEmpty() {
        return claims.isEmpty();
    }

    public void clear() {
        claims.clear();
    }
}
