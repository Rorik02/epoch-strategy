
package com.epochstrategy.core.civilization;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CivilizationManager {

    private final Map<Long, Civilization> civilizations =
            new LinkedHashMap<>();

    private final Map<String, Long> civilizationIdsByName =
            new HashMap<>();

    public void registerCivilization(
            Civilization civilization
    ) {

        Objects.requireNonNull(
                civilization,
                "civilization"
        );

        if (civilizations.containsKey(civilization.getId())) {
            throw new IllegalArgumentException(
                    "Civilization ID already exists: "
                            + civilization.getId()
            );
        }

        String nameKey = normalizeName(
                civilization.getName()
        );

        if (civilizationIdsByName.containsKey(nameKey)) {
            throw new IllegalArgumentException(
                    "Civilization name already exists: "
                            + civilization.getName()
            );
        }

        civilizations.put(
                civilization.getId(),
                civilization
        );

        civilizationIdsByName.put(
                nameKey,
                civilization.getId()
        );
    }

    public Optional<Civilization> findCivilization(
            long civilizationId
    ) {
        return Optional.ofNullable(
                civilizations.get(civilizationId)
        );
    }

    public Optional<Civilization> findCivilizationByName(
            String name
    ) {

        Objects.requireNonNull(name, "name");

        Long civilizationId = civilizationIdsByName.get(
                normalizeName(name)
        );

        if (civilizationId == null) {
            return Optional.empty();
        }

        return findCivilization(civilizationId);
    }

    public void assignCity(
            long cityId,
            long civilizationId,
            CityManager cityManager
    ) {

        Objects.requireNonNull(
                cityManager,
                "cityManager"
        );

        Civilization civilization =
                findCivilization(civilizationId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Civilization not found: "
                                                + civilizationId
                                )
                        );

        City city = cityManager.findCity(cityId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "City not found: " + cityId
                        )
                );

        city.setCivilizationId(
                civilization.getId()
        );
    }

    public void makeCityNeutral(
            long cityId,
            CityManager cityManager
    ) {

        Objects.requireNonNull(
                cityManager,
                "cityManager"
        );

        City city = cityManager.findCity(cityId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "City not found: " + cityId
                        )
                );

        city.setCivilizationId(null);
    }

    public List<Civilization> getCivilizations() {
        return List.copyOf(civilizations.values());
    }

    public int getCivilizationCount() {
        return civilizations.size();
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
