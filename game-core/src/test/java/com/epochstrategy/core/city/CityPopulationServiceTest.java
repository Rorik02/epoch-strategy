
package com.epochstrategy.core.city;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityPopulationServiceTest {

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
    void freshwaterProvidesHighestGrowth() {

        City city = createCity(400);

        var result = CityPopulationService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.FRESHWATER
        );

        assertEquals(412, city.getPopulation());
        assertEquals(12, result.growth());
        assertEquals(1000, result.capacity());
    }

    @Test
    void coastalCityGrowsSlower() {

        City city = createCity(400);

        var result = CityPopulationService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.COASTAL
        );

        assertEquals(408, city.getPopulation());
        assertEquals(8, result.growth());
    }

    @Test
    void cityWithoutWaterStillGrows() {

        City city = createCity(400);

        var result = CityPopulationService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.NONE
        );

        assertEquals(404, city.getPopulation());
        assertEquals(4, result.growth());
    }

    @Test
    void populationCannotExceedCapacity() {

        City city = createCity(999);

        var result = CityPopulationService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.FRESHWATER
        );

        assertEquals(1000, city.getPopulation());
        assertEquals(1, result.growth());
    }

    @Test
    void populationStopsGrowingAtCapacity() {

        City city = createCity(1000);

        var result = CityPopulationService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.FRESHWATER
        );

        assertEquals(1000, city.getPopulation());
        assertEquals(0, result.growth());
    }

    @Test
    void populationGrowthAccumulatesAcrossTurns() {

        City city = createCity(400);

        CityPopulationService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.FRESHWATER
        );

        CityPopulationService.advanceTurn(
                city,
                CityPlacementService.WaterAccess.FRESHWATER
        );

        assertEquals(424, city.getPopulation());
    }
}
