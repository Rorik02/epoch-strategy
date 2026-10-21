
package com.epochstrategy.desktop.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import com.epochstrategy.core.building.BuildingType;
import com.epochstrategy.core.building.CityBuildingManager;
import com.epochstrategy.core.city.City;
import com.epochstrategy.core.world.WorldMap;

import java.util.List;
import java.util.Objects;

public final class CityBuildingRenderer implements Disposable {

    private static final long ATTRIBUTES =
            VertexAttributes.Usage.Position
                    | VertexAttributes.Usage.Normal;

    private final WorldMap worldMap;

    private final Array<Model> models = new Array<>();
    private final Array<ModelInstance> instances = new Array<>();

    private final Model farmSoil;
    private final Model farmCrops;

    private final Model sawmillBody;
    private final Model sawmillRoof;
    private final Model sawmillLog;

    private final Model quarryStone;

    private final Model mineBody;
    private final Model mineRoof;
    private final Model mineChimney;

    private final Model warehouseBody;
    private final Model warehouseRoof;

    private final Model housingBody;
    private final Model housingRoof;

    public CityBuildingRenderer(WorldMap worldMap) {

        this.worldMap =
                Objects.requireNonNull(worldMap);

        ModelBuilder builder = new ModelBuilder();

        // FARM
        farmSoil = createBox(
                builder,
                0.56f, 0.04f, 0.48f,
                new Color(0.42f, 0.30f, 0.17f, 1f)
        );

        farmCrops = createBox(
                builder,
                0.43f, 0.06f, 0.065f,
                new Color(0.35f, 0.75f, 0.20f, 1f)
        );

        // SAWMILL
        sawmillBody = createBox(
                builder,
                0.38f, 0.32f, 0.36f,
                new Color(0.48f, 0.30f, 0.15f, 1f)
        );

        sawmillRoof = createCone(
                builder,
                0.51f, 0.19f, 0.51f,
                new Color(0.59f, 0.19f, 0.12f, 1f)
        );

        sawmillLog = createCylinder(
                builder,
                0.10f, 0.32f, 0.10f,
                new Color(0.58f, 0.36f, 0.17f, 1f)
        );

        // QUARRY
        quarryStone = createBox(
                builder,
                0.22f, 0.22f, 0.22f,
                new Color(0.56f, 0.55f, 0.51f, 1f)
        );

        // IRON MINE
        mineBody = createBox(
                builder,
                0.40f, 0.34f, 0.34f,
                new Color(0.24f, 0.25f, 0.28f, 1f)
        );

        mineRoof = createCone(
                builder,
                0.52f, 0.18f, 0.48f,
                new Color(0.15f, 0.16f, 0.19f, 1f)
        );

        mineChimney = createBox(
                builder,
                0.11f, 0.40f, 0.11f,
                new Color(0.40f, 0.24f, 0.20f, 1f)
        );

        // WAREHOUSE
        warehouseBody = createBox(
                builder,
                0.56f, 0.40f, 0.46f,
                new Color(0.67f, 0.49f, 0.25f, 1f)
        );

        warehouseRoof = createBox(
                builder,
                0.64f, 0.08f, 0.54f,
                new Color(0.26f, 0.30f, 0.34f, 1f)
        );

        // HOUSING
        housingBody = createBox(
                builder,
                0.26f, 0.28f, 0.26f,
                new Color(0.84f, 0.77f, 0.61f, 1f)
        );

        housingRoof = createCone(
                builder,
                0.38f, 0.17f, 0.38f,
                new Color(0.68f, 0.28f, 0.17f, 1f)
        );
    }

    public void refresh(
            List<City> cities,
            CityBuildingManager buildingManager
    ) {

        Objects.requireNonNull(cities, "cities");
        Objects.requireNonNull(
                buildingManager,
                "buildingManager"
        );

        instances.clear();

        for (City city : cities) {

            float centerX =
                    city.getX()
                            - (worldMap.getWidth() - 1) / 2f;

            float centerZ =
                    city.getY()
                            - (worldMap.getHeight() - 1) / 2f;

            // Stable positions around the city center.
            for (BuildingType type : BuildingType.values()) {

                if (!buildingManager.hasBuilding(
                        city.getId(),
                        type
                )) {
                    continue;
                }

                switch (type) {

                    case FARM -> renderFarm(
                            centerX - 0.78f,
                            centerZ
                    );

                    case SAWMILL -> renderSawmill(
                            centerX + 0.78f,
                            centerZ
                    );

                    case QUARRY -> renderQuarry(
                            centerX,
                            centerZ - 0.78f
                    );

                    case IRON_MINE -> renderMine(
                            centerX,
                            centerZ + 0.78f
                    );

                    case WAREHOUSE -> renderWarehouse(
                            centerX - 0.70f,
                            centerZ - 0.70f
                    );

                    case HOUSING -> renderHousing(
                            centerX + 0.70f,
                            centerZ + 0.70f
                    );
                }
            }
        }
    }

