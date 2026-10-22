
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

import com.epochstrategy.core.civilization.CivilizationManager;
import com.epochstrategy.core.unit.LandUnit;
import com.epochstrategy.core.unit.LandUnitType;
import com.epochstrategy.core.world.WorldMap;

import java.util.List;
import java.util.Objects;

public final class SettlerRenderer implements Disposable {

    private static final long ATTRIBUTES =
            VertexAttributes.Usage.Position
                    | VertexAttributes.Usage.Normal;

    private final WorldMap worldMap;

    private final Model bodyModel;
    private final Model headModel;
    private final Model staffModel;
    private final Model flagModel;

    private final Array<ModelInstance> instances =
            new Array<>();

    public SettlerRenderer(WorldMap worldMap) {

        this.worldMap =
                Objects.requireNonNull(worldMap);

        ModelBuilder builder = new ModelBuilder();

        bodyModel = builder.createCone(
                0.30f, 0.46f, 0.30f, 8,
                material(new Color(
                        0.68f, 0.57f, 0.40f, 1f
                )),
                ATTRIBUTES
        );

        headModel = builder.createSphere(
                0.21f, 0.21f, 0.21f,
                10, 8,
                material(new Color(
                        0.94f, 0.76f, 0.58f, 1f
                )),
                ATTRIBUTES
        );

        staffModel = builder.createCylinder(
                0.035f, 0.70f, 0.035f, 8,
                material(new Color(
                        0.42f, 0.24f, 0.12f, 1f
                )),
                ATTRIBUTES
        );

        flagModel = builder.createBox(
                0.32f, 0.18f, 0.035f,
                material(Color.WHITE),
                ATTRIBUTES
        );
    }

    public void refresh(
            List<LandUnit> units,
            CivilizationManager civilizationManager
    ) {

        Objects.requireNonNull(units, "units");

        Objects.requireNonNull(
                civilizationManager,
                "civilizationManager"
        );

        instances.clear();

        for (LandUnit unit : units) {

            if (unit.getType() != LandUnitType.SETTLER) {
                continue;
            }

            int rgb = civilizationManager
                    .findCivilization(
                            unit.getCivilizationId()
                    )
                    .orElseThrow(
                            () -> new IllegalStateException(
                                    "Unknown unit civilization"
                            )
                    )
                    .getColorRgb();

            addSettler(unit, rgb);
        }
    }

    private void addSettler(
            LandUnit unit,
            int civilizationRgb
    ) {

        // A small offset makes the settler visible
        // beside the city standing on the same tile.
        float worldX =
                unit.getX()
                        - (worldMap.getWidth() - 1) / 2f
                        + 0.28f;

        float worldZ =
                unit.getY()
                        - (worldMap.getHeight() - 1) / 2f
                        - 0.25f;

        float groundY =
                TerrainMeshBuilder.surfaceHeightAt(
                        worldMap,
                        worldX,
                        worldZ
                );

        addInstance(
                bodyModel,
                worldX,
                groundY + 0.25f,
                worldZ
        );

        addInstance(
                headModel,
                worldX,
                groundY + 0.57f,
                worldZ
        );

        addInstance(
                staffModel,
                worldX + 0.20f,
                groundY + 0.38f,
                worldZ
        );

        ModelInstance flag = addInstance(
                flagModel,
                worldX + 0.33f,
                groundY + 0.65f,
                worldZ
        );

        flag.materials.first().set(
                ColorAttribute.createDiffuse(
                        colorFromRgb(civilizationRgb)
                )
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

        instance.transform.setToTranslation(x, y, z);

        instances.add(instance);

        return instance;
    }

    private Material material(Color color) {

        return new Material(
                ColorAttribute.createDiffuse(color)
        );
    }

    private Color colorFromRgb(int rgb) {

        return new Color(
                ((rgb >> 16) & 0xFF) / 255f,
                ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f,
                1f
        );
    }

    public void render(
            ModelBatch modelBatch,
            Environment environment
    ) {

        if (instances.size > 0) {
            modelBatch.render(
                    instances,
                    environment
            );
        }
    }

    @Override
    public void dispose() {

        instances.clear();

        bodyModel.dispose();
        headModel.dispose();
        staffModel.dispose();
        flagModel.dispose();
    }
}
