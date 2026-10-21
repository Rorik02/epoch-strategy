
package com.epochstrategy.core.territory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CityExpansionManager {

    private final Map<Long, CityExpansionMode> modesByCity =
            new HashMap<>();

    public CityExpansionMode getMode(long cityId) {

        validateCityId(cityId);

        return modesByCity.getOrDefault(
                cityId,
                CityExpansionMode.MANUAL
        );
    }

    public void setMode(
            long cityId,
            CityExpansionMode mode
    ) {

        validateCityId(cityId);
        Objects.requireNonNull(mode, "mode");

        if (mode == CityExpansionMode.MANUAL) {
            modesByCity.remove(cityId);
        } else {
            modesByCity.put(cityId, mode);
        }
    }

    public boolean isAutomatic(long cityId) {

        return getMode(cityId)
                == CityExpansionMode.AUTOMATIC;
    }

    public CityExpansionMode toggleMode(long cityId) {

        CityExpansionMode nextMode =
                isAutomatic(cityId)
                        ? CityExpansionMode.MANUAL
                        : CityExpansionMode.AUTOMATIC;

        setMode(cityId, nextMode);

        return nextMode;
    }

    public void removeCity(long cityId) {

        modesByCity.remove(cityId);
    }

    private void validateCityId(long cityId) {

        if (cityId <= 0) {
            throw new IllegalArgumentException(
                    "City ID must be positive"
            );
        }
    }
}
