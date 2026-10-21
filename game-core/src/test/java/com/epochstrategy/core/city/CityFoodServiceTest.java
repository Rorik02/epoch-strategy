
package com.epochstrategy.core.city;

import com.epochstrategy.core.economy.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityFoodServiceTest {

    private City createCity(int population) {

        return new City(
                1L,
                "Test City",
                10,
                10,
                population,
                null
        );
    }

    @Test
    void freshwaterCityProducesFoodAndGrows() {

        City city = createCity(400);

        var result = CityFoodService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.FRESHWATER
        );

        assertEquals(8, result.producedFood());
        assertEquals(4, result.requiredFood());
        assertEquals(4, result.consumedFood());

        assertEquals(4, result.foodInStorage());
        assertEquals(412, city.getPopulation());

        assertEquals(12, result.populationChange());
        assertFalse(result.starving());
    }

    @Test
    void coastalCityProducesFoodAndGrows() {

        City city = createCity(400);

        var result = CityFoodService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.COASTAL
        );

        assertEquals(6, result.producedFood());
        assertEquals(4, result.consumedFood());

        assertEquals(2, result.foodInStorage());
        assertEquals(408, city.getPopulation());

        assertFalse(result.starving());
    }

    @Test
    void foodShortageReducesPopulation() {

        City city = createCity(400);

        var result = CityFoodService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.NONE
        );

        assertEquals(3, result.producedFood());
        assertEquals(4, result.requiredFood());
        assertEquals(3, result.consumedFood());
        assertEquals(1, result.missingFood());

        assertEquals(395, city.getPopulation());
        assertEquals(-5, result.populationChange());

        assertTrue(result.starving());
    }

    @Test
    void storedFoodPreventsStarvation() {

        City city = createCity(400);

        city.getInventory().add(
                ResourceType.FOOD,
                5
        );

        var result = CityFoodService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.NONE
        );

        assertEquals(3, result.producedFood());
        assertEquals(4, result.consumedFood());
        assertEquals(4, result.foodInStorage());

        assertEquals(404, city.getPopulation());

        assertFalse(result.starving());
    }

    @Test
    void fullWarehouseBlocksFoodProduction() {

        City city = createCity(400);

        city.getInventory().add(
                ResourceType.WOOD,
                200
        );

        var result = CityFoodService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.FRESHWATER
        );

        assertEquals(0, result.producedFood());
        assertEquals(0, result.consumedFood());
        assertEquals(4, result.missingFood());

        assertEquals(380, city.getPopulation());
        assertTrue(result.starving());

        // Existing wood must not disappear.
        assertEquals(
                200,
                city.getInventory().getAmount(
                        ResourceType.WOOD
                )
        );
    }

    @Test
    void productionRespectsRemainingStorageCapacity() {

        City city = createCity(400);

        city.getInventory().add(
                ResourceType.FOOD,
                199
        );

        var result = CityFoodService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.FRESHWATER
        );

        assertEquals(1, result.producedFood());
        assertEquals(4, result.consumedFood());

        assertEquals(196, result.foodInStorage());
        assertEquals(412, city.getPopulation());
    }

    @Test
    void eachCityHasIndependentInventory() {

        City first = createCity(400);

        City second = new City(
                2L,
                "Second City",
                20,
                20,
                400,
                null
        );

        assertNotSame(
                first.getInventory(),
                second.getInventory()
        );

        CityFoodService.advanceTurn(
                first,
                CityPlacementService.WaterAccess.FRESHWATER
        );

        assertEquals(
                4,
                first.getInventory().getAmount(
                        ResourceType.FOOD
                )
        );

        assertEquals(
                0,
                second.getInventory().getAmount(
                        ResourceType.FOOD
                )
        );

        assertEquals(400, second.getPopulation());
    }
}
