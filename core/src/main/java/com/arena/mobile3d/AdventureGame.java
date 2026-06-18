package com.arena.mobile3d;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
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
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Mobile 3D Adventure prototype.
 *
 * Built as a pure Java/Kotlin Android + LibGDX project. The 3D hero is assembled
 * from textured primitive body parts, using the generated texture pack in
 * android/assets/textures. This keeps the project source-editable while still
 * producing a complete third-person 3D mobile prototype.
 */
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

    private Texture grass, stone, wood, cloth, skin, leather, hair;
    private final Vector3 heroPos = new Vector3(0f, 0.1f, 0f);
    private final Vector3 velocity = new Vector3();
    private float heroYaw = 0f;
    private float cameraYaw = 35f;
    private float cameraPitch = 22f;
    private float walkTime = 0f;

    private final Vector2 moveStick = new Vector2();
    private final Vector2 stickOrigin = new Vector2();
    private final Vector2 stickCurrent = new Vector2();
    private int stickPointer = -1;
    private int lookPointer = -1;
    private float lastLookX, lastLookY;

    @Override
    public void create() {
        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        shapes = new ShapeRenderer();
        sprites = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        grass = tex("textures/environment/grass_albedo.png");
        stone = tex("textures/environment/stone_albedo.png");
        wood = tex("textures/environment/wood_albedo.png");
        cloth = tex("textures/character/hero_cloth_albedo.png");
        skin = tex("textures/character/hero_skin_albedo.png");
        leather = tex("textures/character/hero_leather_albedo.png");
        hair = tex("textures/character/hero_hair_albedo.png");

        camera = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 140f;

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.72f, 0.76f, 0.85f, 1f));
        environment.add(new DirectionalLight().set(1f, 0.92f, 0.78f, -0.45f, -0.82f, -0.35f));
        environment.add(new DirectionalLight().set(0.25f, 0.35f, 0.55f, 0.5f, -0.25f, 0.2f));

        buildWorld();
        buildHero();
        setupInput();
    }

    private Texture tex(String path) {
        Texture t = new Texture(Gdx.files.internal(path), true);
        t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        return t;
    }

    private Material mat(Texture texture) {
        return new Material(TextureAttribute.createDiffuse(texture), ColorAttribute.createSpecular(0.18f, 0.18f, 0.18f, 1f));
    }

    private void buildWorld() {
        Model ground = modelBuilder.createBox(80f, 0.18f, 80f, mat(grass), ATTRS);
        models.add(ground);
        world.add(new ModelInstance(ground, 0f, -0.11f, 0f));

        Model pathTile = modelBuilder.createBox(2.2f, 0.08f, 2.2f, mat(stone), ATTRS);
        models.add(pathTile);
        for (int i = -11; i <= 11; i++) {
            ModelInstance tile = new ModelInstance(pathTile);
            tile.transform.setToTranslation(i * 2.1f, 0.02f, MathUtils.sin(i * 0.7f) * 1.5f - 5f)
                    .rotate(Vector3.Y, MathUtils.random(-10f, 10f));
            world.add(tile);
        }

        Model trunk = modelBuilder.createCylinder(0.55f, 4.8f, 0.55f, 18, mat(wood), ATTRS);
        Model crown = modelBuilder.createSphere(3.2f, 3.0f, 3.2f, 18, 18,
                new Material(ColorAttribute.createDiffuse(new Color(0.18f, 0.48f, 0.18f, 1f))), ATTRS);
        Model rock = modelBuilder.createSphere(1.4f, 0.9f, 1.2f, 16, 12, mat(stone), ATTRS);
        models.add(trunk); models.add(crown); models.add(rock);

        float[][] trees = {{-11, 7}, {-17, -4}, {-7, -15}, {8, 10}, {16, 4}, {13, -12}, {-22, 16}, {23, -18}};
        for (float[] p : trees) {
            world.add(new ModelInstance(trunk, p[0], 2.25f, p[1]));
            ModelInstance c = new ModelInstance(crown, p[0], 5.3f, p[1]);
            c.transform.rotate(Vector3.Y, MathUtils.random(0, 360));
            world.add(c);
        }
        for (int i = 0; i < 30; i++) {
            ModelInstance r = new ModelInstance(rock);
            r.transform.setToTranslation(MathUtils.random(-34f, 34f), 0.32f, MathUtils.random(-34f, 34f))
                    .rotate(Vector3.Y, MathUtils.random(360f))
                    .scale(MathUtils.random(0.6f, 1.7f), MathUtils.random(0.45f, 1.2f), MathUtils.random(0.6f, 1.6f));
            world.add(r);
        }
    }

    private void buildHero() {
        addHeroPart("body", box(0.95f, 1.45f, 0.48f, cloth), 0f, 1.6f, 0f);
        addHeroPart("head", sphere(0.72f, 0.78f, 0.72f, skin), 0f, 2.65f, 0f);
        addHeroPart("hair", sphere(0.78f, 0.36f, 0.78f, hair), 0f, 3.03f, -0.02f);
        addHeroPart("armL", box(0.28f, 1.18f, 0.28f, skin), -0.72f, 1.52f, 0f);
        addHeroPart("armR", box(0.28f, 1.18f, 0.28f, skin), 0.72f, 1.52f, 0f);
        addHeroPart("handL", sphere(0.34f, 0.34f, 0.34f, skin), -0.72f, 0.88f, 0f);
        addHeroPart("handR", sphere(0.34f, 0.34f, 0.34f, skin), 0.72f, 0.88f, 0f);
        addHeroPart("legL", box(0.34f, 1.25f, 0.34f, leather), -0.28f, 0.55f, 0f);
        addHeroPart("legR", box(0.34f, 1.25f, 0.34f, leather), 0.28f, 0.55f, 0f);
        addHeroPart("bootL", box(0.45f, 0.25f, 0.75f, leather), -0.28f, -0.12f, 0.14f);
        addHeroPart("bootR", box(0.45f, 0.25f, 0.75f, leather), 0.28f, -0.12f, 0.14f);
    }

    private Model box(float w, float h, float d, Texture t) { Model m = modelBuilder.createBox(w, h, d, mat(t), ATTRS); models.add(m); return m; }
    private Model sphere(float w, float h, float d, Texture t) { Model m = modelBuilder.createSphere(w, h, d, 24, 16, mat(t), ATTRS); models.add(m); return m; }
    private void addHeroPart(String name, Model model, float x, float y, float z) { hero.add(new HeroPart(name, new ModelInstance(model), new Vector3(x, y, z))); }

    private void setupInput() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (screenX < Gdx.graphics.getWidth() * 0.45f && stickPointer == -1) {
                    stickPointer = pointer;
                    stickOrigin.set(screenX, screenY);
                    stickCurrent.set(screenX, screenY);
                    return true;
                }
                lookPointer = pointer;
                lastLookX = screenX;
                lastLookY = screenY;
                return true;
            }
            @Override public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (pointer == stickPointer) {
                    stickCurrent.set(screenX, screenY);
                    moveStick.set(stickCurrent).sub(stickOrigin).scl(1f / 90f);
                    moveStick.y = -moveStick.y;
                    if (moveStick.len() > 1f) moveStick.nor();
                    return true;
                }
                if (pointer == lookPointer) {
                    cameraYaw -= (screenX - lastLookX) * 0.22f;
                    cameraPitch += (screenY - lastLookY) * 0.16f;
                    cameraPitch = MathUtils.clamp(cameraPitch, 8f, 48f);
                    lastLookX = screenX; lastLookY = screenY;
                    return true;
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

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        update(dt);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.47f, 0.66f, 0.92f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        for (ModelInstance instance : world) modelBatch.render(instance, environment);
        for (HeroPart part : hero) modelBatch.render(part.instance, environment);
        modelBatch.end();

        drawHud();
    }

    private void update(float dt) {
        Vector3 camForward = new Vector3(MathUtils.sinDeg(cameraYaw), 0f, MathUtils.cosDeg(cameraYaw)).nor();
        Vector3 camRight = new Vector3(camForward.z, 0f, -camForward.x).nor();
        Vector3 move = new Vector3(camRight).scl(moveStick.x).add(new Vector3(camForward).scl(moveStick.y));
        if (move.len2() > 0.001f) {
            move.nor();
            velocity.set(move).scl(5.2f);
            heroYaw = MathUtils.atan2(move.x, move.z) * MathUtils.radiansToDegrees;
            walkTime += dt * 9.5f;
        } else {
            velocity.scl((float)Math.pow(0.05f, dt));
            walkTime += dt * 2.0f;
        }
        heroPos.mulAdd(velocity, dt);
        heroPos.x = MathUtils.clamp(heroPos.x, -36f, 36f);
        heroPos.z = MathUtils.clamp(heroPos.z, -36f, 36f);
        updateHeroParts();
        updateCamera();
    }

    private void updateHeroParts() {
        float speed = moveStick.len();
        float swing = MathUtils.sin(walkTime) * 24f * speed;
        float bob = Math.abs(MathUtils.sin(walkTime)) * 0.07f * speed;
        for (HeroPart p : hero) {
            float rx = 0f;
            if (p.name.equals("armL") || p.name.equals("handL")) rx = swing;
            if (p.name.equals("armR") || p.name.equals("handR")) rx = -swing;
            if (p.name.equals("legL") || p.name.equals("bootL")) rx = -swing * 0.65f;
            if (p.name.equals("legR") || p.name.equals("bootR")) rx = swing * 0.65f;
            p.instance.transform.idt()
                    .translate(heroPos.x, heroPos.y + bob, heroPos.z)
                    .rotate(Vector3.Y, heroYaw)
                    .translate(p.offset)
                    .rotate(Vector3.X, rx);
        }
    }

    private void updateCamera() {
        float dist = 8.5f;
        float y = MathUtils.sinDeg(cameraPitch) * dist + 2.6f;
        float flat = MathUtils.cosDeg(cameraPitch) * dist;
        Vector3 desired = new Vector3(
                heroPos.x - MathUtils.sinDeg(cameraYaw) * flat,
                heroPos.y + y,
                heroPos.z - MathUtils.cosDeg(cameraYaw) * flat
        );
        camera.position.lerp(desired, 0.12f);
        camera.lookAt(heroPos.x, heroPos.y + 1.6f, heroPos.z);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    private void drawHud() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.28f);
        float baseX = stickPointer == -1 ? 120f : stickOrigin.x;
        float baseY = stickPointer == -1 ? Gdx.graphics.getHeight() - 120f : stickOrigin.y;
        shapes.circle(baseX, Gdx.graphics.getHeight() - baseY, 72f);
        shapes.setColor(0.2f, 0.48f, 1f, 0.58f);
        float knobX = stickPointer == -1 ? baseX : stickCurrent.x;
        float knobY = stickPointer == -1 ? baseY : stickCurrent.y;
        shapes.circle(knobX, Gdx.graphics.getHeight() - knobY, 34f);
        shapes.end();
        sprites.begin();
        font.draw(sprites, "Mobile 3D Adventure  •  left: move  right: camera", 18, Gdx.graphics.getHeight() - 18);
        font.draw(sprites, "Generated texture pack: grass, stone, wood, cloth, skin, leather, hair", 18, Gdx.graphics.getHeight() - 42);
        sprites.end();
    }

    @Override public void resize(int width, int height) { camera.viewportWidth = width; camera.viewportHeight = height; camera.update(); }

    @Override
    public void dispose() {
        modelBatch.dispose(); shapes.dispose(); sprites.dispose(); font.dispose();
        grass.dispose(); stone.dispose(); wood.dispose(); cloth.dispose(); skin.dispose(); leather.dispose(); hair.dispose();
        for (Model model : models) model.dispose();
    }

    private static class HeroPart {
        final String name;
        final ModelInstance instance;
        final Vector3 offset;
        HeroPart(String name, ModelInstance instance, Vector3 offset) { this.name = name; this.instance = instance; this.offset = offset; }
    }
}
