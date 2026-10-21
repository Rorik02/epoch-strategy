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
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.epochstrategy.core.building.BuildingType;
import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityFoodService;
import com.epochstrategy.core.city.CityGenerator;
import com.epochstrategy.core.city.CityPlacementService;
import com.epochstrategy.core.city.CityPopulationService;
import com.epochstrategy.core.civilization.CivilizationDemoInitializer;
import com.epochstrategy.core.economy.ResourceInventory;
import com.epochstrategy.core.economy.ResourceType;
import com.epochstrategy.core.engine.GameEngine;
import com.epochstrategy.core.engine.GameState;
import com.epochstrategy.core.territory.TerritoryExpansionConfig;
import com.epochstrategy.core.territory.TerritoryExpansionService;
import com.epochstrategy.core.territory.TerritoryGenerator;
import com.epochstrategy.core.world.DrainageMap;
import com.epochstrategy.core.world.LakeGenerator;
import com.epochstrategy.core.world.MapType;
import com.epochstrategy.core.world.ProceduralMapGenerator;
import com.epochstrategy.core.world.RiverGenerator;
import com.epochstrategy.core.world.RiverNavigationService;
import com.epochstrategy.core.world.RiverNetwork;
import com.epochstrategy.core.world.RiverProperties;
import com.epochstrategy.core.world.RiverTransportManager;
import com.epochstrategy.core.world.RiverTransportService;
import com.epochstrategy.core.world.RiverTransportUnit;
import com.epochstrategy.core.world.RiverVesselType;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WatershedCalculator;
import com.epochstrategy.core.world.WaterwayFastestRouteFinder;
import com.epochstrategy.core.world.WorldMap;
import com.epochstrategy.desktop.renderer.CityBuildingRenderer;
import com.epochstrategy.desktop.renderer.CityRenderer;
import com.epochstrategy.desktop.renderer.RiverRenderer;
import com.epochstrategy.desktop.renderer.RiverRouteRenderer;
import com.epochstrategy.desktop.renderer.RiverTransportFleetRenderer;
import com.epochstrategy.desktop.renderer.TerrainChunkManager;
import com.epochstrategy.desktop.renderer.TerrainMeshBuilder;
import com.epochstrategy.desktop.renderer.TerritoryBorderRenderer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/** Desktop UI and rendering. All authoritative turn and cargo state live in game-core. */
public class EpochGame extends ApplicationAdapter {

    // -------------------------------------------------------------------------
    // World generation configuration
    // -------------------------------------------------------------------------

    private static final int MAP_WIDTH = 100;
    private static final int MAP_HEIGHT = 100;
    private static final int CHUNK_SIZE = 16;
    private static final long MAP_SEED = 1927L;
    private static final long LAKE_SEED = 98765L;
    private static final long RIVER_SEED = 12345L;
    private static final MapType MAP_TYPE = MapType.PANGAEA;

    // World and game state

    private WorldMap worldMap;
    private DrainageMap drainageMap;
    private RiverNetwork riverNetwork;
    private GameState gameState;
    private GameEngine gameEngine;

    // Camera and rendering

    private PerspectiveCamera camera;
    private CameraInputController cameraController;
    private ModelBatch modelBatch;
    private Environment environment;
    private TerrainChunkManager terrainChunkManager;
    private RiverRenderer riverRenderer;
    private RiverRouteRenderer riverRouteRenderer;
    private RiverTransportFleetRenderer riverTransportRenderer;
    private CityRenderer cityRenderer;
    private CityBuildingRenderer cityBuildingRenderer;
    private TerritoryBorderRenderer territoryBorderRenderer;

    private final Array<Model> models = new Array<>();
    private final Array<ModelInstance> tiles = new Array<>();
    private final Array<ModelInstance> selectionOutline = new Array<>();
    private Model selectionEdgeModel;
    private Tile selectedTile;

    // River transport and routing

    private Tile riverRouteStart;
    private long nextRiverUnitId = 1L;
    private RiverTransportUnit selectedTransportUnit;

    private final Map<Long, WaterwayFastestRouteFinder.WaterwayRoute>
            riverRoutesByUnit = new HashMap<>();

    private final Map<Long, ResourceInventory> demoDestinationsByUnit =
            new HashMap<>();

    private String riverRouteStatus =
            "Shift + Left Click: select river route start";

    // UI: information and city construction

