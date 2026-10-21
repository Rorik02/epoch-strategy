
package com.epochstrategy.core.city;

import com.epochstrategy.core.world.RiverNetwork;
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
}
