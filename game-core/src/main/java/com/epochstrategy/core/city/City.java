
package com.epochstrategy.core.city;

import com.epochstrategy.core.economy.ResourceInventory;

public final class City {

    private final long id;
    private final String name;

    private final int x;
    private final int y;

    private int population;
    private int level;

    private static final int INITIAL_STORAGE_CAPACITY = 200;

    private final ResourceInventory inventory;

    // Null means the city is neutral.
    private Long civilizationId;

    public City(
            long id,
            String name,
            int x,
            int y,
            int population,
            ResourceInventory inventory,
            Long civilizationId
    ) {
        this.inventory = new ResourceInventory(
                INITIAL_STORAGE_CAPACITY
        );

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "City ID must be positive"
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "City name cannot be empty"
            );
        }

        if (x < 0 || y < 0) {
            throw new IllegalArgumentException(
                    "City coordinates cannot be negative"
            );
        }

        if (population <= 0) {
            throw new IllegalArgumentException(
                    "City population must be positive"
            );
        }

        validateCivilizationId(civilizationId);

        this.id = id;
        this.name = name.trim();

        this.x = x;
        this.y = y;

        this.population = population;
        this.level = 1;

        this.civilizationId = civilizationId;
    }


    public City(
            long id,
            String name,
            int x,
            int y,
            int population,
            Long civilizationId
    ) {
        this(
                id,
                name,
                x,
                y,
                population,
                new ResourceInventory(200),
                civilizationId
        );
    }


    public boolean isNeutral() {
        return civilizationId == null;
    }

    public void setCivilizationId(Long civilizationId) {

        validateCivilizationId(civilizationId);

        this.civilizationId = civilizationId;
    }

    public void setPopulation(int population) {

        if (population <= 0) {
            throw new IllegalArgumentException(
                    "City population must be positive"
            );
        }

        this.population = population;
    }

    private static void validateCivilizationId(
            Long civilizationId
    ) {

        if (civilizationId != null
                && civilizationId <= 0) {

            throw new IllegalArgumentException(
                    "Civilization ID must be positive"
            );
        }
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getPopulation() {
        return population;
    }

    public int getLevel() {
        return level;
    }

    public Long getCivilizationId() {
        return civilizationId;
    }

    public ResourceInventory getInventory() {
        return inventory;
    }
}
