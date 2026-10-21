
package com.epochstrategy.core.territory;

import com.epochstrategy.core.city.City;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CityInfluenceManagerTest {

    @Test
    void incomeDependsOnPopulation() {

        assertEquals(
                50,
                TerritoryExpansionConfig.calculateIncome(499)
        );

        assertEquals(
                100,
                TerritoryExpansionConfig.calculateIncome(500)
        );

        assertEquals(
                100,
                TerritoryExpansionConfig.calculateIncome(999)
        );

        assertEquals(
                150,
                TerritoryExpansionConfig.calculateIncome(1000)
        );

        assertEquals(
                200,
                TerritoryExpansionConfig.calculateIncome(1500)
        );

        assertEquals(
                0,
                TerritoryExpansionConfig.calculateIncome(0)
        );
    }

    @Test
    void cityAccumulatesInfluenceOverTurns() {

        City city = new City(
                1L, "Rome",
                3, 3, 1000, 1L
        );

        CityInfluenceManager manager =
                new CityInfluenceManager();

        for (int turn = 0; turn < 10; turn++) {
            manager.advanceCityTurn(city);
        }

        assertEquals(
                1500,
                manager.getBalanceHundredths(1L)
        );

        assertEquals(
                15.0,
                manager.getBalance(1L),
                0.0001
        );
    }

    @Test
    void citiesHaveIndependentInfluence() {

        City rome = new City(
                1L, "Rome",
                3, 3, 500, 1L
        );

        City egypt = new City(
                2L, "Egypt",
                7, 7, 1500, 2L
        );

        CityInfluenceManager manager =
                new CityInfluenceManager();

        manager.advanceTurn(
                List.of(rome, egypt)
        );

        assertEquals(
                100,
                manager.getBalanceHundredths(1L)
        );

        assertEquals(
                200,
                manager.getBalanceHundredths(2L)
        );
    }

    @Test
    void neutralCityDoesNotGenerateInfluence() {

        City city = new City(
                1L, "Neutral",
                3, 3, 500, null
        );

        CityInfluenceManager manager =
                new CityInfluenceManager();

        manager.advanceCityTurn(city);

        assertEquals(
                0,
                manager.getBalanceHundredths(1L)
        );
    }

    @Test
    void expansionCostIncreasesAfterPurchase() {

        City city = new City(
                1L, "Rome",
                3, 3, 500, 1L
        );

        CityInfluenceManager manager =
                new CityInfluenceManager();

        // 25 turns * 1.0 Influence = 25.
        for (int i = 0; i < 25; i++) {
            manager.advanceCityTurn(city);
        }

        assertEquals(
                20.0,
                manager.getNextExpansionCost(1L)
        );

        assertTrue(
                manager.hasEnoughInfluence(1L)
        );

        assertTrue(
                manager.trySpendForExpansion(1L)
        );

        assertEquals(
                5.0,
                manager.getBalance(1L)
        );

        assertEquals(
                1,
                manager.getPurchasedExpansions(1L)
        );

        assertEquals(
                30.0,
                manager.getNextExpansionCost(1L)
        );

        assertFalse(
                manager.hasEnoughInfluence(1L)
        );
    }

    @Test
    void insufficientInfluenceCannotBeSpent() {

        CityInfluenceManager manager =
                new CityInfluenceManager();

        assertFalse(
                manager.trySpendForExpansion(1L)
        );

        assertEquals(
                0,
                manager.getPurchasedExpansions(1L)
        );

        assertEquals(
                20.0,
                manager.getNextExpansionCost(1L)
        );
    }

    @Test
    void cityCannotExpandOutsideSevenBySevenArea() {

        assertTrue(
                TerritoryExpansionConfig.isWithinCityLimits(
                        10, 10, 13, 13
                )
        );

        assertTrue(
                TerritoryExpansionConfig.isWithinCityLimits(
                        10, 10, 7, 7
                )
        );

        assertFalse(
                TerritoryExpansionConfig.isWithinCityLimits(
                        10, 10, 14, 10
                )
        );

        assertFalse(
                TerritoryExpansionConfig.isWithinCityLimits(
                        10, 10, 10, 6
                )
        );
    }
}