    private void renderFarm(float x, float z) {

        float y = terrainHeight(x, z);

        add(farmSoil, x, y + 0.06f, z);

        // Three visible rows of crops.
        for (int i = -1; i <= 1; i++) {

            add(
                    farmCrops,
                    x,
                    y + 0.12f,
                    z + i * 0.14f
            );
        }
    }

    private void renderSawmill(float x, float z) {

        float y = terrainHeight(x, z);

        add(
                sawmillBody,
                x,
                y + 0.20f,
                z
        );

        add(
                sawmillRoof,
                x,
                y + 0.45f,
                z
        );

        // Timber stored beside the sawmill.
        for (int i = 0; i < 2; i++) {

            ModelInstance log = add(
                    sawmillLog,
                    x + 0.30f,
                    y + 0.11f,
                    z - 0.09f + i * 0.16f
            );

            log.transform.rotate(
                    Vector3.X,
                    90f
            );
        }
    }

    private void renderQuarry(float x, float z) {

        float y = terrainHeight(x, z);

        add(
                quarryStone,
                x - 0.16f,
                y + 0.13f,
                z
        );

        add(
                quarryStone,
                x + 0.12f,
                y + 0.13f,
                z - 0.12f
        );

        add(
                quarryStone,
                x + 0.10f,
                y + 0.35f,
                z + 0.12f
        );
    }

    private void renderMine(float x, float z) {

        float y = terrainHeight(x, z);

        add(
                mineBody,
                x,
                y + 0.22f,
                z
        );

        add(
                mineRoof,
                x,
                y + 0.48f,
                z
        );

        add(
                mineChimney,
                x + 0.29f,
                y + 0.29f,
                z
        );
    }

    private void renderWarehouse(float x, float z) {

        float y = terrainHeight(x, z);

        add(
                warehouseBody,
                x,
                y + 0.24f,
                z
        );

        add(
                warehouseRoof,
                x,
                y + 0.48f,
                z
        );
    }

    private void renderHousing(float x, float z) {

        float y = terrainHeight(x, z);

        // Two smaller houses.
        for (int i = 0; i < 2; i++) {

            float houseX =
                    x + (i == 0 ? -0.18f : 0.18f);

            add(
                    housingBody,
                    houseX,
                    y + 0.18f,
                    z
            );

            add(
                    housingRoof,
                    houseX,
                    y + 0.40f,
                    z
            );
        }
    }

    private float terrainHeight(float x, float z) {

        return TerrainMeshBuilder.surfaceHeightAt(
                worldMap,
                x,
                z
        );
    }

    private ModelInstance add(
            Model model,
            float x,
            float y,
            float z
    ) {

        ModelInstance instance =
                new ModelInstance(model);

        instance.transform.setToTranslation(
                x,
                y,
                z
        );

        instances.add(instance);

        return instance;
    }

    private Model createBox(
            ModelBuilder builder,
            float width,
            float height,
            float depth,
            Color color
    ) {

        Model model = builder.createBox(
                width,
                height,
                depth,
                new Material(
                        ColorAttribute.createDiffuse(color)
                ),
                ATTRIBUTES
        );

        models.add(model);
        return model;
    }

    private Model createCone(
            ModelBuilder builder,
            float width,
            float height,
            float depth,
            Color color
    ) {

        Model model = builder.createCone(
                width,
                height,
                depth,
                4,
                new Material(
                        ColorAttribute.createDiffuse(color)
                ),
                ATTRIBUTES
        );

        models.add(model);
        return model;
    }

    private Model createCylinder(
            ModelBuilder builder,
            float width,
            float height,
            float depth,
            Color color
    ) {

        Model model = builder.createCylinder(
                width,
                height,
                depth,
                10,
                new Material(
                        ColorAttribute.createDiffuse(color)
                ),
                ATTRIBUTES
        );

        models.add(model);
        return model;
    }

    public void render(
            ModelBatch modelBatch,
            Environment environment
    ) {

        if (instances.size == 0) {
            return;
        }

        modelBatch.render(
                instances,
                environment
        );
    }

    @Override
    public void dispose() {

        instances.clear();

        for (Model model : models) {
            model.dispose();
        }

        models.clear();
    }
}
