
package com.epochstrategy.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.epochstrategy.core.world.DemoMapFactory;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;
import com.epochstrategy.core.world.ProceduralMapGenerator;
import com.epochstrategy.core.world.MapType;
import com.epochstrategy.core.world.TerrainType;
import java.util.Random;
import com.epochstrategy.desktop.renderer.TerrainMeshBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.epochstrategy.desktop.renderer.TerrainChunkManager;
import com.epochstrategy.core.world.RiverGenerator;
import com.epochstrategy.core.world.RiverNetwork;
import com.epochstrategy.desktop.renderer.RiverRenderer;


public class EpochGame extends ApplicationAdapter {

    private WorldMap worldMap;
    private Tile selectedTile;

    private PerspectiveCamera camera;
    private CameraInputController cameraController;

    private ModelBatch modelBatch;
    private Environment environment;
    private TerrainChunkManager terrainChunkManager;
    private RiverNetwork riverNetwork;
    private RiverRenderer riverRenderer;


    private static final int CHUNK_SIZE = 8;

    private final Array<Model> models = new Array<>();
    private final Array<ModelInstance> tiles = new Array<>();


    private final Array<ModelInstance> selectionOutline = new Array<>();

    private Model selectionEdgeModel;

    private Stage uiStage;
    private BitmapFont font;
    private Texture panelTexture;
    private Label tileInfoLabel;

    @Override
    public void create() {


        worldMap = ProceduralMapGenerator.generate(
                30,
                20,
                1927L,
                MapType.ARCHIPELAGO
        );

        riverNetwork = RiverGenerator.generate(
                worldMap,
                12345L
        );

        long uphillSegments = riverNetwork.getSegments()
                .stream()
                .filter(segment -> {

                    int startElevation = worldMap.getTile(
                            segment.fromX(),
                            segment.fromY()
                    ).getElevation();

                    int endElevation = worldMap.getTile(
                            segment.toX(),
                            segment.toY()
                    ).getElevation();

                    return endElevation > startElevation;
                })
                .count();

        System.out.println(
                "River segments generated: "
                        + riverNetwork.getSegments().size()
        );

        System.out.println(
                "Uphill river segments: "
                        + uphillSegments
        );


        riverRenderer = new RiverRenderer(
                worldMap,
                riverNetwork
        );




        modelBatch = new ModelBatch();

        environment = new Environment();
        environment.set(new ColorAttribute(
                ColorAttribute.AmbientLight,
                0.7f, 0.7f, 0.7f, 1f
        ));

        environment.add(new DirectionalLight().set(
                Color.WHITE,
                -1f, -2f, -1f
        ));

        camera = new PerspectiveCamera(
                67,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );

        camera.position.set(0f, 32f, 36f);
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = 200f;
        camera.update();

        createWorldModels();
        createSelectionModels();
        createUI();

        setupInput();
    }



    private void createWorldModels() {

        // Create one terrain mesh from WorldMap


        terrainChunkManager = new TerrainChunkManager(
                worldMap,
                CHUNK_SIZE
        );

        System.out.println(
                "Terrain chunks created: "
                        + terrainChunkManager.getChunkCount()
        );



        // Create reusable tree models
        ModelBuilder builder = new ModelBuilder();

        long attributes =
                VertexAttributes.Usage.Position |
                        VertexAttributes.Usage.Normal;

        Model treeTrunk = builder.createCylinder(
                0.14f,
                0.38f,
                0.14f,
                8,
                new Material(
                        ColorAttribute.createDiffuse(
                                new Color(0.38f, 0.22f, 0.10f, 1f)
                        )
                ),
                attributes
        );

        Model treeCrown = builder.createCone(
                0.52f,
                0.85f,
                0.52f,
                7,
                new Material(
                        ColorAttribute.createDiffuse(
                                new Color(0.06f, 0.38f, 0.16f, 1f)
                        )
                ),
                attributes
        );

        models.addAll(treeTrunk, treeCrown);

        // Add trees only on forest tiles
        for (int x = 0; x < worldMap.getWidth(); x++) {
            for (int y = 0; y < worldMap.getHeight(); y++) {

                Tile tile = worldMap.getTile(x, y);

                if (tile.getTerrain() == TerrainType.FOREST) {

                    addForestTrees(
                            tile,
                            treeTrunk,
                            treeCrown
                    );
                }
            }
        }
    }



