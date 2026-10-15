package com.epochstrategy.desktop;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.epochstrategy.core.economy.ResourceInventory;
import com.epochstrategy.core.economy.ResourceType;
import com.epochstrategy.core.engine.GameEngine;
import com.epochstrategy.core.engine.GameState;
import com.epochstrategy.core.world.*;
import com.epochstrategy.desktop.renderer.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/** Desktop UI and rendering. All authoritative turn and cargo state live in game-core. */
public class EpochGame extends ApplicationAdapter {

    private static final int MAP_WIDTH = 100;
    private static final int MAP_HEIGHT = 100;
    private static final int CHUNK_SIZE = 16;
    private static final long MAP_SEED = 1927L;
    private static final long LAKE_SEED = 98765L;
    private static final long RIVER_SEED = 12345L;
    private static final MapType MAP_TYPE = MapType.PANGAEA;

    private WorldMap worldMap;
    private DrainageMap drainageMap;
    private RiverNetwork riverNetwork;
    private Tile selectedTile;
    private Tile riverRouteStart;

    private GameState gameState;
    private GameEngine gameEngine;
    private long nextRiverUnitId = 1L;
    private RiverTransportUnit selectedTransportUnit;

    private final Map<
            Long,
            WaterwayFastestRouteFinder.WaterwayRoute
            > riverRoutesByUnit = new HashMap<>();

    private final Map<Long, ResourceInventory> demoDestinationsByUnit =
            new HashMap<>();

    private String riverRouteStatus =
            "Shift + Left Click: select river route start";

