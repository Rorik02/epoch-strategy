
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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.civilization.Civilization;
import com.epochstrategy.core.civilization.CivilizationManager;
import com.epochstrategy.core.world.WorldMap;

import java.util.List;
import java.util.Objects;

public final class CityRenderer implements Disposable {

    private static final int NEUTRAL_COLOR = 0x808080;

    private final WorldMap worldMap;

    private final Model foundationModel;
    private final Model buildingModel;
    private final Model roofModel;
    private final Model ownershipMarkerModel;

    private final Array<ModelInstance> instances =
            new Array<>();

    public CityRenderer(WorldMap worldMap) {

        this.worldMap = Objects.requireNonNull(worldMap);

        ModelBuilder builder = new ModelBuilder();

        long attributes =
                VertexAttributes.Usage.Position
                        | VertexAttributes.Usage.Normal;

        // Stone foundation
        foundationModel = builder.createCylinder(
                0.68f,
                0.12f,
                0.68f,
                12,
                new Material(
                        ColorAttribute.createDiffuse(
                                new Color(
                                        0.45f, 0.42f, 0.36f, 1f
                                )
                        )
                ),
                attributes
        );

        // Main building
        buildingModel = builder.createBox(
                0.42f,
                0.36f,
                0.42f,
                new Material(
                        ColorAttribute.createDiffuse(
                                new Color(
                                        0.72f, 0.58f, 0.37f, 1f
                                )
                        )
                ),
                attributes
        );

        // Roof - recolored per civilization
        roofModel = builder.createCone(
                0.57f,
                0.25f,
                0.57f,
                4,
                new Material(
                        ColorAttribute.createDiffuse(Color.WHITE)
                ),
                attributes
        );

        // Circular ownership marker
        ownershipMarkerModel = builder.createCylinder(
                0.86f,
                0.035f,
                0.86f,
                16,
                new Material(
                        ColorAttribute.createDiffuse(Color.WHITE)
                ),
                attributes
        );
    }

    public void setCities(
            List<City> cities,
            CivilizationManager civilizationManager
    ) {

        Objects.requireNonNull(cities, "cities");
        Objects.requireNonNull(
                civilizationManager,
                "civilizationManager"
        );

        instances.clear();

        for (City city : cities) {

            float worldX =
                    city.getX()
                            - (worldMap.getWidth() - 1) / 2f;

            float worldZ =
                    city.getY()
                            - (worldMap.getHeight() - 1) / 2f;

            float terrainHeight =
                    TerrainMeshBuilder.surfaceHeightAt(
                            worldMap,
                            worldX,
                            worldZ
                    );

            Color civilizationColor =
                    getCivilizationColor(
                            city,
                            civilizationManager
                    );

            // Colored ring around the settlement
            ModelInstance marker = addInstance(
                    ownershipMarkerModel,
                    worldX,
                    terrainHeight + 0.02f,
                    worldZ
            );

            applyColor(
                    marker,
                    civilizationColor
            );

            // Foundation
            addInstance(
                    foundationModel,
                    worldX,
                    terrainHeight + 0.06f,
                    worldZ
            );

            // Building
            addInstance(
                    buildingModel,
                    worldX,
                    terrainHeight + 0.305f,
                    worldZ
            );

            // Roof in civilization color
            ModelInstance roof = addInstance(
                    roofModel,
                    worldX,
                    terrainHeight + 0.625f,
                    worldZ
            );

            applyColor(
                    roof,
                    civilizationColor
            );
        }
    }

    private Color getCivilizationColor(
            City city,
            CivilizationManager civilizationManager
    ) {

        int rgb = NEUTRAL_COLOR;

        if (!city.isNeutral()) {

            rgb = civilizationManager
                    .findCivilization(
                            city.getCivilizationId()
                    )
                    .map(Civilization::getColorRgb)
                    .orElse(NEUTRAL_COLOR);
        }

        float red =
                ((rgb >> 16) & 0xFF) / 255f;

        float green =
                ((rgb >> 8) & 0xFF) / 255f;

        float blue =
                (rgb & 0xFF) / 255f;

        return new Color(
                red,
                green,
                blue,
                1f
        );
    }

    private void applyColor(
            ModelInstance instance,
            Color color
    ) {

        instance.materials.first().set(
                ColorAttribute.createDiffuse(color)
        );
    }

    private ModelInstance addInstance(
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

        foundationModel.dispose();
        buildingModel.dispose();
        roofModel.dispose();
        ownershipMarkerModel.dispose();
    }
}