    private void addForestTrees(
            Tile tile,
            Model trunkModel,
            Model crownModel
    ) {

        long tileSeed = 12345L
                ^ ((long) tile.getX() * 73856093L)
                ^ ((long) tile.getY() * 19349663L);

        Random random = new Random(tileSeed);

        float worldX = getWorldX(tile.getX());
        float worldZ = getWorldZ(tile.getY());
        float groundHeight = getGroundHeight(tile);

        float[][] positions = {
                {-0.27f, -0.25f},
                {0.25f, -0.14f},
                {0.03f, 0.24f}
        };

        for (float[] position : positions) {

            float treeX = worldX + position[0]
                    + (random.nextFloat() - 0.5f) * 0.12f;

            float treeZ = worldZ + position[1]
                    + (random.nextFloat() - 0.5f) * 0.12f;

            float scale = 0.75f + random.nextFloat() * 0.30f;

            // Tree trunk
            ModelInstance trunk = new ModelInstance(trunkModel);

            trunk.transform.setToTranslation(
                    treeX,
                    groundHeight + 0.19f * scale,
                    treeZ
            );

            trunk.transform.scale(scale, scale, scale);

            tiles.add(trunk);

            // Tree crown
            ModelInstance crown = new ModelInstance(crownModel);

            crown.transform.setToTranslation(
                    treeX,
                    groundHeight + 0.60f * scale,
                    treeZ
            );

            crown.transform.scale(scale, scale, scale);

            tiles.add(crown);
        }
    }




    private void createSelectionModels() {

        ModelBuilder builder = new ModelBuilder();

        long attributes =
                VertexAttributes.Usage.Position |
                        VertexAttributes.Usage.Normal;

        Material gold = new Material(
                ColorAttribute.createDiffuse(
                        new Color(1f, 0.75f, 0.1f, 1f)
                )
        );

        selectionEdgeModel = builder.createCylinder(
                1f,
                1f,
                1f,
                8,
                gold,
                attributes
        );

        models.add(selectionEdgeModel);
    }


    private void createUI() {

        uiStage = new Stage(new ScreenViewport());

        font = new BitmapFont();
        font.getData().setScale(1.3f);

        tileInfoLabel = new Label(
                "Select a tile to view information",
                new Label.LabelStyle(font, Color.WHITE)
        );

        Pixmap pixmap = new Pixmap(
                1, 1, Pixmap.Format.RGBA8888
        );

        pixmap.setColor(0.06f, 0.09f, 0.15f, 0.9f);
        pixmap.fill();

        panelTexture = new Texture(pixmap);
        pixmap.dispose();

        Table panel = new Table();

        panel.setBackground(
                new TextureRegionDrawable(
                        new TextureRegion(panelTexture)
                )
        );

        panel.pad(15);
        panel.add(tileInfoLabel).left();

        Table root = new Table();

        root.setFillParent(true);
        root.top().left();
        root.pad(20);
        root.setTouchable(Touchable.disabled);

        root.add(panel).top().left();

        uiStage.addActor(root);
    }

    private void setupInput() {

        cameraController = new CameraInputController(camera);

        cameraController.rotateButton = Input.Buttons.RIGHT;
        cameraController.translateButton = Input.Buttons.MIDDLE;
        cameraController.forwardButton = -1;

        InputAdapter tileInput = new InputAdapter() {

            @Override
            public boolean touchDown(
                    int screenX,
                    int screenY,
                    int pointer,
                    int button
            ) {

                if (button == Input.Buttons.LEFT) {
                    selectTile(screenX, screenY);
                    return true;
                }

                return false;
            }
        };

        Gdx.input.setInputProcessor(
                new InputMultiplexer(
                        tileInput,
                        cameraController
                )
        );
    }


