package com.mouse;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.network.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.texture.Texture;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Force;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class MyGame extends SimpleApplication implements ActionListener, AnalogListener {
    Button clickMe;
    Client myClient = null;
    private static final float PPM = 100;
    private static final float POWER_LIMIT = 5;
    private static final float POWER_MULTIPLIER = 50;
    private final World<Body> world = new World<>();
    private Spatial selectedPlayer;
    private Map<String, Force> forces = new HashMap<>();
    private String myTeamColor = "N"; // B(Blue), R(Red), N(None)
    private int blueScore = 0;
    private int redScore = 0;
    private int turns = 0;
    private double[][] forceArray = new double[6][2];

    public static void main(String[] args) {


        MyGame app = new MyGame();
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setTitle("2D Football Game");
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start(JmeContext.Type.Display);
    }

    @Override
    public void simpleInitApp() {

        // Set up camera
        cam.setParallelProjection(true);
        cam.setLocation(new Vector3f(0 / PPM, 0 / PPM, 1 / PPM));
        cam.setFrustum(-1000, 1000,
                -cam.getWidth() / PPM / 2,
                cam.getWidth() / PPM / 2,
                cam.getHeight() / PPM / 2,
                -cam.getHeight() / PPM / 2);
        getFlyByCamera().setEnabled(false);

        // Remove debug info
        setDisplayStatView(false);
        setDisplayFps(false);


        // Init Lemur GUI
        GuiGlobals.initialize(this);

        // Create objects
        createBackground();
        createBoundaries();
        createGoalArea();
        createPlayer("Player-B1", ColorRGBA.Blue, -200, 80);
        createPlayer("Player-B2", ColorRGBA.Blue, -150, -60);
        createPlayer("Player-B3", ColorRGBA.Blue, -200, -200);
        createPlayer("Player-R1", ColorRGBA.Red, 200, 80);
        createPlayer("Player-R2", ColorRGBA.Red, 150, -60);
        createPlayer("Player-R3", ColorRGBA.Red, 200, -200);
        createBall();
        createHUD();
        createText(blueScore, redScore);


        try {
            myClient = Network.connectToServer("localhost", 4234);
            Serializer.registerClass(PlayerMessage.class);
            myClient.start();
            myClient.addMessageListener(new ClientListener(), PlayerMessage.class);
            myClient.addClientStateListener(new CSL());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // Register input listeners
        inputManager.addMapping("LeftClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "LeftClick");

        // Set gravity to zero
        world.setGravity(World.ZERO_GRAVITY);
    }

    @Override
    public void simpleUpdate(float tpf) {
        world.update(tpf);

        Spatial ball = rootNode.getChild("Ball");
        Spatial blueGoalArea = rootNode.getChild("GoalAreaBlue");
        Spatial redGoalArea = rootNode.getChild("GoalAreaRed");

        // Detect if the ball enters the goal
        if (blueGoalArea.getWorldBound().intersects(ball.getWorldBound())) {
            System.out.println("Goal (Ball enters the Blue's goal)");
        } else if (redGoalArea.getWorldBound().intersects(ball.getWorldBound())) {
            System.out.println("Goal (Ball enters the Red's goal)");
        }

        // TODO: If every object stops moving -> change game state
        BodyControl ballBodyCtrl = ball.getUserData("bodyControl");
        if (!ballBodyCtrl.body.getLinearVelocity().equals(new Vector2(0, 0))) {
            // If the ball stops
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }

    @Override
    public void onAction(@NotNull String name, boolean isPressed, float tpf) {
        if (clickMe.getText().equals("Ready")) {
            if (name.equals("LeftClick")) {
                // Get the cursor position and translate it to the world coordinates
                Vector2f click2D = inputManager.getCursorPosition();
                Vector3f click3D = cam.getWorldCoordinates(click2D, 0);
                if (isPressed) {
                    // Use left mouse click to select a player
                    // Collision detection for selecting a player
                    Vector3f rayDirection = cam.getWorldCoordinates(click2D, 1)
                            .subtractLocal(click3D).normalizeLocal();
                    CollisionResults results = new CollisionResults();
                    rootNode.collideWith(new Ray(click3D, rayDirection), results);

                    if (results.size() > 0) {
                        Spatial spatial = results.getClosestCollision().getGeometry();
                        if (spatial.getName().startsWith("Player-" + myTeamColor)) {
                            // Can only select the players on your team
                            selectedPlayer = spatial;
                        }
                    }
                } else if (selectedPlayer != null) { // If mouse click released and a player is selected
                    // Get the position of the selected player and translate it to the world coordinates
                    BodyControl bodyCtrl = selectedPlayer.getUserData("bodyControl");
                    Vector2 player2D = bodyCtrl.body.getWorldCenter();
                    Vector3f player3D = new Vector3f((float) player2D.x, (float) player2D.y, 0);
                    Vector3f newClick3D = new Vector3f(click3D.x, click3D.y, 0); // Set z-value to zero
                    Vector3f direction = newClick3D.subtract(player3D);

                    // Charge the power by dragging the mouse
                    // Get higher power if dragging farther from the center of the selected player
                    double power = (Math.min(direction.length(), POWER_LIMIT)) * POWER_MULTIPLIER;
                    direction.normalizeLocal(); // Normalize the direction vector and multiply by power value

                    // Store the force of players into a map
                    // After both users set up the forces of players and get ready -> apply the forces to the players
                    forces.put(selectedPlayer.getName(), new Force(
                            direction.x * power,
                            direction.y * power
                    ));

                    // Update the charge bar
                    displayChargeBar(selectedPlayer.getName(), power, selectedPlayer.getWorldTranslation());
                    selectedPlayer = null; // Deselect the player when mouse click released
                }
            }
        }
    }

    @Override
    public void onAnalog(@NotNull String name, float value, float tpf) {
        if (clickMe.getText().equals("Ready")) {
            if (name.equals("LeftClick") && selectedPlayer != null) {
                // If the mouse is on dragging and a player is selected
                // A player is selected by a left click (See onAction() method)

                // Get the position of the cursor and the selected player
                Vector2f click2D = inputManager.getCursorPosition();
                Vector3f click3D = cam.getWorldCoordinates(click2D, 0);
                BodyControl bodyCtrl = selectedPlayer.getUserData("bodyControl");
                Vector2 player2D = bodyCtrl.body.getWorldCenter();
                Vector3f player3D = new Vector3f((float) player2D.x, (float) player2D.y, 0);
                Vector3f newClick3D = new Vector3f(click3D.x, click3D.y, 0); // Set z-value to zero
                Vector3f direction = newClick3D.subtract(player3D);

                // Charge the power by dragging the mouse
                double power = (Math.min(direction.length(), POWER_LIMIT)) * POWER_MULTIPLIER;
                direction.normalizeLocal();
                forces.put(selectedPlayer.getName(), new Force(
                        direction.x * power,
                        direction.y * power
                ));

                // Update the charge bar
                displayChargeBar(selectedPlayer.getName(), power, selectedPlayer.getWorldTranslation());

                // Rotate the player
                double playerAngle = bodyCtrl.body.getTransform().getRotationAngle();
                float cursorAngle = direction.angleBetween(new Vector3f(0, 1, 0));
                cursorAngle *= direction.x > 0 ? -1 : 1;
                bodyCtrl.body.rotateAboutCenter(cursorAngle - playerAngle);
            }
        }
    }

    private void createBackground() {
        Quad quad = new Quad(cam.getWidth() / PPM, cam.getHeight() / PPM);
        Geometry background = new Geometry("Background", quad);
        background.center();
        Texture texture = assetManager.loadTexture("Textures/background.png");
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", texture);
        background.setMaterial(material);
        rootNode.attachChild(background);
    }

    private void createBoundaries() {
        Body[] horizontalBoundaries = new Body[2];
        Body[] verticalBoundaries = new Body[2];
        Body[] cornerBoundaries = new Body[4];

        for (int i = 0; i < 2; i++) {
            horizontalBoundaries[i] = new Body();
            horizontalBoundaries[i].setMass(MassType.INFINITE);
            horizontalBoundaries[i].addFixture(new Rectangle(1000 / PPM, 10 / PPM));
            world.addBody(horizontalBoundaries[i]);

            Node node = new Node("BoundaryNode");
            node.addControl(new BodyControl(horizontalBoundaries[i]));

            // Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            // mat.setColor("Color", ColorRGBA.Blue);
            // Geometry geom = new Geometry("borderGeom", new Quad(1000 / PPM, 10 / PPM));
            // geom.move(-500 / PPM, -5 / PPM, 0 / PPM);
            // geom.setMaterial(mat);
            // node.attachChild(geom);

            rootNode.attachChild(node);
        }

        for (int i = 0; i < 2; i++) {
            verticalBoundaries[i] = new Body();
            verticalBoundaries[i].setMass(MassType.INFINITE);
            verticalBoundaries[i].addFixture(new Rectangle(10 / PPM, 500 / PPM));
            world.addBody(verticalBoundaries[i]);

            Node node = new Node("BoundaryNode");
            node.addControl(new BodyControl(verticalBoundaries[i]));

            // Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            // mat.setColor("Color", ColorRGBA.Blue);
            // Geometry geom = new Geometry("borderGeom", new Quad(10 / PPM, 500 / PPM));
            // geom.move(-5 / PPM, -250 / PPM, 0 / PPM);
            // geom.setMaterial(mat);
            // node.attachChild(geom);

            rootNode.attachChild(node);
        }

        for (int i = 0; i < 4; i++) {
            cornerBoundaries[i] = new Body();
            cornerBoundaries[i].setMass(MassType.INFINITE);
            cornerBoundaries[i].addFixture(new Rectangle(90 / PPM, 125 / PPM));
            world.addBody(cornerBoundaries[i]);

            Node node = new Node("BoundaryNode");
            node.addControl(new BodyControl(cornerBoundaries[i]));

            // Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            // mat.setColor("Color", ColorRGBA.Blue);
            // Geometry geom = new Geometry("borderGeom", new Quad(90 / PPM, 125 / PPM));
            // geom.move(-45 / PPM, -62.5f / PPM, 0 / PPM);
            // geom.setMaterial(mat);
            // node.attachChild(geom);

            rootNode.attachChild(node);
        }

        horizontalBoundaries[0].translate(0 / PPM, 195 / PPM);
        horizontalBoundaries[1].translate(0 / PPM, -315 / PPM);
        verticalBoundaries[0].translate(-590 / PPM, -60 / PPM);
        verticalBoundaries[1].translate(590 / PPM, -60 / PPM);
        cornerBoundaries[0].translate(-545 / PPM, 130 / PPM);
        cornerBoundaries[1].translate(545 / PPM, 130 / PPM);
        cornerBoundaries[2].translate(-545 / PPM, -250 / PPM);
        cornerBoundaries[3].translate(545 / PPM, -250 / PPM);
    }

    private void createGoalArea() {
        // Create the goal area on blue team
        Quad blueGoalAreaMesh = new Quad(60 / PPM, 255 / PPM);
        Material blueGoalAreaMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Geometry blueGoalAreaGeom = new Geometry("GoalAreaBlue", blueGoalAreaMesh);
        blueGoalAreaGeom.setMaterial(blueGoalAreaMat);
        blueGoalAreaGeom.setLocalTranslation(-585 / PPM, -187.5f / PPM, 0 / PPM);
        rootNode.attachChild(blueGoalAreaGeom);

        // Create the goal area on red team
        Quad redGoalAreaMesh = new Quad(60 / PPM, 255 / PPM);
        Material redGoalAreaMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Geometry redGoalAreaGeom = new Geometry("GoalAreaRed", redGoalAreaMesh);
        redGoalAreaGeom.setMaterial(redGoalAreaMat);
        redGoalAreaGeom.setLocalTranslation(525 / PPM, -187.5f / PPM, 0 / PPM);
        rootNode.attachChild(redGoalAreaGeom);
    }

    private void createPlayer(String name, ColorRGBA color, float posX, float posY) {
        final float playerRadius = 35 / PPM;
        final float triangleSize = 20 / PPM;

        Body body = new Body();
        BodyFixture fixture = body.addFixture(new Circle(playerRadius));
        fixture.setRestitution(0.05);
        body.setMass(MassType.NORMAL);
        body.setLinearDamping(1);
        body.setAngularDamping(Double.MAX_VALUE);
        body.translate(posX / PPM, posY / PPM);
        world.addBody(body);

        Sphere playerMesh = new Sphere(32, 32, playerRadius);
        Material playerMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        playerMat.setColor("Color", color);
        Geometry playerGeom = new Geometry(name, playerMesh);
        playerGeom.setMaterial(playerMat);

        // Create a small triangle indicating the direction of a player
        Quad directMesh = new Quad(triangleSize, triangleSize);
        Texture directTex = assetManager.loadTexture("Textures/triangle.png");
        Material directMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        directMat.setTexture("ColorMap", directTex);
        directMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        Geometry directGeom = new Geometry("Triangle", directMesh);
        directGeom.setMaterial(directMat);
        directGeom.move(-triangleSize / 2, playerRadius + 10 / PPM, 0);

        Node node = new Node("PlayerNode");
        BodyControl control = new BodyControl(body);
        playerGeom.setUserData("bodyControl", control);
        node.addControl(control);
        node.attachChild(playerGeom);
        node.attachChild(directGeom);

        rootNode.attachChild(node);
    }

    private void createBall() {
        final float ballRadius = 25f / PPM;

        Body body = new Body();
        BodyFixture fixture = body.addFixture(new Circle(ballRadius));
        fixture.setRestitution(0.7);
        body.setMass(MassType.NORMAL);
        body.setGravityScale(5f);
        body.setLinearDamping(0.4);
        body.setAngularDamping(0.4);
        body.translate(0 / PPM, -60 / PPM);
        world.addBody(body);

        Quad quad = new Quad(ballRadius * 2, ballRadius * 2);
        Texture tex = assetManager.loadTexture("Textures/football.png");
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        Geometry geom = new Geometry("Ball", quad);
        geom.setMaterial(mat);
        geom.setLocalTranslation(-ballRadius, -ballRadius, 0);

        Node node = new Node("BallNode");
        BodyControl control = new BodyControl(body);
        geom.setUserData("bodyControl", control);
        node.addControl(control);
        node.attachChild(geom);

        rootNode.attachChild(node);
    }

    private void createHUD() {
        Container container = new Container();
        container.setLocalTranslation(30, 650, 0);
        guiNode.attachChild(container);


        clickMe = container.addChild(new Button("CONNECTING"));
        clickMe.setEnabled(false);
        clickMe.addClickCommands(source -> {
            // If the button is clicked -> Apply forces to each player

            // Message message = new PlayerMessage("Shot",forceArray);
            // message.setReliable(true);

            // myClient.send(message);
            clickMe.setEnabled(false);
            clickMe.setText("Waiting");

            // TODO: Remove charge bar (Do it in RUNNING state)
            rootNode.detachChildNamed("ChargeBar-Player-B1");
            rootNode.detachChildNamed("ChargeBar-Player-B2");
            rootNode.detachChildNamed("ChargeBar-Player-B3");
            rootNode.detachChildNamed("ChargeBarBg-Player-B1");
            rootNode.detachChildNamed("ChargeBarBg-Player-B2");
            rootNode.detachChildNamed("ChargeBarBg-Player-B3");
        });
    }

    private void createText(int blueScore, int redScore) {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText text = new BitmapText(font);
        text.setText(String.valueOf(blueScore));
        text.setSize(55f);
        text.setColor(ColorRGBA.Black);
        text.setLocalTranslation(380, 670, 0);
        guiNode.attachChild(text);
    }

    private void displayChargeBar(String name, double power, Vector3f pos) {
        final float playerRadius = 35 / PPM;
        final float rectHeight = 10 / PPM;

        // Try to get charge bar from rootNode and update the width (the percentage of power)
        Geometry oldChargeBar = (Geometry) rootNode.getChild("ChargeBar-" + name);
        // If the charge bar has not created -> Create one
        if (oldChargeBar == null) {
            // Create the charge bar - background
            Quad chargeBarBgMesh = new Quad(playerRadius * 2, rectHeight);
            Material chargeBarBgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            chargeBarBgMat.setColor("Color", ColorRGBA.White);
            Geometry chargeBarBgGeom = new Geometry("ChargeBarBg-" + name, chargeBarBgMesh);
            chargeBarBgGeom.setMaterial(chargeBarBgMat);
            chargeBarBgGeom.setLocalTranslation(pos);
            chargeBarBgGeom.move(-playerRadius, -playerRadius - 20 / PPM, 0);

            // Create the charge bar - percentage
            Quad chargeBarMesh = new Quad(playerRadius * 2f * (float) power / 250f, rectHeight);
            Material chargeBarMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            chargeBarMat.setColor("Color", myTeamColor == "B" ? ColorRGBA.Blue : ColorRGBA.Red);
            Geometry chargeBarGeom = new Geometry("ChargeBar-" + name, chargeBarMesh);
            chargeBarGeom.setMaterial(chargeBarMat);
            chargeBarGeom.setLocalTranslation(pos);
            chargeBarGeom.move(-playerRadius, -playerRadius - 20 / PPM, 0);

            rootNode.attachChildAt(chargeBarGeom, 1);
            rootNode.attachChildAt(chargeBarBgGeom, 1);
            return;
        }

        // Update the charge bar
        Quad q = (Quad) oldChargeBar.getMesh();
        q.updateGeometry(playerRadius * 2f * (float) power / 250f, rectHeight);
        q.updateCounts();
        oldChargeBar.updateGeometricState();
        oldChargeBar.updateModelBound();
    }


    public class ClientListener implements MessageListener<Client> {
        public void messageReceived(Client source, Message message) {
            if (message instanceof PlayerMessage) {
                // do something with the message
                PlayerMessage helloMessage = (PlayerMessage) message;

                String command = helloMessage.getCommand();
                // Blue
                if (command.equals("Blue")) {
                    myTeamColor = "B";
                }
                // Red
                if (command.equals("Red")) {
                    myTeamColor = "R";
                }
                if (command.equals("Start")) {
                    clickMe.setText("Ready");
                    clickMe.setEnabled(true);
                }
                if (command.equals("Run")) {
                    forces = helloMessage.getForces();
                    clickMe.setText("Running");
                    for (Map.Entry<String, Force> entry : forces.entrySet()) {
                        BodyControl bodyCtrl = rootNode.getChild(entry.getKey()).getUserData("bodyControl");
                        bodyCtrl.body.applyForce(entry.getValue());
                    }
                    forces.clear(); // Clear the forces map
                }
            } // else...
        }
    }

    public class CSL implements ClientStateListener {

        @Override
        public void clientConnected(Client c) {
            Message message = new PlayerMessage("Hello World!");
            message.setReliable(true);
            myClient.send(message);
        }

        @Override
        public void clientDisconnected(Client c, DisconnectInfo info) {
            System.out.println("Disconnect");
        }

        public void ForcesToArray() {
            for (Map.Entry<String, Force> entry : forces.entrySet()) {
                String name = entry.getKey();
                if (name.startsWith("Player-B")) {
                    if (name.endsWith("1")) {
                        System.out.println("1");
                    }

                } else if (entry.getKey().startsWith("Player-R")) {

                }
            }
        }
    }
}
