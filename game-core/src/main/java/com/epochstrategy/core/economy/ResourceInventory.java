
package com.epochstrategy.core.economy;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class ResourceInventory {

    private int capacity;

    private final EnumMap<ResourceType, Integer> resources =
            new EnumMap<>(ResourceType.class);

    private int totalUnits;

    public ResourceInventory(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be positive"
            );
        }

        this.capacity = capacity;
    }

    public void add(ResourceType type, int amount) {

        Objects.requireNonNull(type, "Resource type");

        validateAmount(amount);

        if (amount > getFreeCapacity()) {
            throw new IllegalArgumentException(
                    "Not enough inventory capacity"
            );
        }

        resources.merge(type, amount, Integer::sum);

        totalUnits += amount;
    }

    public void remove(ResourceType type, int amount) {

        Objects.requireNonNull(type, "Resource type");

        validateAmount(amount);

        if (getAmount(type) < amount) {
            throw new IllegalArgumentException(
                    "Not enough resources"
            );
        }

        int remaining = getAmount(type) - amount;

        if (remaining == 0) {
            resources.remove(type);
        } else {
            resources.put(type, remaining);
        }

        totalUnits -= amount;
    }

    public void transferTo(
            ResourceInventory destination,
            ResourceType type,
            int amount
    ) {

        Objects.requireNonNull(
                destination,
                "Destination inventory"
        );

        Objects.requireNonNull(type, "Resource type");

        validateAmount(amount);

        if (destination == this) {
            throw new IllegalArgumentException(
                    "Cannot transfer to same inventory"
            );
        }

        // Validate both sides before modifying anything
        if (getAmount(type) < amount) {
            throw new IllegalArgumentException(
                    "Not enough resources to transfer"
            );
        }

        if (destination.getFreeCapacity() < amount) {
            throw new IllegalArgumentException(
                    "Destination capacity exceeded"
            );
        }

        remove(type, amount);
        destination.add(type, amount);
    }

    private void validateAmount(int amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be positive"
            );
        }
    }

    public int getAmount(ResourceType type) {

        Objects.requireNonNull(type, "Resource type");

        return resources.getOrDefault(type, 0);
    }


    public void increaseCapacity(int amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Capacity increase must be positive"
            );
        }

        if (amount > Integer.MAX_VALUE - capacity) {
            throw new IllegalArgumentException(
                    "Inventory capacity overflow"
            );
        }

        capacity += amount;
    }


    public int getTotalUnits() {
        return totalUnits;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getFreeCapacity() {
        return capacity - totalUnits;
    }

    public Map<ResourceType, Integer> getContents() {
        return Map.copyOf(resources);
    }
}
