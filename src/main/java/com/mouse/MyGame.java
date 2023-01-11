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
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.simsilica.lemur.*;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Force;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.util.HashMap;
import java.util.Map;



public class MyGame extends SimpleApplication implements ActionListener, AnalogListener {
    private final World<Body> world = new World<>();
    Map<String, Force> action = new HashMap<>();
    private static final float PPM = 100;
    private Spatial selectedPlayer;
    //-------------------------------------------------------------------------------------------------------------//
    private String ally = "B"; // Blue, Red, None
    private int bScore = 0;
    private int rScore = 0;
    private int turns = 1;

    @Override
    public void simpleInitApp() {
        cam.setParallelProjection(true);
        cam.setLocation(new Vector3f(0 / PPM, 0 / PPM, 1 / PPM));
        cam.setFrustum(-1000, 1000,
                -cam.getWidth() / PPM / 2,
                cam.getWidth() / PPM / 2,
                cam.getHeight() / PPM / 2,
                -cam.getHeight() / PPM / 2);
        getFlyByCamera().setEnabled(false);

        setDisplayStatView(false);
        setDisplayFps(false);

        // Init Lemur GUI
        GuiGlobals.initialize(this);


        createBackground();
        createBoundaries();
        createPlayers("Player-B1", ColorRGBA.Blue, -200, 80);
        createPlayers("Player-B2", ColorRGBA.Blue, -150, -60);
        createPlayers("Player-B3", ColorRGBA.Blue, -200, -200);
        createPlayers("Player-R1", ColorRGBA.Red, 200, 80);
        createPlayers("Player-R2", ColorRGBA.Red, 150, -60);
        createPlayers("Player-R3", ColorRGBA.Red, 200, -200);
        createBall();
        createHUD();
        setText(bScore, rScore);

        inputManager.addMapping("LeftClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "LeftClick");

        world.setGravity(World.ZERO_GRAVITY);
    }

    //----------------------------------------------------------------------------------------------------------//
    public static void main(String[] args) {
        MyGame app = new MyGame();
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setTitle("2D Football Game");
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    private void setText(int bs, int rs) {
        BitmapFont font = assetManager.loadFont("Interface/default.fnt");
        BitmapText text = new BitmapText(font);
        text.setText("12");
        text.setSize(55f);
        text.setColor(ColorRGBA.Black);
        text.setLocalTranslation(380, 670, 0);
        guiNode.attachChild(text);
    }

    @Override
    public void simpleUpdate(float tpf) {
        world.update(tpf);
        Boolean stop = true;

        BodyControl obj = rootNode.getChild("Ball").getUserData("bodyControl");
        if (!obj.body.getLinearVelocity().equals(new Vector2(0, 0))) {
            stop = false;
        }

    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("LeftClick")) {
            Vector2f click2D = inputManager.getCursorPosition();
            Vector3f click3D = cam.getWorldCoordinates(click2D, 0);
            if (isPressed) {
                // Collision detection
                Vector3f rayDirection = cam.getWorldCoordinates(click2D, 1)
                        .subtractLocal(click3D).normalizeLocal();
                CollisionResults results = new CollisionResults();
                rootNode.collideWith(new Ray(click3D, rayDirection), results);
                if (results.size() > 0) {
                    Spatial spatial = results.getClosestCollision().getGeometry();
                    System.out.println(spatial.getName());
                    if (spatial.getName().startsWith("Player-" + ally)) {
                        selectedPlayer = spatial;
                    }
                }
            } else {
                if (selectedPlayer != null) {
                    BodyControl bodyCtrl = selectedPlayer.getUserData("bodyControl");
                    Vector2 player2D = bodyCtrl.body.getWorldCenter();
                    Vector3f player3D = new Vector3f((float) player2D.x, (float) player2D.y, 0);

                    Vector3f newClick3D = new Vector3f(click3D.x, click3D.y, 0);
                    Vector3f direction = newClick3D.subtract(player3D);

                    // Saving pre_executed Force into "move" map with player's name

                    System.out.println(direction.length());
                    double power = 50 * (direction.length() > 5 ? 5 : direction.length());
                    System.out.println(power);
                    direction.normalizeLocal();
                    action.put(selectedPlayer.getName(), new Force(
                            direction.x * power,
                            direction.y * power
                    ));

                    // selectedPlayer.body.applyForce(new Vector2(
                    //         direction.x * direction.length() * 40,
                    //         direction.y * direction.length() * 40)
                    // );
                    selectedPlayer = null;
                }
            }
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }

    private void createPlayers(String name, ColorRGBA color, float posX, float posY) {
        final float playerRadius = 35 / PPM;
        final float triangleSize = 20 / PPM;
        final float rectHeight = 10 / PPM;

        // Create body for the player
        Body body = new Body();
        BodyFixture fixture = body.addFixture(new Circle(playerRadius));
        fixture.setRestitution(0.05);
        body.setMass(MassType.NORMAL);
        body.setLinearDamping(1);
        body.setAngularDamping(Double.MAX_VALUE);
        body.translate(posX / PPM, posY / PPM);
        world.addBody(body);

        // Create player
        Sphere playerMesh = new Sphere(32, 32, playerRadius);
        Material playerMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        playerMat.setColor("Color", color);
        Geometry playerGeom = new Geometry(name, playerMesh);
        playerGeom.setMaterial(playerMat);

        // Create a small triangle indicating the direction of player
        Quad directMesh = new Quad(triangleSize, triangleSize);
        Texture directTex = assetManager.loadTexture("Textures/triangle.png");
        Material directMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        directMat.setTexture("ColorMap", directTex);
        directMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        Geometry directGeom = new Geometry("Triangle", directMesh);
        directGeom.setMaterial(directMat);
        directGeom.move(-triangleSize / 2, playerRadius + 10 / PPM, 0);

        // Create the charge bar - background
        Quad chargeBarBgMesh = new Quad(playerRadius * 2, rectHeight);
        Material chargeBarBgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        chargeBarBgMat.setColor("Color", ColorRGBA.White);
        Geometry chargeBarBgGeom = new Geometry("ChargeBarBackground", chargeBarBgMesh);
        chargeBarBgGeom.setMaterial(chargeBarBgMat);
        chargeBarBgGeom.move(-playerRadius, -playerRadius - 20 / PPM, 0);

        // Create the charge bar - percentage
        Quad chargeBarMesh = new Quad(playerRadius * 2, rectHeight);
        Material chargeBarMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        chargeBarMat.setColor("Color", ColorRGBA.Blue);
        Geometry chargeBarGeom = new Geometry("ChargeBar", chargeBarMesh);
        chargeBarGeom.setMaterial(chargeBarMat);
        chargeBarGeom.move(-playerRadius, -playerRadius - 20 / PPM, 0);

        Node node = new Node("PlayerNode");
        BodyControl control = new BodyControl(body);
        playerGeom.setUserData("bodyControl", control);

        node.addControl(control);
        node.attachChild(playerGeom);
        node.attachChild(directGeom);
        // node.attachChild(chargeBarBgGeom);
        // node.attachChild(chargeBarGeom);

        rootNode.attachChild(node);
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (name.equals("LeftClick") && selectedPlayer != null) {
            BodyControl bodyCtrl = selectedPlayer.getUserData("bodyControl");

            Vector2f click2D = inputManager.getCursorPosition();
            Vector3f click3D = cam.getWorldCoordinates(click2D, 0);
            Vector2 player2D = bodyCtrl.body.getWorldCenter();
            Vector3f player3D = new Vector3f((float) player2D.x, (float) player2D.y, 0);

            Vector3f newClick3D = new Vector3f(click3D.x, click3D.y, 0);
            Vector3f direction = newClick3D.subtract(player3D).normalize();
            double playerAngle = bodyCtrl.body.getTransform().getRotationAngle();
            float cursorAngle = direction.angleBetween(new Vector3f(0, 1, 0));
            cursorAngle *= direction.x > 0 ? -1 : 1;
            bodyCtrl.body.rotateAboutCenter(cursorAngle - playerAngle);
        }
    }

    private void createBackground() {
        Quad plane = new Quad(cam.getWidth() / PPM, cam.getHeight() / PPM);
        Geometry background = new Geometry("Background", plane);
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

    private void createBar() {

    }

    private void createHUD() {
        Container myWindow = new Container();
        guiNode.attachChild(myWindow);

        myWindow.setLocalTranslation(30, 650, 0);
        myWindow.addChild(new Label("Hello,World."));

        Button clickMe = myWindow.addChild(new Button("Click Me"));
        clickMe.addClickCommands(new Command<Button>() {
            @Override
            public void execute(Button source) {
                for (Map.Entry<String, Force> entry : action.entrySet()) {
                    BodyControl bodyCtrl = rootNode.getChild(entry.getKey()).getUserData("bodyControl");
                    bodyCtrl.body.applyForce(entry.getValue());

                }
                action.clear();
            }
        });


    }

    private enum GameState {
        READY, WAITING, RUNNING, SCORE
    }
}