    private Stage uiStage;
    private BitmapFont font;
    private Texture panelTexture;
    private Texture buttonTexture;
    private Texture buttonHoverTexture;
    private Label tileInfoLabel;
    private Table buildingPanel;
    private Label buildingSummaryLabel;
    private Label buildingStatusLabel;

    private final EnumMap<BuildingType, TextButton> buildingButtons =
            new EnumMap<>(BuildingType.class);

    // UI: territorial expansion

    private final TerritoryExpansionService expansionService =
            new TerritoryExpansionService();

    private final Array<ModelInstance> expansionOutlines = new Array<>();
    private Long expansionCityId = null;
    private TextButton expandTerritoryButton;
    private TextButton autoExpansionButton;
    private Label expansionStatusLabel;

    // =========================================================================
    // Application lifecycle: initialization
    // =========================================================================

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

        CityPlacementService cityPlacementService =
                new CityPlacementService(6);

        CityGenerator cityGenerator =
                new CityGenerator(cityPlacementService);

        var generatedCities = cityGenerator.generate(
                worldMap,
                riverNetwork,
                gameState.getCityManager(),
                12,
                9876L
        );

        // Temporary starting resources for construction tests.
        for (City city : gameState.getCityManager().getCities()) {

            city.getInventory().add(
                    ResourceType.WOOD,
                    40
            );

            city.getInventory().add(
                    ResourceType.STONE,
                    30
            );
        }

        CivilizationDemoInitializer.initialize(
                gameState.getCivilizationManager(),
                gameState.getCityManager()
        );

        TerritoryGenerator territoryGenerator =
                new TerritoryGenerator(
                        TerritoryExpansionConfig.INITIAL_RADIUS,
                        true
                );

        int generatedTerritoryTiles =
                territoryGenerator.generate(
                        worldMap,
                        gameState.getCityManager(),
                        gameState.getCivilizationManager(),
                        gameState.getTerritoryManager()
                );

        territoryBorderRenderer =
                new TerritoryBorderRenderer(worldMap);

        territoryBorderRenderer.refresh(
                gameState.getTerritoryManager(),
                gameState.getCivilizationManager()
        );

        System.out.println(
                "=== TERRITORIES ==="
        );

        System.out.println(
                "Total controlled tiles: "
                        + generatedTerritoryTiles
        );

        for (var civilization :
                gameState.getCivilizationManager()
                        .getCivilizations()) {

            int controlledTiles =
                    gameState.getTerritoryManager()
                            .getClaimedTileCount(
                                    civilization.getId()
                            );

            System.out.println(
                    civilization.getName()
                            + " controls "
                            + controlledTiles
                            + " tiles"
            );
        }

        System.out.println("=== CIVILIZATIONS ===");

        for (var civilization :
                gameState.getCivilizationManager().getCivilizations()) {

            int cityCount =
                    gameState.getCityManager()
                            .getCitiesByCivilization(
                                    civilization.getId()
                            )
                            .size();

            System.out.println(
                    civilization.getName()
                            + " | Cities: " + cityCount
                            + " | Color: " + civilization.getHexColor()
            );
        }

        System.out.println(
                "Generated cities: " + generatedCities.size()
        );

        for (City city : generatedCities) {

            System.out.println(
                    city.getName()
                            + " | Position: "
                            + city.getX()
                            + ", "
                            + city.getY()
                            + " | Population: "
                            + city.getPopulation()
            );
        }

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

        cityRenderer = new CityRenderer(worldMap);

        cityRenderer.setCities(
                gameState.getCityManager().getCities(),
                gameState.getCivilizationManager()
        );

        cityBuildingRenderer =
                new CityBuildingRenderer(worldMap);

        cityBuildingRenderer.refresh(
                gameState.getCityManager().getCities(),
                gameState.getCityBuildingManager()
        );

