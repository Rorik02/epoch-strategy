
package com.epochstrategy.core.building;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;
import com.epochstrategy.core.economy.ResourceType;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CityBuildingManagerTest {

    private City createCity(long id) {

        return new City(
                id,
                "Test City " + id,
                10,
                (int) (id * 5),
                400,
                null
        );
    }

    @Test
    void constructsBuildingAndConsumesResources() {

        CityManager cityManager = new CityManager();
        CityBuildingManager buildings =
                new CityBuildingManager();

        City city = createCity(1L);

        cityManager.registerCity(city);

        city.getInventory().add(
                ResourceType.WOOD, 25
        );

        city.getInventory().add(
                ResourceType.STONE, 15
        );

        assertTrue(
                buildings.canConstruct(
                        1L,
                        BuildingType.SAWMILL,
                        cityManager
                )
        );

        buildings.construct(
                1L,
                BuildingType.SAWMILL,
                cityManager
        );

        assertEquals(
                15,
                city.getInventory().getAmount(
                        ResourceType.WOOD
                )
        );

        assertEquals(
                10,
                city.getInventory().getAmount(
                        ResourceType.STONE
                )
        );

        assertTrue(
                buildings.hasBuilding(
                        1L,
                        BuildingType.SAWMILL
                )
        );

        assertEquals(
                1,
                buildings.getBuildingCount(1L)
        );
    }

    @Test
    void rejectsConstructionWithoutResources() {

        CityManager cityManager = new CityManager();
        CityBuildingManager buildings =
                new CityBuildingManager();

        City city = createCity(1L);
        cityManager.registerCity(city);

        city.getInventory().add(
                ResourceType.WOOD, 25
        );

        city.getInventory().add(
                ResourceType.STONE, 2
        );

        assertFalse(
                buildings.canConstruct(
                        1L,
                        BuildingType.SAWMILL,
                        cityManager
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> buildings.construct(
                        1L,
                        BuildingType.SAWMILL,
                        cityManager
                )
        );

        // No resources were deducted.
        assertEquals(
                25,
                city.getInventory().getAmount(
                        ResourceType.WOOD
                )
        );

        assertEquals(
                2,
                city.getInventory().getAmount(
                        ResourceType.STONE
                )
        );

        assertEquals(
                0,
                buildings.getBuildingCount(1L)
        );
    }

    @Test
    void preventsDuplicateBuildings() {

        CityManager cityManager = new CityManager();
        CityBuildingManager buildings =
                new CityBuildingManager();

        City city = createCity(1L);
        cityManager.registerCity(city);

        city.getInventory().add(
                ResourceType.WOOD, 50
        );

        city.getInventory().add(
                ResourceType.STONE, 50
        );

        buildings.construct(
                1L,
                BuildingType.FARM,
                cityManager
        );

        assertFalse(
                buildings.canConstruct(
                        1L,
                        BuildingType.FARM,
                        cityManager
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> buildings.construct(
                        1L,
                        BuildingType.FARM,
                        cityManager
                )
        );

        assertEquals(
                42,
                city.getInventory().getAmount(
                        ResourceType.WOOD
                )
        );

        assertEquals(
                46,
                city.getInventory().getAmount(
                        ResourceType.STONE
                )
        );
    }

    @Test
    void storesBuildingsSeparatelyForEachCity() {

        CityManager cityManager = new CityManager();
        CityBuildingManager buildings =
                new CityBuildingManager();

        City first = createCity(1L);
        City second = createCity(2L);

        cityManager.registerCity(first);
        cityManager.registerCity(second);

        first.getInventory().add(
                ResourceType.WOOD, 50
        );

        first.getInventory().add(
                ResourceType.STONE, 50
        );

        second.getInventory().add(
                ResourceType.WOOD, 50
        );

        second.getInventory().add(
                ResourceType.STONE, 50
        );

        buildings.construct(
                1L,
                BuildingType.FARM,
                cityManager
        );

        buildings.construct(
                2L,
                BuildingType.QUARRY,
                cityManager
        );

        assertTrue(
                buildings.hasBuilding(
                        1L,
                        BuildingType.FARM
                )
        );

        assertFalse(
                buildings.hasBuilding(
                        2L,
                        BuildingType.FARM
                )
        );

        assertTrue(
                buildings.hasBuilding(
                        2L,
                        BuildingType.QUARRY
                )
        );

        assertEquals(
                1,
                buildings.getBuildingCount(1L)
        );

        assertEquals(
                1,
                buildings.getBuildingCount(2L)
        );
    }

    @Test
    void rejectsUnknownCity() {

        CityManager cityManager = new CityManager();
        CityBuildingManager buildings =
                new CityBuildingManager();

        assertThrows(
                IllegalArgumentException.class,
                () -> buildings.construct(
                        999L,
                        BuildingType.FARM,
                        cityManager
                )
        );
    }

    @Test
    void returnsImmutableBuildingList() {

        CityManager cityManager = new CityManager();
        CityBuildingManager buildings =
                new CityBuildingManager();

        City city = createCity(1L);
        cityManager.registerCity(city);

        city.getInventory().add(
                ResourceType.WOOD, 50
        );

        city.getInventory().add(
                ResourceType.STONE, 50
        );

        buildings.construct(
                1L,
                BuildingType.FARM,
                cityManager
        );

        Set<BuildingType> snapshot =
                buildings.getBuildings(1L);

        assertEquals(
                Set.of(BuildingType.FARM),
                snapshot
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.add(
                        BuildingType.SAWMILL
                )
        );

        buildings.construct(
                1L,
                BuildingType.SAWMILL,
                cityManager
        );

        // Existing snapshot stays unchanged.
        assertEquals(
                Set.of(BuildingType.FARM),
                snapshot
        );

        assertEquals(
                2,
                buildings.getBuildingCount(1L)
        );
    }

    @Test
    void removesBuildingRecordsForCity() {

        CityManager cityManager = new CityManager();
        CityBuildingManager buildings =
                new CityBuildingManager();

        City city = createCity(1L);
        cityManager.registerCity(city);

        city.getInventory().add(
                ResourceType.WOOD, 20
        );

        city.getInventory().add(
                ResourceType.STONE, 10
        );

        buildings.construct(
                1L,
                BuildingType.FARM,
                cityManager
        );

        buildings.removeCityBuildings(1L);

        assertEquals(
                0,
                buildings.getBuildingCount(1L)
        );

        assertTrue(
                buildings.getBuildings(1L).isEmpty()
        );
    }

    @Test
    void buildingTypesHaveProductionBonuses() {

        assertEquals(
                ResourceType.FOOD,
                BuildingType.FARM.getProducedResource()
        );

        assertEquals(
                4,
                BuildingType.FARM.getProductionBonus()
        );

        assertEquals(
                ResourceType.WOOD,
                BuildingType.SAWMILL.getProducedResource()
        );

        assertEquals(
                3,
                BuildingType.SAWMILL.getProductionBonus()
        );

        assertEquals(
                ResourceType.IRON,
                BuildingType.IRON_MINE.getProducedResource()
        );
    }
}
