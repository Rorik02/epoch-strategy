
package com.epochstrategy.desktop.renderer;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;

import com.epochstrategy.core.world.WorldMap;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TerrainChunkManager implements Disposable {

    private final WorldMap worldMap;
    private final int chunkSize;

    private final Map<ChunkPosition, Chunk> chunks =
            new LinkedHashMap<>();

    private int visibleChunkCount;
    private int lastLoggedVisibleCount = -1;

    public TerrainChunkManager(
            WorldMap worldMap,
            int chunkSize
    ) {

        this.worldMap = Objects.requireNonNull(worldMap);

        if (chunkSize <= 0) {
            throw new IllegalArgumentException(
                    "Chunk size must be positive"
            );
        }

        this.chunkSize = chunkSize;

        generateChunks();
    }

    private void generateChunks() {

        for (int x = 0;
             x < worldMap.getWidth();
             x += chunkSize) {

            for (int y = 0;
                 y < worldMap.getHeight();
                 y += chunkSize) {

                rebuildChunk(
                        new ChunkPosition(x, y)
                );
            }
        }
    }

    private void rebuildChunk(ChunkPosition position) {

        Model model = TerrainMeshBuilder.buildChunk(
                worldMap,
                position.x(),
                position.y(),
                chunkSize
        );

        ModelInstance instance = new ModelInstance(model);

        // Calculate the chunk's 3D bounding box once
        BoundingBox bounds = model.calculateBoundingBox(
                new BoundingBox()
        );

        Chunk previous = chunks.put(
                position,
                new Chunk(model, instance, bounds)
        );

        if (previous != null) {
            previous.model().dispose();
        }
    }

    public void refreshAroundTile(int tileX, int tileY) {

        if (!worldMap.isInsideMap(tileX, tileY)) {
            throw new IllegalArgumentException(
                    "Tile outside the map"
            );
        }

        Set<ChunkPosition> affectedChunks =
                new LinkedHashSet<>();

        for (int x = Math.max(0, tileX - 1);
             x <= Math.min(worldMap.getWidth() - 1, tileX + 1);
             x++) {

            for (int y = Math.max(0, tileY - 1);
                 y <= Math.min(worldMap.getHeight() - 1, tileY + 1);
                 y++) {

                int chunkX = (x / chunkSize) * chunkSize;
                int chunkY = (y / chunkSize) * chunkSize;

                affectedChunks.add(
                        new ChunkPosition(chunkX, chunkY)
                );
            }
        }

        for (ChunkPosition position : affectedChunks) {
            rebuildChunk(position);
        }
    }

    public void render(
            Camera camera,
            ModelBatch modelBatch,
            Environment environment
    ) {

        visibleChunkCount = 0;

        for (Chunk chunk : chunks.values()) {

            // Skip chunks outside the camera's view
            if (!camera.frustum.boundsInFrustum(
                    chunk.bounds()
            )) {
                continue;
            }

            modelBatch.render(
                    chunk.instance(),
                    environment
            );

            visibleChunkCount++;
        }

        // Log only when the visible count changes
        if (visibleChunkCount != lastLoggedVisibleCount) {

            System.out.println(
                    "Visible terrain chunks: "
                            + visibleChunkCount
                            + " / "
                            + chunks.size()
            );

            lastLoggedVisibleCount = visibleChunkCount;
        }
    }

    public int getChunkCount() {
        return chunks.size();
    }

    public int getVisibleChunkCount() {
        return visibleChunkCount;
    }

    @Override
    public void dispose() {

        for (Chunk chunk : chunks.values()) {
            chunk.model().dispose();
        }

        chunks.clear();
    }

    private record ChunkPosition(int x, int y) {
    }

    private record Chunk(
            Model model,
            ModelInstance instance,
            BoundingBox bounds
    ) {
    }
}