        createWorldModels();
        createSelectionModels();
        createUI();
        setupInput();
        updateInfoPanel();
    }


    // =========================================================================
    // World generation and rendering setup
    // =========================================================================

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

    private float getWorldX(int tileX) {
        return tileX - (worldMap.getWidth() - 1) / 2f;
    }

    private float getWorldZ(int tileY) {
        return tileY - (worldMap.getHeight() - 1) / 2f;
    }


    // =========================================================================
    // UI initialization and layout
    // =========================================================================

    private void createUI() {

        uiStage = new Stage(new ScreenViewport());

        font = new BitmapFont();
        font.getData().setScale(1.1f);

        Label.LabelStyle labelStyle =
                new Label.LabelStyle(
                        font,
                        Color.WHITE
                );

        tileInfoLabel = new Label(
                "Select a tile to view information",
                labelStyle
        );

        // Background for information panels.
        Pixmap pixmap = new Pixmap(
                1, 1, Pixmap.Format.RGBA8888
        );

        pixmap.setColor(
                0.06f, 0.09f, 0.15f, 0.90f
        );
        pixmap.fill();

        panelTexture = new Texture(pixmap);
        pixmap.dispose();

        // Normal button background.
        pixmap = new Pixmap(
                1, 1, Pixmap.Format.RGBA8888
        );

        pixmap.setColor(
                0.16f, 0.25f, 0.35f, 1f
        );
        pixmap.fill();

        buttonTexture = new Texture(pixmap);
        pixmap.dispose();

        // Hover / pressed button background.
        pixmap = new Pixmap(
                1, 1, Pixmap.Format.RGBA8888
        );

        pixmap.setColor(
                0.25f, 0.39f, 0.50f, 1f
        );
        pixmap.fill();

        buttonHoverTexture = new Texture(pixmap);
        pixmap.dispose();

        var panelDrawable =
                new TextureRegionDrawable(
                        new TextureRegion(panelTexture)
                );

        var normalButtonDrawable =
                new TextureRegionDrawable(
                        new TextureRegion(buttonTexture)
                );

        var hoverButtonDrawable =
                new TextureRegionDrawable(
                        new TextureRegion(buttonHoverTexture)
                );

        TextButton.TextButtonStyle buttonStyle =
                new TextButton.TextButtonStyle();

        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.disabledFontColor = Color.GRAY;

        buttonStyle.up = normalButtonDrawable;
        buttonStyle.over = hoverButtonDrawable;
        buttonStyle.down = hoverButtonDrawable;

        // LEFT PANEL - tile and city information.
        Table infoPanel = new Table();

        infoPanel.setBackground(panelDrawable);
        infoPanel.pad(15);

        infoPanel.add(tileInfoLabel).left();

        // RIGHT PANEL - building construction.
        buildingPanel = new Table();

        buildingPanel.setBackground(panelDrawable);
        buildingPanel.pad(12);

        Label title = new Label(
                "CITY CONSTRUCTION",
                labelStyle
        );

        buildingPanel.add(title)
                .left()
                .padBottom(10)
                .row();

        buildingSummaryLabel = new Label(
                "",
                labelStyle
        );

        buildingPanel.add(buildingSummaryLabel)
                .left()
                .padBottom(12)
                .row();

        // Create one button for every building type.
        for (BuildingType type : BuildingType.values()) {

            TextButton button = new TextButton(
                    type.name(),
                    buttonStyle
            );

            button.pad(7);

            button.addListener(new ChangeListener() {

                @Override
                public void changed(
                        ChangeEvent event,
                        Actor actor
                ) {

                    if (button.isDisabled()) {
                        return;
                    }

                    constructSelectedBuilding(type);
                }
            });

            buildingButtons.put(type, button);

            buildingPanel.add(button)
                    .width(280)
                    .height(38)
                    .padBottom(5)
                    .row();
        }

        expandTerritoryButton = new TextButton(
                "EXPAND TERRITORY",
                buildingButtons.get(BuildingType.FARM).getStyle()
        );

        expandTerritoryButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        toggleExpansionMode();
                    }
                }
        );

        buildingPanel.add(expandTerritoryButton)
                .fillX()
                .padTop(12)
                .row();

        autoExpansionButton = new TextButton(
                "AUTOMATE: OFF",
                buildingButtons.get(
                        BuildingType.FARM
                ).getStyle()
        );

        autoExpansionButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        toggleAutomaticExpansion();
                    }
                }
        );

        buildingPanel.add(autoExpansionButton)
                .fillX()
                .padTop(6)
                .row();

        expansionStatusLabel = new Label(
                "",
                new Label.LabelStyle(
                        font,
                        Color.WHITE
                )
        );

        buildingPanel.add(expansionStatusLabel)
                .left()
                .padTop(6)
                .row();

        buildingStatusLabel = new Label(
                "Select a building",
                labelStyle
        );

        buildingStatusLabel.setWrap(true);

        buildingPanel.add(buildingStatusLabel)
                .width(280)
                .left()
                .padTop(10)
                .row();

        buildingPanel.setVisible(false);

        // Main UI layout.
        Table root = new Table();

        root.setFillParent(true);
        root.top().left();
        root.pad(20);

        // Important: allow child buttons to receive input.
        root.setTouchable(Touchable.childrenOnly);

        root.add(infoPanel)
                .top()
                .left();

        uiStage.addActor(root);

        // Building panel is positioned independently.
        uiStage.addActor(buildingPanel);

        positionBuildingPanel();

    }

    private void positionBuildingPanel() {

        if (buildingPanel == null || uiStage == null) {
            return;
        }

        buildingPanel.pack();

        float width =
                uiStage.getViewport().getWorldWidth();

        float height =
                uiStage.getViewport().getWorldHeight();

        float x = Math.max(
                0f,
                width - buildingPanel.getWidth() - 20f
        );

        float y = Math.max(
                0f,
                height - buildingPanel.getHeight() - 20f
        );

        buildingPanel.setPosition(x, y);
    }


    // =========================================================================
    // UI information and city economy
    // =========================================================================

    private void updateInfoPanel() {

        StringBuilder info = new StringBuilder();

        if (selectedTile == null) {

            info.append("Select a tile to view information");

            Gdx.graphics.setTitle("Epoch Strategy");

        } else {

            info.append("TILE INFORMATION\n\n")
                    .append("Coordinates: ")
                    .append(selectedTile.getX())
                    .append(", ")
                    .append(selectedTile.getY())
                    .append('\n')
                    .append("Terrain: ")
                    .append(selectedTile.getTerrain())
                    .append("\nElevation: ")
                    .append(selectedTile.getElevation())
                    .append("\nLand passable: ")
                    .append(
                            selectedTile.isPassableForLandUnits()
                                    ? "Yes"
                                    : "No"
                    );

            int x = selectedTile.getX();
            int y = selectedTile.getY();
            appendTerritoryInfo(info, x, y);

            Optional<RiverProperties> riverOptional =
                    riverNetwork.findPropertiesAt(x, y);

            if (riverOptional.isPresent()) {

                RiverProperties river = riverOptional.get();

                info.append("\n\nRIVER")
                        .append("\nType: ").append(river.type())
                        .append("\nFlow: ").append(river.flow())
                        .append("\nWidth: ").append(river.width())
                        .append("\nDepth: ").append(river.depth())
                        .append("\nCargo barge navigable: ")
                        .append(river.navigable() ? "Yes" : "No");

                if (drainageMap.hasDrainagePath(x, y)) {

                    info.append("\nCatchment area: ")
                            .append(
                                    drainageMap.getCatchmentArea(x, y)
                            );
                }
            }

// CITY INFORMATION
            Optional<City> cityOptional =
                    gameState.getCityManager().findCityAt(x, y);

            if (cityOptional.isPresent()) {

                City city = cityOptional.get();

                String civilizationName = city.isNeutral()
                        ? "Neutral"
                        : gameState.getCivilizationManager()
                        .findCivilization(
                                city.getCivilizationId()
                        )
                        .map(civilization ->
                                civilization.getName()
                        )
                        .orElse("Unknown");

                info.append("\n\nCITY INFORMATION")
                        .append("\nName: ")
                        .append(city.getName())
                        .append("\nID: ")
                        .append(city.getId())
                        .append("\nCivilization: ")
                        .append(civilizationName)
                        .append("\nLevel: ")
                        .append(city.getLevel())
                        .append("\nPopulation: ")
                        .append(city.getPopulation())
                        .append("/")
                        .append(

                                CityPopulationService.getPopulationCapacity(
                                        city,
                                        gameState.getCityBuildingManager().hasBuilding(
                                                city.getId(),
                                                BuildingType.HOUSING
                                        )
                                                ? BuildingType.HOUSING
                                                .getPopulationCapacityBonus()
                                                : 0
                                )

                        );

                // Current food storage
                int storedFood = city.getInventory()
                        .getAmount(ResourceType.FOOD);

                info.append("\n\nFOOD ECONOMY")
                        .append("\nStored food: ")
                        .append(storedFood)
                        .append("\nStorage: ")
                        .append(
                                city.getInventory().getTotalUnits()
                        )
                        .append("/")
                        .append(
                                city.getInventory().getCapacity()
                        );

                // Results from the last completed turn
                CityFoodService.FoodTurnResult lastResult =
                        gameState.getLastCityFoodResults()
                                .get(city.getId());

                if (lastResult != null) {

                    info.append("\n\nLAST TURN")
                            .append("\nProduced: +")
                            .append(lastResult.producedFood())
                            .append("\nConsumed: -")
                            .append(lastResult.consumedFood())
                            .append("\nRequired: ")
                            .append(lastResult.requiredFood())
                            .append("\nMissing food: ")
                            .append(lastResult.missingFood())
                            .append("\nPopulation change: ")
                            .append(
                                    lastResult.populationChange() >= 0
                                            ? "+"
                                            : ""
                            )
                            .append(lastResult.populationChange())
                            .append("\nStatus: ")
                            .append(
                                    lastResult.starving()
                                            ? "STARVING"
                                            : "FED"
                            );

                } else {

                    info.append("\n\nLAST TURN")
                            .append("\nNo economic data yet");
                }

// RESOURCE ECONOMY
                info.append("\n\nRESOURCE ECONOMY")
                        .append("\nWood: ")
                        .append(
                                city.getInventory()
                                        .getAmount(ResourceType.WOOD)
                        )
                        .append("\nStone: ")
                        .append(
                                city.getInventory()
                                        .getAmount(ResourceType.STONE)
                        )
                        .append("\nIron: ")
                        .append(
                                city.getInventory()
                                        .getAmount(ResourceType.IRON)
                        );

                var resourceResult =
                        gameState.getLastCityResourceResults()
                                .get(city.getId());

                if (resourceResult != null) {

                    info.append("\n\nLAST TURN RESOURCES")
                            .append("\nWood produced: +")
                            .append(resourceResult.woodProduced())
                            .append("\nStone produced: +")
                            .append(resourceResult.stoneProduced())
                            .append("\nIron produced: +")
                            .append(resourceResult.ironProduced())
                            .append("\nTotal produced: +")
                            .append(resourceResult.totalProduced());

                } else {

                    info.append("\n\nLAST TURN RESOURCES")
                            .append("\nNo production data yet");
                }

            }

            Gdx.graphics.setTitle(
                    "Epoch Strategy | "
                            + selectedTile.getTerrain()
                            + (cityOptional.isPresent()
                            ? " | " + cityOptional.get().getName()
                            : "")
            );
        }

        info.append("\n\nTURN: ")
                .append(gameState.getTurnNumber());

        info.append("\nUNITS: ")
                .append(
                        gameState.getRiverTransportManager()
                                .getUnitCount()
                );

        info.append("\n\nRIVER ROUTING")
                .append("\nVessel: SMALL_BOAT")
                .append("\n")
                .append(riverRouteStatus);

        tileInfoLabel.setText(info.toString());

        updateBuildingPanel();
    }

    private void appendTerritoryInfo(
            StringBuilder info,
            int x,
            int y
    ) {

        info.append("\n\nTERRITORY");

        var claimOptional =
                gameState.getTerritoryManager()
                        .findClaim(x, y);

        if (claimOptional.isEmpty()) {

            info.append("\nOwner: Unclaimed");
            return;
        }

        var claim = claimOptional.get();

        String civilizationName =
                gameState.getCivilizationManager()
                        .findCivilization(
                                claim.civilizationId()
                        )
                        .map(civilization ->
                                civilization.getName()
                        )
                        .orElse("Unknown");

        String cityName =
                gameState.getCityManager()
                        .findCity(
                                claim.cityId()
                        )
                        .map(city -> city.getName())
                        .orElse("Unknown");

        info.append("\nOwner: ")
                .append(civilizationName)
                .append("\nCity: ")
                .append(cityName)
                .append('\n');
    }

    private void updateBuildingPanel() {

        if (buildingPanel == null) {
            return;
        }

        if (selectedTile == null) {

            buildingPanel.setVisible(false);
            return;
        }

        var cityOptional =
                gameState.getCityManager().findCityAt(
                        selectedTile.getX(),
                        selectedTile.getY()
                );

        if (cityOptional.isEmpty()) {

            buildingPanel.setVisible(false);
            return;
        }

        City city = cityOptional.get();

        buildingPanel.setVisible(true);

        var inventory = city.getInventory();

        int wood = inventory.getAmount(
                ResourceType.WOOD
        );

        int stone = inventory.getAmount(
                ResourceType.STONE
        );

        buildingSummaryLabel.setText(
                city.getName()
                        + "\nWood: " + wood
                        + " | Stone: " + stone
                        + "\nStorage: "
                        + inventory.getTotalUnits()
                        + "/"
                        + inventory.getCapacity()
        );

        var buildingManager =
                gameState.getCityBuildingManager();

        for (BuildingType type : BuildingType.values()) {

            TextButton button =
                    buildingButtons.get(type);

            if (button == null) {
                continue;
            }

            boolean alreadyBuilt =
                    buildingManager.hasBuilding(
                            city.getId(),
                            type
                    );

            boolean canBuild =
                    buildingManager.canConstruct(
                            city.getId(),
                            type,
                            gameState.getCityManager()
                    );

            String buttonText =
                    type.name()
                            + " | W:"
                            + type.getWoodCost()
                            + " S:"
                            + type.getStoneCost();

            if (alreadyBuilt) {
                buttonText += " [BUILT]";
            }

            button.setText(buttonText);

            button.setDisabled(
                    alreadyBuilt || !canBuild
            );
        }

        updateExpansionControls(city);

        buildingStatusLabel.setText(
                "Choose an available building"
        );

        positionBuildingPanel();
    }

    private void constructSelectedBuilding(
            BuildingType buildingType
    ) {

        if (selectedTile == null) {
            return;
        }

        var cityOptional =
                gameState.getCityManager().findCityAt(
                        selectedTile.getX(),
                        selectedTile.getY()
                );

        if (cityOptional.isEmpty()) {
            return;
        }

        City city = cityOptional.get();

        var buildingManager =
                gameState.getCityBuildingManager();

        if (!buildingManager.canConstruct(
                city.getId(),
                buildingType,
                gameState.getCityManager()
        )) {

            updateBuildingPanel();

            buildingStatusLabel.setText(
                    "Not enough resources or already built"
            );

            return;
        }

        try {

            buildingManager.construct(
                    city.getId(),
                    buildingType,
                    gameState.getCityManager()
            );

            cityBuildingRenderer.refresh(
                    gameState.getCityManager().getCities(),
                    gameState.getCityBuildingManager()
            );

            // Refresh all city information.
            updateInfoPanel();

            buildingStatusLabel.setText(
                    "Built: " + buildingType.name()
            );

            System.out.println(
                    "Constructed " + buildingType
                            + " in " + city.getName()
            );

        } catch (IllegalArgumentException
                 | IllegalStateException exception) {

            updateBuildingPanel();

            buildingStatusLabel.setText(
                    "Construction failed: "
                            + exception.getMessage()
            );
        }
    }


    // =========================================================================
    // City territorial expansion
    // =========================================================================

    private long getCityTerritorySize(long cityId) {

        return gameState.getTerritoryManager()
                .getClaims()
                .stream()
                .filter(claim -> claim.cityId() == cityId)
                .count();
    }

    private void updateExpansionControls(
            City city
    ) {

        if (expandTerritoryButton == null
                || autoExpansionButton == null
                || expansionStatusLabel == null) {
            return;
        }

        var influence =
                gameState.getCityInfluenceManager();

        var expansionManager =
                gameState.getCityExpansionManager();

        long cityId = city.getId();

        double balance = influence.getBalance(cityId);
        double cost = influence.getNextExpansionCost(cityId);
        double income = influence.getIncome(city);

        boolean automatic =
                expansionManager.isAutomatic(cityId);

        boolean active =
                expansionCityId != null
                        && expansionCityId == cityId;

        autoExpansionButton.setText(
                automatic
                        ? "AUTOMATE: ON"
                        : "AUTOMATE: OFF"
        );

        autoExpansionButton.setDisabled(
                city.getCivilizationId() == null
        );

        if (active) {

            expandTerritoryButton.setText(
                    "CANCEL EXPANSION"
            );

            expandTerritoryButton.setDisabled(false);

            expansionStatusLabel.setText(
                    "Click a GREEN tile\nESC to cancel"
            );

            return;
        }

        boolean hasCandidates =
                !expansionService.getEligibleTiles(
                        city,
                        worldMap,
                        gameState.getCityManager(),
                        gameState.getTerritoryManager()
                ).isEmpty();

        expandTerritoryButton.setText(
                "EXPAND TERRITORY"
        );

        // Manual expansion is unavailable
        // while automation is enabled.
        expandTerritoryButton.setDisabled(
                automatic
                        || !influence.hasEnoughInfluence(cityId)
                        || !hasCandidates
        );

        long territorySize = getCityTerritorySize(cityId);

        int maxTerritorySize =
                (TerritoryExpansionConfig.MAX_RADIUS * 2 + 1)
                        * (TerritoryExpansionConfig.MAX_RADIUS * 2 + 1);

        int progress = cost <= 0
                ? 100
                : (int) Math.min(
                100,
                (balance / cost) * 100
        );

        String turnsRemaining;

        if (!hasCandidates) {

            turnsRemaining = "No available tiles";

        } else if (balance >= cost) {

            turnsRemaining = automatic
                    ? "Expansion on next turn"
                    : "Expansion available!";

        } else if (income > 0) {

            long turns = (long) Math.ceil(
                    (cost - balance) / income
            );

            turnsRemaining = "Estimated: "
                    + turns
                    + " turns";

        } else {

            turnsRemaining = "No Influence production";
        }

        String status = String.format(
                Locale.US,
                "TERRITORIAL DEVELOPMENT\n"
                        + "------------------------\n"
                        + "Influence: %.1f / %.0f\n"
                        + "Income: +%.1f / turn\n"
                        + "Progress: %d%%\n"
                        + "Controlled tiles: %d / %d\n"
                        + "Next expansion: #%d\n"
                        + "Mode: %s\n"
                        + "%s",
                balance,
                cost,
                income,
                progress,
                territorySize,
                maxTerritorySize,
                influence.getPurchasedExpansions(cityId) + 1,
                automatic ? "AUTOMATIC" : "MANUAL",
                turnsRemaining
        );

        expansionStatusLabel.setText(status);
    }

    private void toggleExpansionMode() {

        if (expansionCityId != null) {
            cancelExpansionMode();
            return;
        }

        if (selectedTile == null) {
            return;
        }

        var cityOptional =
                gameState.getCityManager().findCityAt(
                        selectedTile.getX(),
                        selectedTile.getY()
                );

        if (cityOptional.isEmpty()) {
            return;
        }

        City city = cityOptional.get();

        if (!gameState.getCityInfluenceManager()
                .hasEnoughInfluence(city.getId())) {
            return;
        }

        if (gameState.getCityExpansionManager()
                .isAutomatic(city.getId())) {
            return;
        }

        var eligibleTiles =
                expansionService.getEligibleTiles(
                        city,
                        worldMap,
                        gameState.getCityManager(),
                        gameState.getTerritoryManager()
                );

        if (eligibleTiles.isEmpty()) {
            return;
        }

        expansionCityId = city.getId();

        updateExpansionOutlines();
        updateInfoPanel();
    }

    private void cancelExpansionMode() {

        expansionCityId = null;
        expansionOutlines.clear();

        updateInfoPanel();
    }

    private void handleExpansionClick(
            int screenX,
            int screenY
    ) {

        if (expansionCityId == null) {
            return;
        }

        Long cityId = expansionCityId;

        City city = gameState.getCityManager()
                .findCity(cityId)
                .orElse(null);

        if (city == null) {
            cancelExpansionMode();
            return;
        }

        // Keep the city selected while picking a target.
        Tile previousSelection = selectedTile;

        selectTile(screenX, screenY);

        Tile targetTile = selectedTile;

        selectedTile = previousSelection;
        updateSelectionOutline();

        if (targetTile == null) {
            updateInfoPanel();
            return;
        }

        try {

            expansionService.expand(
                    city,
                    targetTile.getX(),
                    targetTile.getY(),
                    worldMap,
                    gameState.getCityManager(),
                    gameState.getTerritoryManager(),
                    gameState.getCityInfluenceManager()
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            Gdx.app.log(
                    "TERRITORY",
                    exception.getMessage()
            );

            updateInfoPanel();
            return;
        }

        // The expansion was successful.
        expansionCityId = null;
        expansionOutlines.clear();

        // Immediately rebuild civilization borders.
        territoryBorderRenderer.refresh(
                gameState.getTerritoryManager(),
                gameState.getCivilizationManager()
        );

        Gdx.app.log(
                "TERRITORY",
                city.getName()
                        + " expanded to "
                        + targetTile.getX()
                        + ", "
                        + targetTile.getY()
        );

        updateInfoPanel();
    }

    private void toggleAutomaticExpansion() {

        if (selectedTile == null) {
            return;
        }

        var cityOptional =
                gameState.getCityManager()
                        .findCityAt(
                                selectedTile.getX(),
                                selectedTile.getY()
                        );

        if (cityOptional.isEmpty()) {
            return;
        }

        City city = cityOptional.get();

        if (city.getCivilizationId() == null) {
            return;
        }

        var expansionManager =
                gameState.getCityExpansionManager();

        expansionManager.toggleMode(
                city.getId()
        );

        // If automation was enabled while manual
        // tile selection was active, cancel selection.
        if (expansionManager.isAutomatic(city.getId())
                && expansionCityId != null
                && expansionCityId == city.getId()) {

            expansionCityId = null;
            expansionOutlines.clear();
        }

        updateInfoPanel();
    }

    private void updateExpansionOutlines() {

        expansionOutlines.clear();

        if (expansionCityId == null) {
            return;
        }

        City city = gameState.getCityManager()
                .findCity(expansionCityId)
                .orElse(null);

        if (city == null) {
            return;
        }

        var eligibleTiles =
                expansionService.getEligibleTiles(
                        city,
                        worldMap,
                        gameState.getCityManager(),
                        gameState.getTerritoryManager()
                );

        for (var position : eligibleTiles) {

            Tile tile = worldMap.getTile(
                    position.x(),
                    position.y()
            );

            Vector3[] vertices =
                    TerrainMeshBuilder.getTileVertices(
                            worldMap,
                            tile
                    );

            for (int i = 0; i < 4; i++) {

                Vector3 start = new Vector3(
                        vertices[i]
                ).add(0f, 0.12f, 0f);

                Vector3 end = new Vector3(
                        vertices[(i + 1) % 4]
                ).add(0f, 0.12f, 0f);

                addExpansionOutlineEdge(
                        start,
                        end
                );
            }
        }
    }

    private void addExpansionOutlineEdge(
            Vector3 start,
            Vector3 end
    ) {

        Vector3 direction =
                new Vector3(end).sub(start);

        float length = direction.len();

        if (length < 0.0001f) {
            return;
        }

        direction.nor();

        Vector3 midpoint =
                new Vector3(start)
                        .add(end)
                        .scl(0.5f);

        Quaternion rotation =
                new Quaternion()
                        .setFromCross(
                                Vector3.Y,
                                direction
                        );

        ModelInstance instance =
                new ModelInstance(selectionEdgeModel);

        instance.transform.set(
                midpoint,
                rotation,
                new Vector3(
                        0.075f,
                        length,
                        0.075f
                )
        );

        instance.materials.first().set(
                ColorAttribute.createDiffuse(
                        new Color(
                                0.15f,
                                0.95f,
                                0.30f,
                                1f
                        )
                )
        );

        expansionOutlines.add(instance);
    }


    // =========================================================================
    // Input and tile selection
    // =========================================================================

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

                if (expansionCityId != null) {

                    handleExpansionClick(
                            screenX,
                            screenY
                    );

                    return true;
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

                if (keycode == Input.Keys.ESCAPE
                        && expansionCityId != null) {

                    cancelExpansionMode();
                    return true;
                }

                return false;

            }
        };

        Gdx.input.setInputProcessor(
                new InputMultiplexer(
                        uiStage,
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


    // =========================================================================
    // River routes and transport
    // =========================================================================

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

        int claimedTilesBefore =
                gameState.getTerritoryManager()
                        .getClaimedTileCount();

        GameEngine.TurnReport report = gameEngine.endTurn();
        RiverTransportManager manager =
                gameState.getRiverTransportManager();

        int claimedTilesAfter =
                gameState.getTerritoryManager()
                        .getClaimedTileCount();

        if (claimedTilesAfter != claimedTilesBefore) {

            territoryBorderRenderer.refresh(
                    gameState.getTerritoryManager(),
                    gameState.getCivilizationManager()
            );

            Gdx.app.log(
                    "TERRITORY",
                    "Borders updated after automatic expansion"
            );
        }

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


    // =========================================================================
    // Application lifecycle: drawing and cleanup
    // =========================================================================

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
        cityRenderer.render(modelBatch, environment);
        cityBuildingRenderer.render(modelBatch, environment);
        modelBatch.render(expansionOutlines, environment);
        riverRouteRenderer.render(modelBatch, environment);
        riverTransportRenderer.render(modelBatch, environment);
        territoryBorderRenderer.render(modelBatch, environment);
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

        uiStage.getViewport().update(
                width,
                height,
                true
        );

        positionBuildingPanel();
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

        if (territoryBorderRenderer != null) {
            territoryBorderRenderer.dispose();
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
        if (cityRenderer != null) {
            cityRenderer.dispose();
        }
        if (buttonTexture != null) {
            buttonTexture.dispose();
        }

        if (buttonHoverTexture != null) {
            buttonHoverTexture.dispose();
        }

        if (cityBuildingRenderer != null) {
            cityBuildingRenderer.dispose();
        }

    }

}
