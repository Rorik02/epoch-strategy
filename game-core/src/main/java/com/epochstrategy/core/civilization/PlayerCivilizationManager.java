
package com.epochstrategy.core.civilization;

import com.epochstrategy.core.city.City;

import java.util.Objects;
import java.util.OptionalLong;

public final class PlayerCivilizationManager {

    private Long playerCivilizationId;

    public void selectCivilization(
            long civilizationId,
            CivilizationManager civilizationManager
    ) {

        Objects.requireNonNull(
                civilizationManager,
                "civilizationManager"
        );

        if (civilizationId <= 0) {
            throw new IllegalArgumentException(
                    "Civilization ID must be positive"
            );
        }

        if (civilizationManager.findCivilization(
                civilizationId
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Civilization does not exist: "
                            + civilizationId
            );
        }

        playerCivilizationId = civilizationId;
    }

    public OptionalLong getPlayerCivilizationId() {

        if (playerCivilizationId == null) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(playerCivilizationId);
    }

    public boolean isPlayerCivilization(
            Long civilizationId
    ) {

        return playerCivilizationId != null
                && playerCivilizationId.equals(civilizationId);
    }

    public boolean controlsCity(City city) {

        Objects.requireNonNull(city, "city");

        return isPlayerCivilization(
                city.getCivilizationId()
        );
    }

    public boolean hasSelectedCivilization() {
        return playerCivilizationId != null;
    }
}
