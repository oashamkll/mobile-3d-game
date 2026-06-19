package com.arena.mobile3d;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/** High-FPS third-person 3D mobile prototype with generated premium texture pack. */
public class AdventureGame extends ApplicationAdapter {
    private static final long ATTRS = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private ShapeRenderer shapes;
    private SpriteBatch sprites;
    private BitmapFont font;
    private ModelBuilder modelBuilder;

    private final Array<Model> models = new Array<>();
    private final Array<ModelInstance> world = new Array<>();
    private final Array<HeroPart> hero = new Array<>();

    private Texture grass, stone, wood, leaves, cloth, skin, leather, hair, metal;
    private Sound footstepSound, chimeSound, windSound;
    private long windId = -1;

    private final Vector3 heroPos = new Vector3(0f, 0.18f, 0f);
    private final Vector3 velocity = new Vector3();
    private final Vector3 camForward = new Vector3();
    private final Vector3 camRight = new Vector3();
    private final Vector3 moveVec = new Vector3();
    private final Vector3 desiredCam = new Vector3();
    private float heroYaw = 0f;
    private float cameraYaw = 35f;
    private float cameraPitch = 24f;
    private float walkTime = 0f;
    private int lastStepIndex = -1;

    private final Vector2 moveStick = new Vector2();
    private final Vector2 stickOrigin = new Vector2();
    private final Vector2 stickCurrent = new Vector2();
    private int stickPointer = -1;
    private int lookPointer = -1;
    private float lastLookX, lastLookY;

    private float hudScale = 1f;

    @Override
    public void create() {
        Gdx.graphics.setContinuousRendering(true);
        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        shapes = new ShapeRenderer();
        sprites = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.15f);
        font.setColor(Color.WHITE);

        grass = tex("textures/environment/grass_albedo.png");
        stone = tex("textures/environment/stone_albedo.png");
        wood = tex("textures/environment/wood_albedo.png");
        leaves = tex("textures/environment/leaves_albedo.png");
        cloth = tex("textures/character/hero_cloth_albedo.png");
        skin = tex("textures/character/hero_skin_albedo.png");
        leather = tex("textures/character/hero_leather_albedo.png");
        hair = tex("textures/character/hero_hair_albedo.png");
        metal = tex("textures/character/hero_metal_albedo.png");

        footstepSound = loadSound("audio/footstep.wav");
        chimeSound = loadSound("audio/chime.wav");
        windSound = loadSound("audio/wind_loop.wav");
        if (windSound != null) windId = windSound.loop(0.18f);
        if (chimeSound != null) chimeSound.play(0.55f);

