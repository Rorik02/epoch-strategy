
package com.epochstrategy.core.city;

import com.epochstrategy.core.engine.GameState;
import com.epochstrategy.core.territory.TerritoryExpansionConfig;
import com.epochstrategy.core.unit.LandUnit;
import com.epochstrategy.core.unit.LandUnitType;
import com.epochstrategy.core.world.RiverNetwork;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.WorldMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class CityGenerator {

    private final CityPlacementService placementService;

    private record CityCandidate(
            int x,
            int y,
            int score
    ) {
    }

    public CityGenerator(
            CityPlacementService placementService
    ) {
        this.placementService =
                Objects.requireNonNull(
                        placementService,
                        "placementService"
                );
    }

    public List<City> generate(
            WorldMap map,
            RiverNetwork riverNetwork,
            CityManager cityManager,
            int requestedCount,
            long seed
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(riverNetwork, "riverNetwork");
        Objects.requireNonNull(cityManager, "cityManager");

        if (requestedCount < 0) {
            throw new IllegalArgumentException(
                    "Requested city count cannot be negative"
            );
        }

        if (requestedCount == 0) {
            return List.of();
        }

        Random random = new Random(seed);

        List<CityCandidate> candidates =
                new ArrayList<>();

        // Step 1: Find all valid starting locations.
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {

                var evaluation =
                        placementService.evaluate(
                                map,
                                riverNetwork,
                                cityManager,
                                x,
                                y
                        );

                if (!evaluation.canPlace()) {
                    continue;
                }

                candidates.add(
                        new CityCandidate(
                                x,
                                y,
                                evaluation.locationScore()
                        )
                );
            }
        }

        // Step 2: Randomize equal-score candidates.
        Collections.shuffle(candidates, random);

        // Step 3: Prefer locations with higher scores.
        // Java's stable sort preserves randomized
        // order among candidates with equal scores.
        candidates.sort(
                Comparator.comparingInt(
                        CityCandidate::score
                ).reversed()
        );

        List<City> generatedCities =
                new ArrayList<>();

        long nextId = 0L;

        for (City existingCity : cityManager.getCities()) {
            nextId = Math.max(
                    nextId,
                    existingCity.getId()
            );
        }

        // Step 4: Create cities in the best valid places.
        for (CityCandidate candidate : candidates) {

            if (generatedCities.size() >= requestedCount) {
                break;
            }

            // Validate again because previously generated
            // cities may now occupy nearby locations.
            var evaluation =
                    placementService.evaluate(
                            map,
                            riverNetwork,
                            cityManager,
                            candidate.x(),
                            candidate.y()
                    );

            if (!evaluation.canPlace()) {
                continue;
            }

            if (nextId == Long.MAX_VALUE) {
                throw new IllegalStateException(
                        "No available city IDs"
                );
            }

            nextId++;

            int initialPopulation =
                    200 + random.nextInt(301);

            City city = new City(
                    nextId,
                    "Settlement " + nextId,
                    candidate.x(),
                    candidate.y(),
                    initialPopulation,
                    null
            );

            placementService.placeCity(
                    map,
                    riverNetwork,
                    cityManager,
                    city
            );

            generatedCities.add(city);
        }

        return List.copyOf(generatedCities);
    }

    public static final class CityFoundingService {

        private static final int STARTING_POPULATION = 200;

        public record FoundingCheck(
                boolean allowed,
                String reason
        ) {}

        public FoundingCheck checkFounding(
                LandUnit settler,
                GameState gameState,
                WorldMap worldMap
        ) {

            Objects.requireNonNull(settler, "settler");
            Objects.requireNonNull(gameState, "gameState");
            Objects.requireNonNull(worldMap, "worldMap");

            if (settler.getType() != LandUnitType.SETTLER) {
                return blocked("Unit is not a settler");
            }

            if (gameState.getLandUnitManager()
                    .findUnit(settler.getId())
                    .filter(unit -> unit == settler)
                    .isEmpty()) {

                return blocked("Settler is not registered");
            }

            if (gameState.getCivilizationManager()
                    .findCivilization(settler.getCivilizationId())
                    .isEmpty()) {

                return blocked("Unknown civilization");
            }

            int x = settler.getX();
            int y = settler.getY();

            if (!worldMap.isInsideMap(x, y)) {
                return blocked("Outside map");
            }

            if (!worldMap.getTile(x, y)
                    .isPassableForLandUnits()) {

                return blocked("Invalid terrain");
            }

            if (gameState.getCityManager()
                    .findCityAt(x, y)
                    .isPresent()) {

                return blocked("City already exists here");
            }

            if (gameState.getTerritoryManager()
                    .findClaim(x, y)
                    .isPresent()) {

                return blocked("Tile is already controlled");
            }

            CityFoundingSettings settings =
                    gameState.getCityFoundingSettings();

            for (City existingCity :
                    gameState.getCityManager().getCities()) {

                if (!settings.isDistanceAllowed(
                        x,
                        y,
                        existingCity.getX(),
                        existingCity.getY()
                )) {

                    return blocked(
                            "Too close to " + existingCity.getName()
                                    + " (min. "
                                    + settings.getMinimumCityDistance()
                                    + " tiles)"
                    );
                }
            }

            return new FoundingCheck(
                    true,
                    "Available"
            );
        }

        public City foundCity(
                long settlerId,
                GameState gameState,
                WorldMap worldMap
        ) {

            Objects.requireNonNull(gameState, "gameState");
            Objects.requireNonNull(worldMap, "worldMap");

            LandUnit settler =
                    gameState.getLandUnitManager()
                            .findUnit(settlerId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Unknown settler: " + settlerId
                                    )
                            );

            FoundingCheck check =
                    checkFounding(
                            settler,
                            gameState,
                            worldMap
                    );

            if (!check.allowed()) {
                throw new IllegalStateException(
                        check.reason()
                );
            }

            long maxCityId =
                    gameState.getCityManager()
                            .getCities()
                            .stream()
                            .mapToLong(City::getId)
                            .max()
                            .orElse(0L);

            long newCityId = Math.addExact(maxCityId, 1L);

            int x = settler.getX();
            int y = settler.getY();

            City newCity = new City(
                    newCityId,
                    "Settlement " + newCityId,
                    x,
                    y,
                    STARTING_POPULATION,
                    settler.getCivilizationId()
            );

            gameState.getCityManager()
                    .registerCity(newCity);

            // Initial city territory: up to 3x3.
            // Only free, non-ocean tiles can be claimed.

            int radius =
                    TerritoryExpansionConfig.INITIAL_RADIUS;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {

                    int tileX = x + dx;
                    int tileY = y + dy;

                    if (!worldMap.isInsideMap(
                            tileX, tileY
                    )) {
                        continue;
                    }

                    if (worldMap.getTile(tileX, tileY)
                            .getTerrain() == TerrainType.OCEAN) {
                        continue;
                    }

                    if (gameState.getTerritoryManager()
                            .findClaim(tileX, tileY)
                            .isPresent()) {
                        continue;
                    }

                    gameState.getTerritoryManager()
                            .claimTile(
                                    tileX,
                                    tileY,
                                    newCity.getCivilizationId(),
                                    newCity.getId()
                            );
                }
            }

            // Settler is consumed when the city is founded.
            gameState.getLandUnitManager()
                    .removeUnit(settlerId);

            return newCity;
        }

        private FoundingCheck blocked(String reason) {

            return new FoundingCheck(
                    false,
                    reason
            );
        }
    }
}
