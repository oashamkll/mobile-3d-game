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

/**
 * Optimized third-person 3D prototype.
 * Uses pooled procedural chunks so the world never ends, while keeping a small
 * constant number of rendered objects for stable FPS on phones.
 */
public class AdventureGame extends ApplicationAdapter {
    private static final long ATTRS = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;
    private static final float CHUNK_SIZE = 26f;
    private static final int CHUNK_RADIUS = 2;
    private static final int CHUNK_COUNT = (CHUNK_RADIUS * 2 + 1) * (CHUNK_RADIUS * 2 + 1);

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private ShapeRenderer shapes;
    private SpriteBatch sprites;
    private BitmapFont font;
    private ModelBuilder modelBuilder;

    private final Array<Model> models = new Array<>();
    private final Array<Chunk> chunks = new Array<>();
    private final Array<HeroPart> hero = new Array<>();

    private Texture groundTex, stoneTex, barkTex, leavesTex, clothTex, skinTex, leatherTex, hairTex, metalTex;
    private Sound footstepSound, chimeSound, windSound;
    private long windId = -1;

    private Model groundModel, pathModel, trunkModel, crownModel, crownSmallModel, rockModel, grassClumpModel;

    private final Vector3 heroPos = new Vector3(0f, 0.2f, 0f);
    private final Vector3 velocity = new Vector3();
    private final Vector3 camForward = new Vector3();
    private final Vector3 camRight = new Vector3();
    private final Vector3 moveVec = new Vector3();
    private final Vector3 desiredCam = new Vector3();
    private float heroYaw = 0f;
    private float cameraYaw = 30f;
    private float cameraPitch = 24f;
    private float walkTime = 0f;
    private int lastStepIndex = -1;
    private int centerChunkX = Integer.MIN_VALUE;
    private int centerChunkZ = Integer.MIN_VALUE;

    private final Vector2 moveStick = new Vector2();
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
        font.getData().setScale(1.05f);
        font.setColor(new Color(0.88f, 0.90f, 0.86f, 1f));

        groundTex = tex("textures/environment/grass_albedo.png");
        stoneTex = tex("textures/environment/stone_albedo.png");
        barkTex = tex("textures/environment/wood_albedo.png");
        leavesTex = tex("textures/environment/leaves_albedo.png");
        clothTex = tex("textures/character/hero_cloth_albedo.png");
        skinTex = tex("textures/character/hero_skin_albedo.png");
        leatherTex = tex("textures/character/hero_leather_albedo.png");
        hairTex = tex("textures/character/hero_hair_albedo.png");
        metalTex = tex("textures/character/hero_metal_albedo.png");

        footstepSound = loadSound("audio/footstep.wav");
        chimeSound = loadSound("audio/chime.wav");
        windSound = loadSound("audio/wind_loop.wav");
        if (windSound != null) windId = windSound.loop(0.12f);
        if (chimeSound != null) chimeSound.play(0.28f);

