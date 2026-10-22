
package com.epochstrategy.core.unit;

import java.util.Objects;

public final class LandUnit {

    private final long id;
    private final long civilizationId;
    private final LandUnitType type;

    private int x;
    private int y;

    private int movementPoints;

    LandUnit(
            long id,
            long civilizationId,
            LandUnitType type,
            int x,
            int y
    ) {

        if (id <= 0 || civilizationId <= 0) {
            throw new IllegalArgumentException(
                    "IDs must be positive"
            );
        }

        if (x < 0 || y < 0) {
            throw new IllegalArgumentException(
                    "Coordinates cannot be negative"
            );
        }

        this.id = id;
        this.civilizationId = civilizationId;
        this.type = Objects.requireNonNull(type);

        this.x = x;
        this.y = y;

        this.movementPoints = type.getMaxMovementPoints();
    }

    public long getId() {
        return id;
    }

    public long getCivilizationId() {
        return civilizationId;
    }

    public LandUnitType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getMovementPoints() {
        return movementPoints;
    }

    public int getMaxMovementPoints() {
        return type.getMaxMovementPoints();
    }

    public boolean canMove() {
        return movementPoints > 0;
    }

    // Package-private: movement will be validated
    // by a dedicated service in the next stage.
    void moveTo(
            int targetX,
            int targetY,
            int movementCost
    ) {

        if (targetX < 0 || targetY < 0) {
            throw new IllegalArgumentException(
                    "Invalid destination"
            );
        }

        if (movementCost <= 0
                || movementCost > movementPoints) {
            throw new IllegalArgumentException(
                    "Invalid movement cost"
            );
        }

        x = targetX;
        y = targetY;
        movementPoints -= movementCost;
    }

    void restoreMovementPoints() {
        movementPoints = getMaxMovementPoints();
    }
}