    private PerspectiveCamera camera;
    private CameraInputController cameraController;
    private ModelBatch modelBatch;
    private Environment environment;
    private TerrainChunkManager terrainChunkManager;
    private RiverRenderer riverRenderer;
    private RiverRouteRenderer riverRouteRenderer;
    private RiverTransportFleetRenderer riverTransportRenderer;

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
                MAP_WIDTH, MAP_HEIGHT, MAP_SEED, MAP_TYPE
        );

        int generatedLakes = LakeGenerator.generate(worldMap, LAKE_SEED);
        System.out.println("Lakes generated: " + generatedLakes);

        drainageMap = WatershedCalculator.calculate(worldMap);
        printDrainageStatistics();

        riverNetwork = RiverGenerator.generate(
                worldMap, drainageMap, RIVER_SEED
        );
        printRiverStatistics();

        gameState = new GameState();
        gameEngine = new GameEngine(gameState, worldMap, riverNetwork);

        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(
                ColorAttribute.AmbientLight, 0.7f, 0.7f, 0.7f, 1f
        ));
        environment.add(new DirectionalLight().set(
                Color.WHITE, -1f, -2f, -1f
        ));

        camera = new PerspectiveCamera(
                67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()
        );
        float worldSize = Math.max(MAP_WIDTH, MAP_HEIGHT);
        camera.position.set(0f, worldSize * 0.92f, worldSize * 0.98f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f;
        camera.far = Math.max(300f, worldSize * 5f);
        camera.update();

        riverRenderer = new RiverRenderer(worldMap, riverNetwork);
        riverRouteRenderer = new RiverRouteRenderer(worldMap);
        riverTransportRenderer = new RiverTransportFleetRenderer(worldMap);

        createWorldModels();
        createSelectionModels();
        createUI();
        setupInput();
        updateInfoPanel();
    }

    private void printDrainageStatistics() {
        int withDrainage = 0;
        int withoutDrainage = 0;
        int largestCatchment = 0;

        for (int x = 0; x < worldMap.getWidth(); x++) {
            for (int y = 0; y < worldMap.getHeight(); y++) {
                TerrainType terrain = worldMap.getTile(x, y).getTerrain();
                if (terrain == TerrainType.OCEAN || terrain == TerrainType.LAKE) {
                    continue;
                }
                if (drainageMap.hasDrainagePath(x, y)) {
                    withDrainage++;
                    largestCatchment = Math.max(
                            largestCatchment,
                            drainageMap.getCatchmentArea(x, y)
                    );
                } else {
                    withoutDrainage++;
                }
            }
        }

        System.out.println("Land tiles with drainage: " + withDrainage);
        System.out.println("Land tiles without drainage: " + withoutDrainage);
        System.out.println("Largest land catchment: " + largestCatchment);
    }

    private void printRiverStatistics() {
        int invalidSegments = 0;
        int largestFlow = 0;
        int navigableSegments = 0;
        int uphillSegments = 0;

        for (RiverNetwork.RiverSegment segment : riverNetwork.getSegments()) {
            int fromX = segment.fromX();
            int fromY = segment.fromY();
            int toX = segment.toX();
            int toY = segment.toY();

            if (drainageMap.getDownstreamX(fromX, fromY) != toX
                    || drainageMap.getDownstreamY(fromX, fromY) != toY) {
                invalidSegments++;
            }
            if (worldMap.getTile(toX, toY).getElevation()
                    > worldMap.getTile(fromX, fromY).getElevation()) {
                uphillSegments++;
            }

            RiverProperties properties = riverNetwork.getProperties(segment);
            if (properties != null) {
                largestFlow = Math.max(largestFlow, properties.flow());
                if (properties.navigable()) {
                    navigableSegments++;
                }
            }
        }

        System.out.println("River segments generated: "
                + riverNetwork.getSegments().size());
        System.out.println("Uphill river segments: " + uphillSegments);
        System.out.println("Invalid drainage segments: " + invalidSegments);
        System.out.println("Largest river flow: " + largestFlow);
        System.out.println("Navigable river segments: " + navigableSegments);

        for (RiverVesselType type : RiverVesselType.values()) {
            long available = riverNetwork.getSegments().stream()
                    .filter(segment -> RiverNavigationService.canNavigate(
                            worldMap, riverNetwork, segment, type))
                    .count();
            System.out.println(type + " accessible segments: " + available
                    + " / " + riverNetwork.getSegments().size());
        }
    }

    private void createWorldModels() {
        terrainChunkManager = new TerrainChunkManager(worldMap, CHUNK_SIZE);
        System.out.println("Terrain chunks created: "
                + terrainChunkManager.getChunkCount());

        ModelBuilder builder = new ModelBuilder();
        long attributes = VertexAttributes.Usage.Position
                | VertexAttributes.Usage.Normal;

        Model trunkModel = builder.createCylinder(
                0.14f, 0.38f, 0.14f, 8,
                new Material(ColorAttribute.createDiffuse(
                        new Color(0.38f, 0.22f, 0.10f, 1f))),
                attributes
        );
        Model crownModel = builder.createCone(
                0.52f, 0.85f, 0.52f, 7,
                new Material(ColorAttribute.createDiffuse(
                        new Color(0.06f, 0.38f, 0.16f, 1f))),
                attributes
        );
        models.add(trunkModel);
        models.add(crownModel);

        for (int x = 0; x < worldMap.getWidth(); x++) {
            for (int y = 0; y < worldMap.getHeight(); y++) {
                Tile tile = worldMap.getTile(x, y);
                if (tile.getTerrain() == TerrainType.FOREST) {
                    addForestTrees(tile, trunkModel, crownModel);
                }
            }
        }
    }

    private void addForestTrees(
            Tile tile, Model trunkModel, Model crownModel) {

        long seed = 12345L
                ^ ((long) tile.getX() * 73856093L)
                ^ ((long) tile.getY() * 19349663L);
        Random random = new Random(seed);

        float x = getWorldX(tile.getX());
        float z = getWorldZ(tile.getY());
        float groundHeight = TerrainMeshBuilder.surfaceHeight(tile);

        float[][] offsets = {
                {-0.27f, -0.25f},
                {0.25f, -0.14f},
                {0.03f, 0.24f}
        };

        for (float[] offset : offsets) {
            float px = x + offset[0]
                    + (random.nextFloat() - 0.5f) * 0.12f;
            float pz = z + offset[1]
                    + (random.nextFloat() - 0.5f) * 0.12f;
            float scale = 0.75f + random.nextFloat() * 0.30f;

            ModelInstance trunk = new ModelInstance(trunkModel);
            trunk.transform.setToTranslation(
                    px, groundHeight + 0.19f * scale, pz
            );
            trunk.transform.scale(scale, scale, scale);
            tiles.add(trunk);

            ModelInstance crown = new ModelInstance(crownModel);
            crown.transform.setToTranslation(
                    px, groundHeight + 0.60f * scale, pz
            );
            crown.transform.scale(scale, scale, scale);
            tiles.add(crown);
        }
    }

    private void createSelectionModels() {
        ModelBuilder builder = new ModelBuilder();
        selectionEdgeModel = builder.createCylinder(
                1f, 1f, 1f, 8,
                new Material(ColorAttribute.createDiffuse(
                        new Color(1f, 0.75f, 0.1f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        models.add(selectionEdgeModel);
    }

    private void createUI() {
        uiStage = new Stage(new ScreenViewport());
        font = new BitmapFont();
        font.getData().setScale(1.1f);

        tileInfoLabel = new Label(
                "Select a tile to view information",
                new Label.LabelStyle(font, Color.WHITE)
        );

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.06f, 0.09f, 0.15f, 0.90f);
        pixmap.fill();
        panelTexture = new Texture(pixmap);
        pixmap.dispose();

        Table panel = new Table();
        panel.setBackground(new TextureRegionDrawable(
                new TextureRegion(panelTexture)));
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
                    int screenX, int screenY, int pointer, int button) {

                if (button != Input.Buttons.LEFT) {
                    return false;
                }

                selectTile(screenX, screenY);

                boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                        || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

                if (shift) {
                    selectRiverRoutePoint();
                } else {
                    selectTransportUnitAtSelectedTile();
                }
                return true;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.T) {
                    advanceGameTurn();
                    return true;
                }
                return false;
            }
        };

        Gdx.input.setInputProcessor(new InputMultiplexer(
                tileInput, cameraController
        ));
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
                Vector3[] vertices = TerrainMeshBuilder.getTileVertices(
                        worldMap, tile
                );

                for (int[] triangle : triangles) {
                    if (Intersector.intersectRayTriangle(
                            ray,
                            vertices[triangle[0]],
                            vertices[triangle[1]],
                            vertices[triangle[2]],
                            intersection)) {

                        float distance = ray.origin.dst2(intersection);
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




    private void selectRiverRoutePoint() {

        if (selectedTile == null) {
            return;
        }

        int x = selectedTile.getX();
        int y = selectedTile.getY();

        // Allow selecting river and lake tiles.
        boolean isRiver = riverNetwork.hasRiverAt(x, y);

        boolean isLake =
                worldMap.getTile(x, y).getTerrain()
                        == TerrainType.LAKE;

        if (!isRiver && !isLake) {

            riverRouteStatus =
                    "Selected tile is not a waterway";

            updateInfoPanel();
            return;
        }

        // First Shift + click: choose starting tile.
        if (riverRouteStart == null) {

            riverRouteStart = selectedTile;

            riverRouteRenderer.showStart(x, y);

            riverRouteStatus =
                    "Start: " + x + ", " + y
                            + "\nShift + click destination";

            updateInfoPanel();
            return;
        }

        int startX = riverRouteStart.getX();
        int startY = riverRouteStart.getY();

        if (startX == x && startY == y) {

            riverRouteStatus =
                    "Choose a different destination tile";

            updateInfoPanel();
            return;
        }

        riverRouteStart = null;

        // Find fastest route across rivers and lakes.
        Optional<WaterwayFastestRouteFinder.WaterwayRoute> result =
                WaterwayFastestRouteFinder.findFastestRoute(
                        worldMap,
                        riverNetwork,
                        RiverVesselType.SMALL_BOAT,
                        startX,
                        startY,
                        x,
                        y
                );

        if (result.isEmpty()
                || result.get().getSegmentCount() == 0) {

            riverRouteRenderer.showEndpoints(
                    startX,
                    startY,
                    x,
                    y
            );

            riverRouteStatus =
                    "No navigable waterway route for SMALL_BOAT";

            System.out.println(
                    "Waterway route unavailable"
            );

            updateInfoPanel();
            return;
        }

        WaterwayFastestRouteFinder.WaterwayRoute route =
                result.get();

        long unitId = nextRiverUnitId++;

        // Create a new vessel.
        RiverTransportUnit unit =
                new RiverTransportUnit(
                        unitId,
                        RiverVesselType.SMALL_BOAT,
                        startX,
                        startY
                );

        // Assign the actual waterway route to the vessel.
        unit.assignRoute(route);

        // Temporary warehouses used before the city system.
        ResourceInventory source =
                new ResourceInventory(100);

        ResourceInventory destination =
                new ResourceInventory(100);

        source.add(ResourceType.WOOD, 25);
        source.add(ResourceType.FOOD, 15);

        // Load cargo onto the ship.
        unit.loadFrom(
                source,
                ResourceType.WOOD,
                6
        );

        unit.loadFrom(
                source,
                ResourceType.FOOD,
                4
        );

        // Register the transport unit in GameEngine.
        gameState.getRiverTransportManager()
                .registerUnit(unit);

        // Store destination inventory for unloading.
        demoDestinationsByUnit.put(
                unitId,
                destination
        );

        selectedTransportUnit = unit;

        /*
         * Temporary adapter for existing renderers.
         *
         * Renderers currently accept RiverRoute,
         * while navigation now uses WaterwayRoute.
         *
         * Lake segments are included in the first
         * legacy counter only for compatibility.
         * Actual movement and route statistics use
         * WaterwayRoute, not this adapter.
         */

        // Keep the route for selecting the ship later.

        riverRoutesByUnit.put(
                unitId,
                route
        );

        riverTransportRenderer.showUnit(
                unit,
                route
        );

        riverRouteRenderer.showRoute(
                route
        );


        // Update information panel.
        riverRouteStatus =
                "Created waterway unit #" + unitId
                        + "\nSegments: "
                        + route.getSegmentCount()
                        + "\nMovement cost: "
                        + route.movementCost()
                        + "\nDownstream: "
                        + route.downstreamSegments()
                        + "\nUpstream: "
                        + route.upstreamSegments()
                        + "\nLake: "
                        + route.lakeSegments()
                        + "\nCargo: "
                        + unit.getCargoContents();

        // Debug information.
        System.out.println(
                "=== WATERWAY TRANSPORT CREATED ==="
        );

        System.out.println(
                "Unit ID: " + unitId
        );

        System.out.println(
                "Movement cost: "
                        + route.movementCost()
        );

        System.out.println(
                "Lake segments: "
                        + route.lakeSegments()
        );

        System.out.println(
                "Route: " + route.points()
        );

        System.out.println(
                "SOURCE WAREHOUSE: "
                        + source.getContents()
        );

        System.out.println(
                "SHIP CARGO: "
                        + unit.getCargoContents()
        );

        updateInfoPanel();
    }




    private void selectTransportUnitAtSelectedTile() {
        if (selectedTile == null) {
            return;
        }

        RiverTransportManager manager =
                gameState.getRiverTransportManager();

        for (RiverTransportUnit unit : manager.getUnits()) {
            if (unit.getX() == selectedTile.getX()
                    && unit.getY() == selectedTile.getY()) {
                selectedTransportUnit = unit;
                WaterwayFastestRouteFinder.WaterwayRoute route =
                        riverRoutesByUnit.get(unit.getId());

                if (route != null) {
                    riverRouteRenderer.showRoute(route);
                }

                riverRouteStatus = "SELECTED TRANSPORT UNIT"
                        + "\nID: #" + unit.getId()
                        + "\nType: " + unit.getVesselType()
                        + "\nPosition: " + unit.getX()
                        + ", " + unit.getY()
                        + "\nCargo: " + unit.getCargoContents()
                        + "\nRemaining segments: "
                        + unit.getRemainingSegments();
                updateInfoPanel();
                return;
            }
        }
    }

    private void advanceGameTurn() {
        GameEngine.TurnReport report = gameEngine.endTurn();
        RiverTransportManager manager =
                gameState.getRiverTransportManager();

        riverTransportRenderer.updateUnits(manager);
        int movedUnits = 0;

        for (Map.Entry<Long, RiverTransportService.MovementResult> entry
                : report.movements().entrySet()) {

            long unitId = entry.getKey();
            RiverTransportService.MovementResult movement = entry.getValue();

            if (movement.traveledSegments() > 0) {
                movedUnits++;
            }

            if (movement.arrived()) {
                RiverTransportUnit unit = manager.findUnit(unitId)
                        .orElseThrow();
                ResourceInventory destination =
                        demoDestinationsByUnit.get(unitId);

                if (destination != null) {
                    // Copy to avoid modifying an inventory while iterating.
                    for (Map.Entry<ResourceType, Integer> cargoEntry
                            : unit.getCargoContents().entrySet()) {
                        unit.unloadInto(
                                destination,
                                cargoEntry.getKey(),
                                cargoEntry.getValue()
                        );
                    }
                    System.out.println("TRANSPORT COMPLETED: unit #" + unitId);
                    System.out.println("DESTINATION WAREHOUSE: "
                            + destination.getContents());
                    System.out.println("SHIP CARGO AFTER UNLOADING: "
                            + unit.getCargoContents());
                }
            }
        }

        System.out.println("Turn " + report.completedTurn()
                + " completed | Units: " + manager.getUnitCount()
                + " | Moved: " + movedUnits);

        if (selectedTransportUnit != null) {
            RiverTransportService.MovementResult movement =
                    report.movements().get(selectedTransportUnit.getId());

            if (movement != null) {
                riverRouteStatus = "Turn: " + report.completedTurn()
                        + "\nSelected unit: #"
                        + selectedTransportUnit.getId()
                        + "\nType: " + selectedTransportUnit.getVesselType()
                        + "\nPosition: " + movement.currentX()
                        + ", " + movement.currentY()
                        + "\nMoved: " + movement.traveledSegments()
                        + "\nRemaining: " + movement.remainingSegments()
                        + "\nArrived: " + (movement.arrived() ? "Yes" : "No")
                        + "\nBlocked: " + (movement.blocked() ? "Yes" : "No")
                        + "\nCargo: " + selectedTransportUnit.getCargoContents();
            }
        } else {
            riverRouteStatus = "Turn: " + report.completedTurn()
                    + "\nUnits: " + manager.getUnitCount()
                    + "\nMoved: " + movedUnits;
        }

        updateInfoPanel();
    }

    private void updateSelectionOutline() {
        selectionOutline.clear();
        if (selectedTile == null) {
            return;
        }

        Vector3[] vertices = TerrainMeshBuilder.getTileVertices(
                worldMap, selectedTile
        );
        for (int i = 0; i < 4; i++) {
            Vector3 start = new Vector3(vertices[i]).add(0f, 0.035f, 0f);
            Vector3 end = new Vector3(vertices[(i + 1) % 4])
                    .add(0f, 0.035f, 0f);
            addOutlineEdge(start, end);
        }
    }

    private void addOutlineEdge(Vector3 start, Vector3 end) {
        Vector3 direction = new Vector3(end).sub(start);
        float length = direction.len();
        if (length < 0.0001f) {
            return;
        }
        direction.nor();

        Vector3 midpoint = new Vector3(start).add(end).scl(0.5f);
        Quaternion rotation = new Quaternion()
                .setFromCross(Vector3.Y, direction);
        ModelInstance instance = new ModelInstance(selectionEdgeModel);
        instance.transform.set(
                midpoint,
                rotation,
                new Vector3(0.03f, length, 0.03f)
        );
        selectionOutline.add(instance);
    }

    private void updateInfoPanel() {
        StringBuilder info = new StringBuilder();

        if (selectedTile == null) {
            info.append("Select a tile to view information");
            Gdx.graphics.setTitle("Epoch Strategy");
        } else {
            info.append("TILE INFORMATION\n\n")
                    .append("Coordinates: ")
                    .append(selectedTile.getX()).append(", ")
                    .append(selectedTile.getY()).append('\n')
                    .append("Terrain: ").append(selectedTile.getTerrain())
                    .append("\nElevation: ").append(selectedTile.getElevation())
                    .append("\nLand passable: ")
                    .append(selectedTile.isPassableForLandUnits()
                            ? "Yes" : "No");

            int x = selectedTile.getX();
            int y = selectedTile.getY();
            Optional<RiverProperties> riverOptional =
                    riverNetwork.findPropertiesAt(x, y);

            if (riverOptional.isPresent()) {
                RiverProperties river = riverOptional.get();

                if (river != null) {
                    info.append("\n\nRIVER")
                            .append("\nType: ").append(river.type())
                            .append("\nFlow: ").append(river.flow())
                            .append("\nWidth: ").append(river.width())
                            .append("\nDepth: ").append(river.depth())
                            .append("\nCargo barge navigable: ")
                            .append(river.navigable() ? "Yes" : "No");
                }

                if (drainageMap.hasDrainagePath(x, y)) {
                    info.append("\nCatchment area: ")
                            .append(drainageMap.getCatchmentArea(x, y));
                }

                Gdx.graphics.setTitle(
                        "Epoch Strategy | " + selectedTile.getTerrain()
                );
            }
        }

        info.append("\n\nTURN: ").append(gameState.getTurnNumber());
        info.append("\nUNITS: ")
                .append(gameState.getRiverTransportManager().getUnitCount());
        info.append("\n\nRIVER ROUTING\nVessel: SMALL_BOAT\n")
                .append(riverRouteStatus);

        tileInfoLabel.setText(info.toString());
    }

    private float getWorldX(int tileX) {
        return tileX - (worldMap.getWidth() - 1) / 2f;
    }

    private float getWorldZ(int tileY) {
        return tileY - (worldMap.getHeight() - 1) / 2f;
    }

    @Override
    public void render() {
        cameraController.update();
        Gdx.gl.glClearColor(0.10f, 0.14f, 0.20f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        camera.update();

        modelBatch.begin(camera);
        terrainChunkManager.render(camera, modelBatch, environment);
        riverRenderer.render(modelBatch, environment);
        modelBatch.render(tiles, environment);
        riverRouteRenderer.render(modelBatch, environment);
        riverTransportRenderer.render(modelBatch, environment);
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
        if (riverTransportRenderer != null) {
            riverTransportRenderer.dispose();
        }
        if (riverRouteRenderer != null) {
            riverRouteRenderer.dispose();
        }
        if (riverRenderer != null) {
            riverRenderer.dispose();
        }
        if (terrainChunkManager != null) {
            terrainChunkManager.dispose();
        }
        for (Model model : models) {
            model.dispose();
        }
        if (modelBatch != null) {
            modelBatch.dispose();
        }
        if (uiStage != null) {
            uiStage.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (panelTexture != null) {
            panelTexture.dispose();
        }
    }
}
