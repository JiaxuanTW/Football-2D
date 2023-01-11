package com.mouse;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
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
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

public class MyGame extends SimpleApplication implements ActionListener, AnalogListener {
    private World world = new World();
    private BodyControl selectedPlayer;
    private Geometry lineGeom;
    private static final float PPM = 100;

    public static void main(String[] args) {
        MyGame app = new MyGame();
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setTitle("2D Football Game");
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

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

        createBackground();
        createBoundaries();
        createPlayers();
        createBall();

        inputManager.addMapping("LeftClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "LeftClick");

        world.setGravity(World.ZERO_GRAVITY);
    }

    @Override
    public void simpleUpdate(float tpf) {
        world.update(tpf);
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("LeftClick")) {
            if (isPressed) {
                Vector2f click2D = inputManager.getCursorPosition();
                Vector3f click3D = cam.getWorldCoordinates(click2D, 0);
                Vector3f direction = cam.getWorldCoordinates(click2D, 1)
                        .subtractLocal(click3D).normalizeLocal();

                CollisionResults results = new CollisionResults();
                rootNode.collideWith(new Ray(click3D, direction), results);
                if (results.size() > 0) {
                    Spatial spatial = results.getClosestCollision().getGeometry();
                    if (spatial.getName().equals("Player")) {
                        selectedPlayer = spatial.getUserData("bodyControl");
                    }
                }
            } else {
                if (selectedPlayer != null) {
                    selectedPlayer.body.applyForce(new Vector2(100, 10));
                    selectedPlayer = null;
                }
            }
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (name.equals("LeftClick") && selectedPlayer != null) {
            if (lineGeom != null) lineGeom.removeFromParent();
            Vector2f click2D = inputManager.getCursorPosition();
            Vector3f click3D = cam.getWorldCoordinates(click2D, 0);
            Vector2 playerCenter2D = selectedPlayer.body.getWorldCenter();

            Line line = new Line(new Vector3f((float) playerCenter2D.x, (float) playerCenter2D.y, 0), click3D);
            lineGeom = new Geometry("Line", line);
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Blue);
            line.setLineWidth(20);
            lineGeom.setMaterial(mat);
            rootNode.attachChild(lineGeom);
        }
    }

    private void createBackground() {
        Mesh backgroundMesh = new Box(Vector3f.ZERO, cam.getWidth() / PPM / 2, cam.getHeight() / PPM / 2, 0);
        Geometry background = new Geometry("Background", backgroundMesh);
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

    private void createPlayers() {
        final float playerRadius = 35f / PPM;

        Body body = new Body();
        BodyFixture fixture = body.addFixture(new Circle(playerRadius));
        fixture.setRestitution(0.7);
        body.setMass(MassType.NORMAL);
        body.setLinearDamping(0.4);
        body.setAngularDamping(0.4);
        body.translate(-100 / PPM, -60 / PPM);
        world.addBody(body);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        Sphere sphere = new Sphere(32, 32, playerRadius);
        Geometry geom = new Geometry("Player", sphere);
        geom.setMaterial(mat);

        Node node = new Node("PlayerNode");
        BodyControl control = new BodyControl(body);
        geom.setUserData("bodyControl", control);
        node.addControl(control);
        node.attachChild(geom);

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

        Texture tex = assetManager.loadTexture("Textures/football.png");
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Quad quad = new Quad(ballRadius * 2, ballRadius * 2);
        Geometry geom = new Geometry("body", quad);
        geom.setMaterial(mat);
        geom.setLocalTranslation(-ballRadius, -ballRadius, 0);

        Node node = new Node("Ball");
        node.addControl(new BodyControl(body));
        node.attachChild(geom);

        rootNode.attachChild(node);
    }
}
