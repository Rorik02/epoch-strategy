
package com.epochstrategy.core.city;

import com.epochstrategy.core.world.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CityGeneratorTest {

    private final CityGenerator generator =
            new CityGenerator(
                    new CityPlacementService(5)
            );

    @Test
    void generatesRequestedNumberOfCities() {

        WorldMap map = createMap(30, 30);
        RiverNetwork rivers = new RiverNetwork(30, 30);
        CityManager manager = new CityManager();

        List<City> generated = generator.generate(
                map,
                rivers,
                manager,
                10,
                12345L
        );

        assertEquals(10, generated.size());
        assertEquals(10, manager.getCityCount());

        for (City city : generated) {
            assertTrue(
                    manager.findCity(city.getId()).isPresent()
            );

            assertTrue(city.isNeutral());
            assertTrue(city.getPopulation() >= 200);
            assertTrue(city.getPopulation() <= 500);
        }
    }

    @Test
    void respectsMinimumDistanceBetweenCities() {

        WorldMap map = createMap(30, 30);
        RiverNetwork rivers = new RiverNetwork(30, 30);
        CityManager manager = new CityManager();

        List<City> cities = generator.generate(
                map,
                rivers,
                manager,
                15,
                9876L
        );

        assertEquals(15, cities.size());

        for (int i = 0; i < cities.size(); i++) {
            for (int j = i + 1; j < cities.size(); j++) {

                City a = cities.get(i);
                City b = cities.get(j);

                int dx = a.getX() - b.getX();
                int dy = a.getY() - b.getY();

                int distanceSquared =
                        dx * dx + dy * dy;

                assertTrue(
                        distanceSquared >= 25,
                        "Cities are too close: "
                                + a.getName()
                                + " and "
                                + b.getName()
                );
            }
        }
    }

    @Test
    void generatesSameCitiesWithSameSeed() {

        WorldMap mapA = createMap(20, 20);
        WorldMap mapB = createMap(20, 20);

        RiverNetwork riversA =
                new RiverNetwork(20, 20);

        RiverNetwork riversB =
                new RiverNetwork(20, 20);

        List<City> first = generator.generate(
                mapA,
                riversA,
                new CityManager(),
                8,
                555L
        );

        List<City> second = generator.generate(
                mapB,
                riversB,
                new CityManager(),
                8,
                555L
        );

        assertEquals(
                describeCities(first),
                describeCities(second)
        );
    }

    @Test
    void prioritizesFreshwaterLocations() {

        WorldMap map = createMap(7, 7);
        RiverNetwork rivers = new RiverNetwork(7, 7);
        CityManager manager = new CityManager();

        map.setTile(
                new Tile(
                        3, 3,
                        TerrainType.LAKE,
                        1
                )
        );

        List<City> cities = generator.generate(
                map,
                rivers,
                manager,
                1,
                100L
        );

        assertEquals(1, cities.size());

        City city = cities.getFirst();

        var evaluation =
                new CityPlacementService(5).evaluate(
                        map,
                        rivers,
                        new CityManager(),
                        city.getX(),
                        city.getY()
                );

        assertEquals(
                CityPlacementService.WaterAccess.FRESHWATER,
                evaluation.waterAccess()
        );

        assertEquals(40, evaluation.locationScore());
    }

    @Test
    void preservesExistingCitiesAndIds() {

        WorldMap map = createMap(20, 20);
        RiverNetwork rivers = new RiverNetwork(20, 20);
        CityManager manager = new CityManager();

        City existing = new City(
                42L,
                "Existing City",
                0,
                0,
                1000,
                1L
        );

        manager.registerCity(existing);

        List<City> generated = generator.generate(
                map,
                rivers,
                manager,
                3,
                777L
        );

        assertEquals(3, generated.size());
        assertEquals(4, manager.getCityCount());

        assertSame(
                existing,
                manager.findCity(42L).orElseThrow()
        );

        assertEquals(43L, generated.get(0).getId());
        assertEquals(44L, generated.get(1).getId());
        assertEquals(45L, generated.get(2).getId());
    }

    @Test
    void stopsWhenMapHasNoMoreValidLocations() {

        WorldMap map = createMap(3, 3);
        RiverNetwork rivers = new RiverNetwork(3, 3);
        CityManager manager = new CityManager();

        List<City> generated = generator.generate(
                map,
                rivers,
                manager,
                10,
                123L
        );

        // With minimum distance 5, only one city
        // fits on this 3x3 map.
        assertEquals(1, generated.size());
        assertEquals(1, manager.getCityCount());
    }

    private static List<String> describeCities(
            List<City> cities
    ) {
        return cities.stream()
                .map(city ->
                        city.getId()
                                + ":"
                                + city.getX()
                                + ","
                                + city.getY()
                                + ":"
                                + city.getPopulation()
                )
                .toList();
    }

    private static WorldMap createMap(
            int width,
            int height
    ) {

        WorldMap map = new WorldMap(width, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                map.setTile(
                        new Tile(
                                x,
                                y,
                                TerrainType.PLAINS,
                                1
                        )
                );
            }
        }

        return map;
    }
}
