
package com.epochstrategy.core.civilization;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class CivilizationDemoInitializer {

    private CivilizationDemoInitializer() {
    }

    public static void initialize(
            CivilizationManager civilizationManager,
            CityManager cityManager
    ) {

        Objects.requireNonNull(
                civilizationManager,
                "civilizationManager"
        );

        Objects.requireNonNull(
                cityManager,
                "cityManager"
        );

        if (civilizationManager.getCivilizationCount() != 0) {
            throw new IllegalStateException(
                    "Demo civilizations already initialized"
            );
        }

        List<Civilization> civilizations = List.of(
                new Civilization(
                        1L, "Ancient Egypt", 0xDAA520
                ),
                new Civilization(
                        2L, "Rome", 0xB22222
                ),
                new Civilization(
                        3L, "Greece", 0x2864AA
                ),
                new Civilization(
                        4L, "Carthage", 0x7D3C98
                )
        );

        for (Civilization civilization : civilizations) {

            civilizationManager.registerCivilization(
                    civilization
            );
        }

        // Group cities geographically from west to east.
        List<City> cities = cityManager.getCities()
                .stream()
                .sorted(
                        Comparator.comparingInt(City::getX)
                                .thenComparingInt(City::getY)
                )
                .toList();

        if (cities.isEmpty()) {
            return;
        }

        for (int i = 0; i < cities.size(); i++) {

            int civilizationIndex = (int) (
                    (long) i * civilizations.size()
                            / cities.size()
            );

            Civilization owner =
                    civilizations.get(civilizationIndex);

            civilizationManager.assignCity(
                    cities.get(i).getId(),
                    owner.getId(),
                    cityManager
            );
        }
    }
}