        camera = new PerspectiveCamera(66f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 120f;

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.64f, 0.68f, 0.78f, 1f));
        environment.add(new DirectionalLight().set(1f, 0.92f, 0.76f, -0.38f, -0.86f, -0.34f));
        environment.add(new DirectionalLight().set(0.25f, 0.38f, 0.62f, 0.62f, -0.35f, 0.35f));

        buildWorld();
        buildHero();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        setupInput();
    }

    private Texture tex(String path) {
        try {
            Texture t = new Texture(Gdx.files.internal(path), false);
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            return t;
        } catch (Throwable error) {
            Gdx.app.error("AdventureGame", "Texture load failed: " + path, error);
            return fallbackTexture(path);
        }
    }

    private Sound loadSound(String path) {
        try { return Gdx.audio.newSound(Gdx.files.internal(path)); }
        catch (Throwable t) { Gdx.app.error("AdventureGame", "Sound load failed: " + path, t); return null; }
    }

    private Texture fallbackTexture(String name) {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        Color base = name.contains("grass") ? new Color(0.22f, 0.55f, 0.20f, 1f)
                : name.contains("stone") ? new Color(0.45f, 0.45f, 0.43f, 1f)
                : name.contains("wood") || name.contains("leather") ? new Color(0.43f, 0.25f, 0.13f, 1f)
                : name.contains("skin") ? new Color(0.78f, 0.52f, 0.36f, 1f)
                : name.contains("hair") ? new Color(0.16f, 0.10f, 0.06f, 1f)
                : new Color(0.15f, 0.32f, 0.65f, 1f);
        pixmap.setColor(base); pixmap.fill(); pixmap.setColor(base.cpy().mul(1.25f));
        for (int y = 0; y < 64; y += 16) for (int x = 0; x < 64; x += 16) if (((x + y) / 16) % 2 == 0) pixmap.fillRectangle(x, y, 16, 16);
        Texture texture = new Texture(pixmap); pixmap.dispose(); return texture;
    }

    private Material mat(Texture texture) {
        return new Material(TextureAttribute.createDiffuse(texture), ColorAttribute.createSpecular(0.22f, 0.22f, 0.22f, 1f), FloatAttribute.createShininess(8f));
    }

    private Material colorMat(Color color) { return new Material(ColorAttribute.createDiffuse(color)); }

    private void buildWorld() {
        Model ground = modelBuilder.createBox(92f, 0.14f, 92f, mat(grass), ATTRS); models.add(ground); world.add(new ModelInstance(ground, 0f, -0.08f, 0f));
        Model water = modelBuilder.createBox(92f, 0.04f, 16f, colorMat(new Color(0.12f, 0.44f, 0.72f, 0.78f)), ATTRS); models.add(water); world.add(new ModelInstance(water, 0f, -0.03f, 36f));

        Model pathTile = modelBuilder.createBox(2.25f, 0.075f, 2.25f, mat(stone), ATTRS); models.add(pathTile);
        for (int i = -14; i <= 14; i++) {
            ModelInstance tile = new ModelInstance(pathTile);
            tile.transform.setToTranslation(i * 2.05f, 0.025f, MathUtils.sin(i * 0.62f) * 1.55f - 5f).rotate(Vector3.Y, MathUtils.random(-12f, 12f));
            world.add(tile);
        }

        Model trunk = modelBuilder.createCylinder(0.48f, 4.7f, 0.48f, 14, mat(wood), ATTRS);
        Model crownA = modelBuilder.createSphere(3.0f, 2.6f, 3.0f, 16, 12, mat(leaves), ATTRS);
        Model crownB = modelBuilder.createSphere(2.1f, 1.9f, 2.1f, 14, 10, mat(leaves), ATTRS);
        Model rock = modelBuilder.createSphere(1.35f, 0.82f, 1.2f, 14, 10, mat(stone), ATTRS);
        Model flower = modelBuilder.createSphere(0.18f, 0.18f, 0.18f, 8, 6, colorMat(new Color(1f, 0.85f, 0.25f, 1f)), ATTRS);
        models.add(trunk); models.add(crownA); models.add(crownB); models.add(rock); models.add(flower);

        float[][] trees = {{-11, 7}, {-17, -4}, {-7, -15}, {8, 10}, {16, 4}, {13, -12}, {-22, 16}, {23, -18}, {-31, -22}, {30, 18}, {-33, 6}, {4, -28}};
        for (float[] p : trees) {
            world.add(new ModelInstance(trunk, p[0], 2.25f, p[1]));
            ModelInstance c1 = new ModelInstance(crownA, p[0], 5.0f, p[1]); c1.transform.rotate(Vector3.Y, MathUtils.random(0, 360)); world.add(c1);
            world.add(new ModelInstance(crownB, p[0] + 0.75f, 6.05f, p[1] - 0.35f));
        }
        for (int i = 0; i < 24; i++) {
            ModelInstance r = new ModelInstance(rock);
            r.transform.setToTranslation(MathUtils.random(-38f, 38f), 0.28f, MathUtils.random(-34f, 30f)).rotate(Vector3.Y, MathUtils.random(360f)).scale(MathUtils.random(0.55f, 1.55f), MathUtils.random(0.45f, 1.05f), MathUtils.random(0.55f, 1.45f));
            world.add(r);
        }
        for (int i = 0; i < 42; i++) world.add(new ModelInstance(flower, MathUtils.random(-26f, 26f), 0.11f, MathUtils.random(-22f, 22f)));
    }

    private void buildHero() {
        addHeroPart("torso", box(0.92f, 1.25f, 0.45f, cloth), 0f, 1.62f, 0f, 0f);
        addHeroPart("chest", sphere(1.02f, 0.82f, 0.50f, cloth, 24, 14), 0f, 1.95f, 0.02f, 0f);
        addHeroPart("pelvis", box(0.82f, 0.42f, 0.45f, leather), 0f, 0.93f, 0f, 0f);
        addHeroPart("neck", cylinder(0.24f, 0.28f, skin, 18), 0f, 2.48f, 0f, 0f);
        addHeroPart("head", sphere(0.68f, 0.76f, 0.64f, skin, 28, 18), 0f, 2.86f, 0f, 0f);
        addHeroPart("hair", sphere(0.73f, 0.38f, 0.70f, hair, 28, 12), 0f, 3.22f, -0.04f, 0f);
        addHeroPart("eyeL", sphere(0.075f, 0.055f, 0.035f, metal, 8, 6), -0.18f, 2.93f, 0.32f, 0f);
        addHeroPart("eyeR", sphere(0.075f, 0.055f, 0.035f, metal, 8, 6), 0.18f, 2.93f, 0.32f, 0f);
        addHeroPart("belt", box(1.0f, 0.13f, 0.52f, leather), 0f, 1.19f, 0.01f, 0f);
        addHeroPart("backpack", box(0.72f, 0.95f, 0.32f, leather), 0f, 1.75f, -0.42f, 0f);
        addHeroPart("shoulderL", sphere(0.34f, 0.34f, 0.34f, cloth, 16, 10), -0.68f, 2.08f, 0f, 1f);
        addHeroPart("shoulderR", sphere(0.34f, 0.34f, 0.34f, cloth, 16, 10), 0.68f, 2.08f, 0f, -1f);
        addHeroPart("upperArmL", cylinder(0.17f, 0.78f, skin, 16), -0.82f, 1.58f, 0f, 1f);
        addHeroPart("upperArmR", cylinder(0.17f, 0.78f, skin, 16), 0.82f, 1.58f, 0f, -1f);
        addHeroPart("foreArmL", cylinder(0.15f, 0.68f, skin, 16), -0.84f, 1.02f, 0f, 1f);
        addHeroPart("foreArmR", cylinder(0.15f, 0.68f, skin, 16), 0.84f, 1.02f, 0f, -1f);
        addHeroPart("handL", sphere(0.27f, 0.27f, 0.27f, skin, 14, 10), -0.84f, 0.62f, 0f, 1f);
        addHeroPart("handR", sphere(0.27f, 0.27f, 0.27f, skin, 14, 10), 0.84f, 0.62f, 0f, -1f);
        addHeroPart("thighL", cylinder(0.22f, 0.82f, leather, 16), -0.28f, 0.62f, 0f, -1f);
        addHeroPart("thighR", cylinder(0.22f, 0.82f, leather, 16), 0.28f, 0.62f, 0f, 1f);
        addHeroPart("shinL", cylinder(0.18f, 0.72f, leather, 16), -0.29f, 0.08f, 0f, -1f);
        addHeroPart("shinR", cylinder(0.18f, 0.72f, leather, 16), 0.29f, 0.08f, 0f, 1f);
        addHeroPart("bootL", box(0.42f, 0.22f, 0.68f, leather), -0.29f, -0.36f, 0.14f, -1f);
        addHeroPart("bootR", box(0.42f, 0.22f, 0.68f, leather), 0.29f, -0.36f, 0.14f, 1f);
    }

    private Model box(float w, float h, float d, Texture t) { Model m = modelBuilder.createBox(w, h, d, mat(t), ATTRS); models.add(m); return m; }
    private Model sphere(float w, float h, float d, Texture t, int us, int vs) { Model m = modelBuilder.createSphere(w, h, d, us, vs, mat(t), ATTRS); models.add(m); return m; }
    private Model cylinder(float radius, float height, Texture t, int div) { Model m = modelBuilder.createCylinder(radius, height, radius, div, mat(t), ATTRS); models.add(m); return m; }
    private void addHeroPart(String name, Model model, float x, float y, float z, float side) { hero.add(new HeroPart(name, new ModelInstance(model), new Vector3(x, y, z), side)); }

    private void setupInput() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (screenX < Gdx.graphics.getWidth() * 0.48f && stickPointer == -1) {
                    stickPointer = pointer; stickOrigin.set(screenX, screenY); stickCurrent.set(screenX, screenY); return true;
                }
                lookPointer = pointer; lastLookX = screenX; lastLookY = screenY; return true;
            }
            @Override public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (pointer == stickPointer) {
                    stickCurrent.set(screenX, screenY); moveStick.set(stickCurrent).sub(stickOrigin).scl(1f / (120f * hudScale)); moveStick.y = -moveStick.y; if (moveStick.len() > 1f) moveStick.nor(); return true;
                }
                if (pointer == lookPointer) {
                    cameraYaw -= (screenX - lastLookX) * 0.18f; cameraPitch += (screenY - lastLookY) * 0.13f; cameraPitch = MathUtils.clamp(cameraPitch, 10f, 47f); lastLookX = screenX; lastLookY = screenY; return true;
                }
                return false;
            }
            @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (pointer == stickPointer) { stickPointer = -1; moveStick.setZero(); return true; }
                if (pointer == lookPointer) { lookPointer = -1; return true; }
                return false;
            }
        });
    }

    @Override public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        update(dt);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.42f, 0.62f, 0.88f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(camera);
        for (ModelInstance instance : world) modelBatch.render(instance, environment);
        for (HeroPart part : hero) modelBatch.render(part.instance, environment);
        modelBatch.end();
        drawHud();
    }

    private void update(float dt) {
        camForward.set(MathUtils.sinDeg(cameraYaw), 0f, MathUtils.cosDeg(cameraYaw)).nor();
        camRight.set(camForward.z, 0f, -camForward.x).nor();
        moveVec.set(camRight).scl(moveStick.x).mulAdd(camForward, moveStick.y);
        float speed = moveStick.len();
        if (moveVec.len2() > 0.001f) {
            moveVec.nor(); velocity.set(moveVec).scl(5.6f); heroYaw = MathUtils.atan2(moveVec.x, moveVec.z) * MathUtils.radiansToDegrees; walkTime += dt * (8.8f + speed * 4f);
            int stepIndex = (int)(walkTime / MathUtils.PI);
            if (stepIndex != lastStepIndex) { lastStepIndex = stepIndex; if (footstepSound != null) footstepSound.play(0.28f, MathUtils.random(0.88f, 1.12f), 0f); }
        } else { velocity.scl((float)Math.pow(0.035f, dt)); walkTime += dt * 1.7f; }
        heroPos.mulAdd(velocity, dt); heroPos.x = MathUtils.clamp(heroPos.x, -41f, 41f); heroPos.z = MathUtils.clamp(heroPos.z, -39f, 35f);
        updateHeroParts(speed); updateCamera();
    }

    private void updateHeroParts(float speed) {
        float swing = MathUtils.sin(walkTime) * 30f * speed;
        float bob = Math.abs(MathUtils.sin(walkTime)) * 0.085f * speed;
        float breath = MathUtils.sin(walkTime * 0.35f) * 0.015f;
        for (HeroPart p : hero) {
            float rx = 0f, rz = 0f, extraY = breath;
            if (p.name.contains("Arm") || p.name.contains("hand")) rx = -swing * p.side;
            if (p.name.contains("foreArm")) rx *= 0.72f;
            if (p.name.contains("thigh") || p.name.contains("shin") || p.name.contains("boot")) rx = swing * p.side * 0.72f;
            if (p.name.contains("shoulder")) rz = p.side * (4f + speed * 3f);
            if (p.name.equals("head") || p.name.equals("hair") || p.name.startsWith("eye")) extraY = breath * 2f;
            p.instance.transform.idt().translate(heroPos.x, heroPos.y + bob + extraY, heroPos.z).rotate(Vector3.Y, heroYaw).translate(p.offset).rotate(Vector3.Z, rz).rotate(Vector3.X, rx);
        }
    }

    private void updateCamera() {
        float dist = 8.2f; float y = MathUtils.sinDeg(cameraPitch) * dist + 2.85f; float flat = MathUtils.cosDeg(cameraPitch) * dist;
        desiredCam.set(heroPos.x - MathUtils.sinDeg(cameraYaw) * flat, heroPos.y + y, heroPos.z - MathUtils.cosDeg(cameraYaw) * flat);
        camera.position.lerp(desiredCam, 0.14f); camera.lookAt(heroPos.x, heroPos.y + 1.72f, heroPos.z); camera.up.set(Vector3.Y); camera.update();
    }

    private void drawHud() {
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float radius = 96f * hudScale, knobRadius = 43f * hudScale;
        float baseX = stickPointer == -1 ? 155f * hudScale : stickOrigin.x;
        float baseY = stickPointer == -1 ? h - 155f * hudScale : stickOrigin.y;
        float knobX = stickPointer == -1 ? baseX : stickCurrent.x;
        float knobY = stickPointer == -1 ? baseY : stickCurrent.y;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.30f); shapes.circle(baseX, h - baseY, radius + 8f * hudScale);
        shapes.setColor(0.07f, 0.17f, 0.28f, 0.42f); shapes.circle(baseX, h - baseY, radius);
        shapes.setColor(0.22f, 0.55f, 1f, 0.72f); shapes.circle(knobX, h - knobY, knobRadius);
        shapes.setColor(1f, 1f, 1f, 0.18f); shapes.circle(knobX - knobRadius * 0.25f, h - knobY + knobRadius * 0.25f, knobRadius * 0.32f);
        shapes.setColor(0f, 0f, 0f, 0.26f); shapes.rect(w - 270f * hudScale, h - 86f * hudScale, 250f * hudScale, 60f * hudScale);
        shapes.end();
        sprites.begin();
        font.draw(sprites, "3D Adventure", 20f, h - 20f);
        font.draw(sprites, "FPS " + Gdx.graphics.getFramesPerSecond(), w - 250f * hudScale, h - 48f * hudScale);
        font.draw(sprites, "Move", baseX - 31f * hudScale, h - baseY - radius - 18f * hudScale);
        sprites.end();
    }

    @Override public void resize(int width, int height) { hudScale = MathUtils.clamp(Math.min(width, height) / 720f, 0.85f, 1.35f); camera.viewportWidth = width; camera.viewportHeight = height; camera.update(); }

    @Override public void pause() { if (windSound != null && windId != -1) windSound.pause(windId); }
    @Override public void resume() { if (windSound != null && windId != -1) windSound.resume(windId); }

    @Override public void dispose() {
        modelBatch.dispose(); shapes.dispose(); sprites.dispose(); font.dispose();
        if (footstepSound != null) footstepSound.dispose(); if (chimeSound != null) chimeSound.dispose(); if (windSound != null) windSound.dispose();
        Texture[] textures = {grass, stone, wood, leaves, cloth, skin, leather, hair, metal}; for (Texture t : textures) if (t != null) t.dispose();
        for (Model model : models) model.dispose();
    }

    private static class HeroPart {
        final String name; final ModelInstance instance; final Vector3 offset; final float side;
        HeroPart(String name, ModelInstance instance, Vector3 offset, float side) { this.name = name; this.instance = instance; this.offset = offset; this.side = side; }
    }
}