        camera = new PerspectiveCamera(64f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.12f;
        camera.far = 105f;

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.42f, 0.45f, 0.48f, 1f));
        environment.add(new DirectionalLight().set(0.95f, 0.86f, 0.68f, -0.45f, -0.88f, -0.25f));
        environment.add(new DirectionalLight().set(0.20f, 0.26f, 0.33f, 0.55f, -0.18f, 0.45f));

        buildReusableWorldModels();
        buildChunks();
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
        Color base = name.contains("grass") ? new Color(0.18f, 0.28f, 0.18f, 1f)
                : name.contains("stone") ? new Color(0.34f, 0.34f, 0.32f, 1f)
                : name.contains("wood") || name.contains("leather") ? new Color(0.30f, 0.20f, 0.13f, 1f)
                : name.contains("skin") ? new Color(0.62f, 0.42f, 0.32f, 1f)
                : name.contains("hair") ? new Color(0.10f, 0.07f, 0.05f, 1f)
                : new Color(0.12f, 0.18f, 0.23f, 1f);
        pixmap.setColor(base); pixmap.fill(); pixmap.setColor(base.cpy().mul(1.18f));
        for (int y = 0; y < 64; y += 16) for (int x = 0; x < 64; x += 16) if (((x + y) / 16) % 2 == 0) pixmap.fillRectangle(x, y, 16, 16);
        Texture texture = new Texture(pixmap); pixmap.dispose(); return texture;
    }

    private Material mat(Texture texture, float shininess) {
        return new Material(TextureAttribute.createDiffuse(texture), ColorAttribute.createSpecular(0.08f, 0.08f, 0.075f, 1f), FloatAttribute.createShininess(shininess));
    }

    private Material colorMat(Color color) { return new Material(ColorAttribute.createDiffuse(color)); }

    private void buildReusableWorldModels() {
        groundModel = add(modelBuilder.createBox(CHUNK_SIZE + 0.12f, 0.10f, CHUNK_SIZE + 0.12f, mat(groundTex, 3f), ATTRS));
        pathModel = add(modelBuilder.createBox(3.1f, 0.075f, 3.1f, mat(stoneTex, 6f), ATTRS));
        trunkModel = add(modelBuilder.createCylinder(0.42f, 4.1f, 0.42f, 12, mat(barkTex, 4f), ATTRS));
        crownModel = add(modelBuilder.createSphere(2.45f, 2.2f, 2.45f, 14, 10, mat(leavesTex, 3f), ATTRS));
        crownSmallModel = add(modelBuilder.createSphere(1.75f, 1.5f, 1.75f, 12, 8, mat(leavesTex, 3f), ATTRS));
        rockModel = add(modelBuilder.createSphere(1.15f, 0.72f, 1.0f, 12, 8, mat(stoneTex, 5f), ATTRS));
        grassClumpModel = add(modelBuilder.createCylinder(0.10f, 0.75f, 0.10f, 5, colorMat(new Color(0.26f, 0.34f, 0.21f, 1f)), ATTRS));
    }

    private Model add(Model model) { models.add(model); return model; }

    private void buildChunks() {
        for (int i = 0; i < CHUNK_COUNT; i++) chunks.add(new Chunk());
        refreshChunks(true);
    }

    private void refreshChunks(boolean force) {
        int cx = fastFloor(heroPos.x / CHUNK_SIZE);
        int cz = fastFloor(heroPos.z / CHUNK_SIZE);
        if (!force && cx == centerChunkX && cz == centerChunkZ) return;
        centerChunkX = cx; centerChunkZ = cz;
        int idx = 0;
        for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                positionChunk(chunks.get(idx++), cx + dx, cz + dz);
            }
        }
    }

    private int fastFloor(float v) { int i = (int)v; return v < i ? i - 1 : i; }

    private void positionChunk(Chunk c, int cx, int cz) {
        float baseX = cx * CHUNK_SIZE;
        float baseZ = cz * CHUNK_SIZE;
        c.ground.transform.idt().translate(baseX, -0.055f, baseZ);

        float pathOffset = signedRand(cx, cz, 100) * 2.8f;
        c.path.transform.idt().translate(baseX + pathOffset, 0.018f, baseZ + signedRand(cx, cz, 101) * 4.0f).rotate(Vector3.Y, signedRand(cx, cz, 102) * 11f).scale(1.9f, 1f, 1.1f);

        for (int i = 0; i < c.trees.length; i++) {
            float x = baseX + signedRand(cx, cz, 10 + i * 3) * 10.5f;
            float z = baseZ + signedRand(cx, cz, 11 + i * 3) * 10.5f;
            float scale = 0.80f + rand01(cx, cz, 12 + i * 3) * 0.48f;
            float rot = rand01(cx, cz, 13 + i * 3) * 360f;
            c.trees[i].trunk.transform.idt().translate(x, 1.95f * scale, z).rotate(Vector3.Y, rot).scale(scale, scale, scale);
            c.trees[i].crown.transform.idt().translate(x, 4.55f * scale, z).rotate(Vector3.Y, rot).scale(scale, scale, scale);
            c.trees[i].crownSmall.transform.idt().translate(x + 0.65f * scale, 5.35f * scale, z - 0.45f * scale).rotate(Vector3.Y, rot * 0.7f).scale(scale, scale, scale);
        }
        for (int i = 0; i < c.rocks.length; i++) {
            float x = baseX + signedRand(cx, cz, 40 + i * 4) * 11.0f;
            float z = baseZ + signedRand(cx, cz, 41 + i * 4) * 11.0f;
            float sx = 0.55f + rand01(cx, cz, 42 + i * 4) * 1.15f;
            float sy = 0.42f + rand01(cx, cz, 43 + i * 4) * 0.65f;
            float sz = 0.55f + rand01(cx, cz, 44 + i * 4) * 1.05f;
            c.rocks[i].transform.idt().translate(x, 0.22f, z).rotate(Vector3.Y, rand01(cx, cz, 45 + i * 4) * 360f).scale(sx, sy, sz);
        }
        for (int i = 0; i < c.grass.length; i++) {
            float x = baseX + signedRand(cx, cz, 70 + i * 2) * 11.8f;
            float z = baseZ + signedRand(cx, cz, 71 + i * 2) * 11.8f;
            c.grass[i].transform.idt().translate(x, 0.25f, z).rotate(Vector3.Y, rand01(cx, cz, 72 + i) * 360f).rotate(Vector3.X, signedRand(cx, cz, 73 + i) * 15f).scale(0.85f, 0.85f + rand01(cx, cz, 74 + i) * 0.85f, 0.85f);
        }
    }

    private float rand01(int x, int z, int salt) {
        int h = x * 73428767 ^ z * 912931 ^ salt * 19349663;
        h ^= (h >>> 13); h *= 1274126177; h ^= (h >>> 16);
        return (h & 0x7fffffff) / (float)0x7fffffff;
    }
    private float signedRand(int x, int z, int salt) { return rand01(x, z, salt) * 2f - 1f; }

    private void buildHero() {
        addHeroPart("torso", box(0.78f, 1.18f, 0.36f, clothTex), 0f, 1.63f, 0f, 0f);
        addHeroPart("chest", sphere(0.92f, 0.72f, 0.43f, clothTex, 20, 12), 0f, 1.98f, 0.02f, 0f);
        addHeroPart("pelvis", box(0.72f, 0.36f, 0.38f, leatherTex), 0f, 0.98f, 0f, 0f);
        addHeroPart("neck", cylinder(0.18f, 0.23f, skinTex, 14), 0f, 2.42f, 0f, 0f);
        addHeroPart("head", sphere(0.54f, 0.64f, 0.52f, skinTex, 22, 14), 0f, 2.78f, 0.02f, 0f);
        addHeroPart("hair", sphere(0.58f, 0.26f, 0.54f, hairTex, 20, 10), 0f, 3.10f, -0.04f, 0f);
        addHeroPart("eyeL", sphere(0.052f, 0.040f, 0.026f, metalTex, 8, 6), -0.14f, 2.83f, 0.276f, 0f);
        addHeroPart("eyeR", sphere(0.052f, 0.040f, 0.026f, metalTex, 8, 6), 0.14f, 2.83f, 0.276f, 0f);
        addHeroPart("belt", box(0.86f, 0.10f, 0.42f, leatherTex), 0f, 1.21f, 0.02f, 0f);
        addHeroPart("pack", box(0.58f, 0.82f, 0.24f, leatherTex), 0f, 1.72f, -0.36f, 0f);
        addHeroPart("shoulderL", sphere(0.26f, 0.26f, 0.26f, clothTex, 12, 8), -0.57f, 2.06f, 0f, 1f);
        addHeroPart("shoulderR", sphere(0.26f, 0.26f, 0.26f, clothTex, 12, 8), 0.57f, 2.06f, 0f, -1f);
        addHeroPart("upperArmL", cylinder(0.13f, 0.64f, clothTex, 12), -0.70f, 1.58f, 0f, 1f);
        addHeroPart("upperArmR", cylinder(0.13f, 0.64f, clothTex, 12), 0.70f, 1.58f, 0f, -1f);
        addHeroPart("foreArmL", cylinder(0.12f, 0.58f, skinTex, 12), -0.72f, 1.06f, 0f, 1f);
        addHeroPart("foreArmR", cylinder(0.12f, 0.58f, skinTex, 12), 0.72f, 1.06f, 0f, -1f);
        addHeroPart("handL", sphere(0.20f, 0.20f, 0.20f, skinTex, 10, 8), -0.72f, 0.72f, 0f, 1f);
        addHeroPart("handR", sphere(0.20f, 0.20f, 0.20f, skinTex, 10, 8), 0.72f, 0.72f, 0f, -1f);
        addHeroPart("thighL", cylinder(0.17f, 0.72f, leatherTex, 12), -0.22f, 0.62f, 0f, -1f);
        addHeroPart("thighR", cylinder(0.17f, 0.72f, leatherTex, 12), 0.22f, 0.62f, 0f, 1f);
        addHeroPart("shinL", cylinder(0.145f, 0.64f, leatherTex, 12), -0.23f, 0.10f, 0f, -1f);
        addHeroPart("shinR", cylinder(0.145f, 0.64f, leatherTex, 12), 0.23f, 0.10f, 0f, 1f);
        addHeroPart("bootL", box(0.34f, 0.18f, 0.55f, leatherTex), -0.23f, -0.28f, 0.11f, -1f);
        addHeroPart("bootR", box(0.34f, 0.18f, 0.55f, leatherTex), 0.23f, -0.28f, 0.11f, 1f);
    }

    private Model box(float w, float h, float d, Texture t) { return add(modelBuilder.createBox(w, h, d, mat(t, 5f), ATTRS)); }
    private Model sphere(float w, float h, float d, Texture t, int us, int vs) { return add(modelBuilder.createSphere(w, h, d, us, vs, mat(t, 5f), ATTRS)); }
    private Model cylinder(float radius, float height, Texture t, int div) { return add(modelBuilder.createCylinder(radius, height, radius, div, mat(t, 4f), ATTRS)); }
    private void addHeroPart(String name, Model model, float x, float y, float z, float side) { hero.add(new HeroPart(name, new ModelInstance(model), new Vector3(x, y, z), side)); }

    private void setupInput() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (screenX < Gdx.graphics.getWidth() * 0.48f && stickPointer == -1) { stickPointer = pointer; updateJoystick(screenX, screenY); return true; }
                lookPointer = pointer; lastLookX = screenX; lastLookY = screenY; return true;
            }
            @Override public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (pointer == stickPointer) { updateJoystick(screenX, screenY); return true; }
                if (pointer == lookPointer) { cameraYaw -= (screenX - lastLookX) * 0.16f; cameraPitch += (screenY - lastLookY) * 0.11f; cameraPitch = MathUtils.clamp(cameraPitch, 11f, 46f); lastLookX = screenX; lastLookY = screenY; return true; }
                return false;
            }
            @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (pointer == stickPointer) { stickPointer = -1; moveStick.setZero(); stickCurrent.set(fixedStickX(), fixedStickY()); return true; }
                if (pointer == lookPointer) { lookPointer = -1; return true; }
                return false;
            }
        });
    }

    private float fixedStickX() { return 182f * hudScale; }
    private float fixedStickY() { return Gdx.graphics.getHeight() - 182f * hudScale; }

    private void updateJoystick(float screenX, float screenY) {
        float baseX = fixedStickX();
        float baseY = fixedStickY();
        float radius = 118f * hudScale;
        moveStick.set(screenX - baseX, -(screenY - baseY));
        if (moveStick.len() > radius) moveStick.setLength(radius);
        stickCurrent.set(baseX + moveStick.x, baseY - moveStick.y);
        moveStick.scl(1f / radius);
        if (moveStick.len() < 0.08f) moveStick.setZero();
    }

    @Override public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        update(dt);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClearColor(0.34f, 0.38f, 0.41f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(camera);
        for (Chunk c : chunks) c.render(modelBatch, environment);
        for (HeroPart part : hero) modelBatch.render(part.instance, environment);
        modelBatch.end();
        drawHud();
    }

    private void update(float dt) {
        refreshChunks(false);
        camForward.set(MathUtils.sinDeg(cameraYaw), 0f, MathUtils.cosDeg(cameraYaw)).nor();
        camRight.set(camForward.z, 0f, -camForward.x).nor();
        moveVec.set(camRight).scl(moveStick.x).mulAdd(camForward, moveStick.y);
        float speed = moveStick.len();
        if (moveVec.len2() > 0.001f) {
            moveVec.nor(); velocity.set(moveVec).scl(6.0f); heroYaw = MathUtils.atan2(moveVec.x, moveVec.z) * MathUtils.radiansToDegrees; walkTime += dt * (8.7f + speed * 4.2f);
            int stepIndex = (int)(walkTime / MathUtils.PI);
            if (stepIndex != lastStepIndex) { lastStepIndex = stepIndex; if (footstepSound != null) footstepSound.play(0.22f, MathUtils.random(0.88f, 1.10f), 0f); }
        } else { velocity.scl((float)Math.pow(0.030f, dt)); walkTime += dt * 1.5f; }
        heroPos.mulAdd(velocity, dt);
        updateHeroParts(speed); updateCamera();
    }

    private void updateHeroParts(float speed) {
        float swing = MathUtils.sin(walkTime) * 31f * speed;
        float bob = Math.abs(MathUtils.sin(walkTime)) * 0.072f * speed;
        float breath = MathUtils.sin(walkTime * 0.32f) * 0.012f;
        for (HeroPart p : hero) {
            float rx = 0f, rz = 0f, extraY = breath;
            if (p.name.contains("Arm") || p.name.contains("hand")) rx = -swing * p.side;
            if (p.name.contains("foreArm")) rx *= 0.72f;
            if (p.name.contains("thigh") || p.name.contains("shin") || p.name.contains("boot")) rx = swing * p.side * 0.74f;
            if (p.name.contains("shoulder")) rz = p.side * (3f + speed * 3f);
            if (p.name.equals("head") || p.name.equals("hair") || p.name.startsWith("eye")) extraY = breath * 1.8f;
            p.instance.transform.idt().translate(heroPos.x, heroPos.y + bob + extraY, heroPos.z).rotate(Vector3.Y, heroYaw).translate(p.offset).rotate(Vector3.Z, rz).rotate(Vector3.X, rx);
        }
    }

    private void updateCamera() {
        float dist = 8.6f; float y = MathUtils.sinDeg(cameraPitch) * dist + 2.75f; float flat = MathUtils.cosDeg(cameraPitch) * dist;
        desiredCam.set(heroPos.x - MathUtils.sinDeg(cameraYaw) * flat, heroPos.y + y, heroPos.z - MathUtils.cosDeg(cameraYaw) * flat);
        camera.position.lerp(desiredCam, 0.16f); camera.lookAt(heroPos.x, heroPos.y + 1.65f, heroPos.z); camera.up.set(Vector3.Y); camera.update();
    }

    private void drawHud() {
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float radius = 100f * hudScale, knobRadius = 42f * hudScale;
        float baseX = fixedStickX(), baseY = fixedStickY();
        float knobX = stickPointer == -1 ? baseX : stickCurrent.x;
        float knobY = stickPointer == -1 ? baseY : stickCurrent.y;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.015f, 0.018f, 0.018f, 0.48f); shapes.circle(baseX, h - baseY, radius + 11f * hudScale);
        shapes.setColor(0.11f, 0.13f, 0.12f, 0.56f); shapes.circle(baseX, h - baseY, radius);
        shapes.setColor(0.40f, 0.44f, 0.40f, 0.82f); shapes.circle(knobX, h - knobY, knobRadius);
        shapes.setColor(0.86f, 0.88f, 0.80f, 0.18f); shapes.circle(knobX - knobRadius * 0.22f, h - knobY + knobRadius * 0.22f, knobRadius * 0.30f);
        shapes.setColor(0.01f, 0.012f, 0.012f, 0.35f); shapes.rect(w - 235f * hudScale, h - 74f * hudScale, 215f * hudScale, 52f * hudScale);
        shapes.end();
        sprites.begin();
        font.draw(sprites, "WILDLAND", 20f, h - 20f);
        font.draw(sprites, "FPS " + Gdx.graphics.getFramesPerSecond(), w - 218f * hudScale, h - 43f * hudScale);
        font.draw(sprites, "MOVE", baseX - 27f * hudScale, h - baseY - radius - 16f * hudScale);
        sprites.end();
    }

    @Override public void resize(int width, int height) {
        hudScale = MathUtils.clamp(Math.min(width, height) / 720f, 0.85f, 1.35f);
        stickCurrent.set(fixedStickX(), fixedStickY());
        camera.viewportWidth = width; camera.viewportHeight = height; camera.update();
    }

    @Override public void pause() { if (windSound != null && windId != -1) windSound.pause(windId); }
    @Override public void resume() { if (windSound != null && windId != -1) windSound.resume(windId); }

    @Override public void dispose() {
        modelBatch.dispose(); shapes.dispose(); sprites.dispose(); font.dispose();
        if (footstepSound != null) footstepSound.dispose(); if (chimeSound != null) chimeSound.dispose(); if (windSound != null) windSound.dispose();
        Texture[] textures = {groundTex, stoneTex, barkTex, leavesTex, clothTex, skinTex, leatherTex, hairTex, metalTex}; for (Texture t : textures) if (t != null) t.dispose();
        for (Model model : models) model.dispose();
    }

    private class Chunk {
        final ModelInstance ground = new ModelInstance(groundModel);
        final ModelInstance path = new ModelInstance(pathModel);
        final Tree[] trees = new Tree[2];
        final ModelInstance[] rocks = new ModelInstance[3];
        final ModelInstance[] grass = new ModelInstance[5];
        Chunk() { for (int i = 0; i < trees.length; i++) trees[i] = new Tree(); for (int i = 0; i < rocks.length; i++) rocks[i] = new ModelInstance(rockModel); for (int i = 0; i < grass.length; i++) grass[i] = new ModelInstance(grassClumpModel); }
        void render(ModelBatch batch, Environment env) { batch.render(ground, env); batch.render(path, env); for (Tree t : trees) t.render(batch, env); for (ModelInstance r : rocks) batch.render(r, env); for (ModelInstance g : grass) batch.render(g, env); }
    }
    private class Tree { final ModelInstance trunk = new ModelInstance(trunkModel); final ModelInstance crown = new ModelInstance(crownModel); final ModelInstance crownSmall = new ModelInstance(crownSmallModel); void render(ModelBatch b, Environment e) { b.render(trunk, e); b.render(crown, e); b.render(crownSmall, e); } }
    private static class HeroPart { final String name; final ModelInstance instance; final Vector3 offset; final float side; HeroPart(String name, ModelInstance instance, Vector3 offset, float side) { this.name = name; this.instance = instance; this.offset = offset; this.side = side; } }
}
