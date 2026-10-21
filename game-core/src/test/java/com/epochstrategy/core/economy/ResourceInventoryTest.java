
package com.epochstrategy.core.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceInventoryTest {

    @Test
    void shouldTransferResourcesBetweenInventories() {

        ResourceInventory source =
                new ResourceInventory(100);

        ResourceInventory destination =
                new ResourceInventory(50);

        source.add(ResourceType.WOOD, 25);

        source.transferTo(
                destination,
                ResourceType.WOOD,
                10
        );

        assertEquals(
                15,
                source.getAmount(ResourceType.WOOD)
        );

        assertEquals(
                10,
                destination.getAmount(ResourceType.WOOD)
        );

        // No resources were created or lost
        assertEquals(
                25,
                source.getTotalUnits()
                        + destination.getTotalUnits()
        );
    }

    @Test
    void shouldRejectTransferWhenCapacityExceeded() {

        ResourceInventory source =
                new ResourceInventory(100);

        ResourceInventory destination =
                new ResourceInventory(5);

        source.add(ResourceType.IRON, 20);

        destination.add(ResourceType.WOOD, 5);

        assertThrows(
                IllegalArgumentException.class,
                () -> source.transferTo(
                        destination,
                        ResourceType.IRON,
                        10
                )
        );

        // Failed transfer must not change inventories
        assertEquals(
                20,
                source.getAmount(ResourceType.IRON)
        );

        assertEquals(
                0,
                destination.getAmount(ResourceType.IRON)
        );

        assertEquals(
                5,
                destination.getAmount(ResourceType.WOOD)
        );
    }

    @Test
    void shouldRejectNegativeResources() {

        ResourceInventory inventory =
                new ResourceInventory(50);

        assertThrows(
                IllegalArgumentException.class,
                () -> inventory.add(
                        ResourceType.FOOD,
                        -10
                )
        );
    }
}
