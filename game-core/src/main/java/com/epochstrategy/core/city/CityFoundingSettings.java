package com.epochstrategy.core.city;

public final class CityFoundingSettings {

    // oznacza: minimum 3 wolne pola pomiędzy miastami
    public static final int DEFAULT_MIN_CITY_DISTANCE = 3;

    private int minimumCityDistance =
            DEFAULT_MIN_CITY_DISTANCE;

    public int getMinimumCityDistance() {
        return minimumCityDistance;
    }

    public void setMinimumCityDistance(int distance) {

        if (distance < 0) {
            throw new IllegalArgumentException(
                    "Minimum city distance cannot be negative"
            );
        }

        minimumCityDistance = distance;
    }


    public boolean isDistanceAllowed(
            int firstX,
            int firstY,
            int secondX,
            int secondY
    ) {

        return countEmptyTilesBetween(
                firstX,
                firstY,
                secondX,
                secondY
        ) >= minimumCityDistance;
    }




    public int countEmptyTilesBetween(
            int firstX,
            int firstY,
            int secondX,
            int secondY
    ) {

        int centerDistance = Math.max(
                Math.abs(firstX - secondX),
                Math.abs(firstY - secondY)
        );

        return Math.max(0, centerDistance - 1);
    }

}