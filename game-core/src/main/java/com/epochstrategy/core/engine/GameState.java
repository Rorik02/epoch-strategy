
package com.epochstrategy.core.engine;

import com.epochstrategy.core.world.RiverTransportManager;
import com.epochstrategy.core.city.CityManager;
import com.epochstrategy.core.civilization.CivilizationManager;
import com.epochstrategy.core.city.CityFoodService;
import java.util.Map;
import java.util.Objects;
import com.epochstrategy.core.city.CityResourceProductionService;
import com.epochstrategy.core.building.CityBuildingManager;
import com.epochstrategy.core.territory.TerritoryManager;
import com.epochstrategy.core.territory.CityInfluenceManager;
import com.epochstrategy.core.territory.CityExpansionManager;
import com.epochstrategy.core.civilization.PlayerCivilizationManager;
import com.epochstrategy.core.unit.LandUnitManager;
import com.epochstrategy.core.city.CityFoundingSettings;

public class GameState {

    private int turnNumber;
    private final GameCalendar calendar;

    private final RiverTransportManager riverTransportManager;
    private final CityManager cityManager = new CityManager();
    private final CivilizationManager civilizationManager = new CivilizationManager();
    private Map<Long, CityFoodService.FoodTurnResult>
            lastCityFoodResults = Map.of();

    private Map<Long, CityResourceProductionService.ResourceTurnResult>
            lastCityResourceResults = Map.of();

    public GameState() {
        this.turnNumber = 1;
        this.calendar = new GameCalendar();
        this.riverTransportManager =
                new RiverTransportManager();
    }

    private final CityBuildingManager cityBuildingManager =
            new CityBuildingManager();

    private final TerritoryManager territoryManager =
            new TerritoryManager();

    private final CityInfluenceManager cityInfluenceManager =
            new CityInfluenceManager();

    private final PlayerCivilizationManager playerCivilizationManager =
            new PlayerCivilizationManager();

    private final CityExpansionManager cityExpansionManager =
            new CityExpansionManager();

    private final LandUnitManager landUnitManager =
            new LandUnitManager();

    private final CityFoundingSettings cityFoundingSettings =
            new CityFoundingSettings();


    public int getTurnNumber() {
        return turnNumber;
    }

    public GameCalendar getCalendar() {
        return calendar;
    }

    public RiverTransportManager getRiverTransportManager() {
        return riverTransportManager;
    }

    public CityManager getCityManager() {
        return cityManager;
    }

    public CivilizationManager getCivilizationManager() {
        return civilizationManager;
    }

    void advanceTurn() {
        calendar.advance();
        turnNumber++;
    }


    public Map<Long, CityFoodService.FoodTurnResult>
    getLastCityFoodResults() {

        return lastCityFoodResults;
    }

    void setLastCityFoodResults(
            Map<Long, CityFoodService.FoodTurnResult> results
    ) {

        Objects.requireNonNull(results, "results");

        this.lastCityFoodResults = Map.copyOf(results);
    }


    public Map<Long, CityResourceProductionService.ResourceTurnResult>
    getLastCityResourceResults() {

        return lastCityResourceResults;
    }

    void setLastCityResourceResults(
            Map<Long, CityResourceProductionService.ResourceTurnResult> results
    ) {

        Objects.requireNonNull(results, "results");

        this.lastCityResourceResults = Map.copyOf(results);
    }

    public CityBuildingManager getCityBuildingManager() {
        return cityBuildingManager;
    }


    public TerritoryManager getTerritoryManager() {
        return territoryManager;
    }

    public CityInfluenceManager getCityInfluenceManager() {
        return cityInfluenceManager;
    }

    public CityExpansionManager getCityExpansionManager() {
        return cityExpansionManager;
    }

    public PlayerCivilizationManager getPlayerCivilizationManager() {
        return playerCivilizationManager;
    }

    public LandUnitManager getLandUnitManager() {
        return landUnitManager;
    }

    public CityFoundingSettings getCityFoundingSettings() {
        return cityFoundingSettings;
    }

}
