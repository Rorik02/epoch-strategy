
package com.epochstrategy.core.territory;

import com.epochstrategy.core.city.City;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CityInfluenceManager {

    private static final class InfluenceState {

        private long balanceHundredths;
        private int purchasedExpansions;
    }

    private final Map<Long, InfluenceState> influenceByCity =
            new HashMap<>();

    public void advanceTurn(
            Collection<City> cities
    ) {

        Objects.requireNonNull(cities, "cities");

        for (City city : cities) {
            advanceCityTurn(city);
        }
    }

    public void advanceCityTurn(
            City city
    ) {

        Objects.requireNonNull(city, "city");

        // Neutral cities do not generate
        // civilization influence.
        if (city.getCivilizationId() == null) {
            return;
        }

        long generated =
                TerritoryExpansionConfig.calculateIncome(
                        city.getPopulation()
                );

        InfluenceState state = getOrCreateState(
                city.getId()
        );

        state.balanceHundredths = Math.addExact(
                state.balanceHundredths,
                generated
        );
    }

    public long getBalanceHundredths(
            long cityId
    ) {

        InfluenceState state = influenceByCity.get(cityId);

        if (state == null) {
            return 0;
        }

        return state.balanceHundredths;
    }

    public double getBalance(
            long cityId
    ) {

        return getBalanceHundredths(cityId)
                / (double) TerritoryExpansionConfig.INFLUENCE_SCALE;
    }

    public long getIncomeHundredths(
            City city
    ) {

        Objects.requireNonNull(city, "city");

        if (city.getCivilizationId() == null) {
            return 0;
        }

        return TerritoryExpansionConfig.calculateIncome(
                city.getPopulation()
        );
    }

    public double getIncome(
            City city
    ) {

        return getIncomeHundredths(city)
                / (double) TerritoryExpansionConfig.INFLUENCE_SCALE;
    }

    public int getPurchasedExpansions(
            long cityId
    ) {

        InfluenceState state = influenceByCity.get(cityId);

        if (state == null) {
            return 0;
        }

        return state.purchasedExpansions;
    }

    public long getNextExpansionCostHundredths(
            long cityId
    ) {

        return TerritoryExpansionConfig.calculateExpansionCost(
                getPurchasedExpansions(cityId)
        );
    }

    public double getNextExpansionCost(
            long cityId
    ) {

        return getNextExpansionCostHundredths(cityId)
                / (double) TerritoryExpansionConfig.INFLUENCE_SCALE;
    }

    public boolean hasEnoughInfluence(
            long cityId
    ) {

        return getBalanceHundredths(cityId)
                >= getNextExpansionCostHundredths(cityId);
    }

    /**
     * Spend influence after an expansion target
     * has been validated.
     *
     * The territory expansion service will call
     * this method when the tile is purchased.
     */
    public boolean trySpendForExpansion(
            long cityId
    ) {

        if (!hasEnoughInfluence(cityId)) {
            return false;
        }

        InfluenceState state = getOrCreateState(cityId);

        long cost = getNextExpansionCostHundredths(
                cityId
        );

        state.balanceHundredths -= cost;
        state.purchasedExpansions++;

        return true;
    }

    public void removeCity(
            long cityId
    ) {

        influenceByCity.remove(cityId);
    }

    private InfluenceState getOrCreateState(
            long cityId
    ) {

        return influenceByCity.computeIfAbsent(
                cityId,
                ignored -> new InfluenceState()
        );
    }
}