    private void selectTile(int screenX, int screenY) {

        Ray ray = camera.getPickRay(screenX, screenY);

        Tile hitTile = null;
        float closestDistance = Float.MAX_VALUE;

        Vector3 intersection = new Vector3();

        int[][] triangles = {
                {0, 4, 1},
                {1, 4, 2},
                {2, 4, 3},
                {3, 4, 0}
        };

        for (int x = 0; x < worldMap.getWidth(); x++) {
            for (int y = 0; y < worldMap.getHeight(); y++) {

                Tile tile = worldMap.getTile(x, y);

                Vector3[] vertices =
                        TerrainMeshBuilder.getTileVertices(
                                worldMap,
                                tile
                        );

                for (int[] triangle : triangles) {

                    boolean hit = Intersector.intersectRayTriangle(
                            ray,
                            vertices[triangle[0]],
                            vertices[triangle[1]],
                            vertices[triangle[2]],
                            intersection
                    );

                    if (hit) {

                        float distance =
                                ray.origin.dst2(intersection);

                        if (distance < closestDistance) {

                            closestDistance = distance;
                            hitTile = tile;
                        }
                    }
                }
            }
        }

        selectedTile = hitTile;

        updateSelectionOutline();
        updateInfoPanel();
    }



    private void updateSelectionOutline() {

        selectionOutline.clear();

        if (selectedTile == null) {
            return;
        }

        Vector3[] vertices =
                TerrainMeshBuilder.getTileVertices(
                        worldMap,
                        selectedTile
                );

        for (int i = 0; i < 4; i++) {

            Vector3 start = new Vector3(vertices[i])
                    .add(0f, 0.035f, 0f);

            Vector3 end = new Vector3(vertices[(i + 1) % 4])
                    .add(0f, 0.035f, 0f);

            addOutlineEdge(start, end);
        }
    }



    private void addOutlineEdge(
            Vector3 start,
            Vector3 end
    ) {

        Vector3 direction = new Vector3(end).sub(start);

        float length = direction.len();

        if (length < 0.0001f) {
            return;
        }

        direction.nor();

        Vector3 midpoint = new Vector3(start)
                .add(end)
                .scl(0.5f);

        Quaternion rotation = new Quaternion()
                .setFromCross(Vector3.Y, direction);

        ModelInstance edge =
                new ModelInstance(selectionEdgeModel);

        edge.transform.set(
                midpoint,
                rotation,
                new Vector3(0.03f, length, 0.03f)
        );

        selectionOutline.add(edge);
    }


    private void updateInfoPanel() {

        if (selectedTile == null) {
            tileInfoLabel.setText(
                    "Select a tile to view information"
            );

            Gdx.graphics.setTitle("Epoch Strategy");
            return;
        }

        String information =
                "TILE INFORMATION\n\n"
                        + "Coordinates: "
                        + selectedTile.getX()
                        + ", "
                        + selectedTile.getY()
                        + "\n"
                        + "Terrain: "
                        + selectedTile.getTerrain()
                        + "\n"
                        + "Elevation: "
                        + selectedTile.getElevation()
                        + "\n"
                        + "Land passable: "
                        + (selectedTile.isPassableForLandUnits()
                        ? "Yes" : "No");

        tileInfoLabel.setText(information);

        Gdx.graphics.setTitle(
                "Epoch Strategy | "
                        + selectedTile.getTerrain()
        );
    }

    private float getWorldX(int x) {
        return x - (worldMap.getWidth() - 1) / 2f;
    }

    private float getWorldZ(int y) {
        return y - (worldMap.getHeight() - 1) / 2f;
    }



    private float getGroundHeight(Tile tile) {
        return TerrainMeshBuilder.surfaceHeight(tile);
    }



    @Override
    public void render() {

        cameraController.update();

        Gdx.gl.glClearColor(
                0.10f, 0.14f, 0.20f, 1f
        );

        Gdx.gl.glClear(
                GL20.GL_COLOR_BUFFER_BIT |
                        GL20.GL_DEPTH_BUFFER_BIT
        );



        camera.update();

        modelBatch.begin(camera);

        // Terrain
        terrainChunkManager.render(
                camera,
                modelBatch,
                environment
        );

        // Rivers
        riverRenderer.render(
                modelBatch,
                environment
        );

        // Trees and objects
        modelBatch.render(tiles, environment);

        // Selected tile outline
        modelBatch.render(selectionOutline, environment);

        modelBatch.end();



        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        uiStage.act(Gdx.graphics.getDeltaTime());
        uiStage.draw();
    }

    @Override
    public void resize(int width, int height) {

        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();

        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {

        terrainChunkManager.dispose();
        modelBatch.dispose();

        for (Model model : models) {
            model.dispose();
        }

        if (riverRenderer != null) {
            riverRenderer.dispose();
        }

        uiStage.dispose();
        font.dispose();
        panelTexture.dispose();
    }
}
