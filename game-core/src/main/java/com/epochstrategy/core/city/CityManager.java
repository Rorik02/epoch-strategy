
package com.epochstrategy.core.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CityManager {

    private final Map<Long, City> cities =
            new LinkedHashMap<>();

    private final Map<Position, Long> occupiedTiles =
            new HashMap<>();

    private record Position(int x, int y) {
    }

    public void registerCity(City city) {

        Objects.requireNonNull(city, "city");

        if (cities.containsKey(city.getId())) {
            throw new IllegalArgumentException(
                    "City ID already exists: " + city.getId()
            );
        }

        Position position = new Position(
                city.getX(),
                city.getY()
        );

        if (occupiedTiles.containsKey(position)) {
            throw new IllegalArgumentException(
                    "Tile is already occupied by a city: "
                            + city.getX() + ", " + city.getY()
            );
        }

        cities.put(city.getId(), city);
        occupiedTiles.put(position, city.getId());
    }

    public Optional<City> findCity(long cityId) {
        return Optional.ofNullable(cities.get(cityId));
    }

    public Optional<City> findCityAt(int x, int y) {

        Long cityId = occupiedTiles.get(
                new Position(x, y)
        );

        if (cityId == null) {
            return Optional.empty();
        }

        return findCity(cityId);
    }

    public boolean isTileOccupied(int x, int y) {

        return occupiedTiles.containsKey(
                new Position(x, y)
        );
    }

    public List<City> getCities() {

        return List.copyOf(cities.values());
    }

    public List<City> getCitiesByCivilization(
            Long civilizationId
    ) {

        List<City> result = new ArrayList<>();

        for (City city : cities.values()) {

            if (Objects.equals(
                    city.getCivilizationId(),
                    civilizationId
            )) {

                result.add(city);
            }
        }

        return List.copyOf(result);
    }

    public boolean removeCity(long cityId) {

        City removed = cities.remove(cityId);

        if (removed == null) {
            return false;
        }

        Position position = new Position(
                removed.getX(),
                removed.getY()
        );

        occupiedTiles.remove(position);

        return true;
    }

    public int getCityCount() {
        return cities.size();
    }
}
