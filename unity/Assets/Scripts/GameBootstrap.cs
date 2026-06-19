using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;

namespace Wildland3D
{
    /// <summary>
    /// Runtime bootstrap for a complete Unity scene. This lets the project run from
    /// an empty scene and still create camera, lighting, UI joystick, physics world,
    /// procedural hero, infinite chunks, materials and audio.
    /// </summary>
    public sealed class GameBootstrap : MonoBehaviour
    {
        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        private static void AutoCreate()
        {
            if (FindObjectOfType<GameBootstrap>() != null) return;
            new GameObject("Wildland3D Bootstrap").AddComponent<GameBootstrap>();
        }

        private void Awake()
        {
            Application.targetFrameRate = 90;
            QualitySettings.vSyncCount = 0;
            QualitySettings.antiAliasing = 2;
            QualitySettings.shadowDistance = 70f;
            QualitySettings.lodBias = 1.4f;

            CreateLighting();
            var materials = CreateMaterials();
            var hero = CreateHero(materials);
            var cam = CreateCamera();
            var joystick = CreateJoystick();
            CreateAudio(hero);
            CreateWorld(hero.transform, materials);

            var controller = hero.AddComponent<ThirdPersonMobileController>();
            controller.gameCamera = cam;
            controller.joystick = joystick;
            controller.heroAnimator = hero.GetComponent<ProceduralHeroAnimator>();
        }

        private void CreateLighting()
        {
            RenderSettings.ambientMode = UnityEngine.Rendering.AmbientMode.Trilight;
            RenderSettings.ambientSkyColor = new Color(.58f, .61f, .62f);
            RenderSettings.ambientEquatorColor = new Color(.42f, .43f, .40f);
            RenderSettings.ambientGroundColor = new Color(.24f, .23f, .21f);
            RenderSettings.fog = false;

            var sunGo = new GameObject("Realistic Low Sun");
            var sun = sunGo.AddComponent<Light>();
            sun.type = LightType.Directional;
            sun.color = new Color(1.0f, .89f, .70f);
            sun.intensity = 1.25f;
            sun.shadows = LightShadows.Soft;
            sun.shadowStrength = .45f;
            sunGo.transform.rotation = Quaternion.Euler(48f, -38f, 0f);
        }

        private MaterialSet CreateMaterials()
        {
            return new MaterialSet
            {
                ground = MakeMat("Textures/environment/grass_albedo", new Color(.32f,.36f,.27f), .85f),
                stone = MakeMat("Textures/environment/stone_albedo", new Color(.46f,.45f,.42f), .75f),
                bark = MakeMat("Textures/environment/wood_albedo", new Color(.33f,.24f,.18f), .78f),
                leaves = MakeMat("Textures/environment/leaves_albedo", new Color(.25f,.32f,.21f), .88f),
                cloth = MakeMat("Textures/character/hero_cloth_albedo", new Color(.15f,.20f,.25f), .66f),
                skin = MakeMat("Textures/character/hero_skin_albedo", new Color(.70f,.50f,.38f), .55f),
                leather = MakeMat("Textures/character/hero_leather_albedo", new Color(.31f,.23f,.17f), .60f),
                hair = MakeMat("Textures/character/hero_hair_albedo", new Color(.12f,.08f,.05f), .72f),
                metal = MakeMat("Textures/character/hero_metal_albedo", new Color(.70f,.72f,.72f), .35f),
            };
        }

        private Material MakeMat(string resourcePath, Color color, float smoothness)
        {
            var mat = new Material(Shader.Find("Universal Render Pipeline/Lit") ?? Shader.Find("Standard"));
            mat.color = color;
            var tex = Resources.Load<Texture2D>(resourcePath);
            if (tex) mat.mainTexture = tex;
            if (mat.HasProperty("_Smoothness")) mat.SetFloat("_Smoothness", smoothness);
            if (mat.HasProperty("_Metallic")) mat.SetFloat("_Metallic", resourcePath.Contains("metal") ? .45f : 0f);
            return mat;
        }

        private GameObject CreateHero(MaterialSet m)
        {
            var root = new GameObject("Physics Hero");
            root.transform.position = new Vector3(0, .2f, 0);
            var cc = root.AddComponent<CharacterController>();
            cc.height = 2.9f;
            cc.radius = .38f;
            cc.center = new Vector3(0, 1.25f, 0);
            cc.stepOffset = .38f;
            cc.slopeLimit = 45f;

            Transform torso = Part(root.transform, "Torso", PrimitiveType.Capsule, m.cloth, new Vector3(0, 1.55f, 0), new Vector3(.55f, .82f, .34f));
            Transform chest = Part(root.transform, "Chest", PrimitiveType.Sphere, m.cloth, new Vector3(0, 1.95f, .02f), new Vector3(.86f, .58f, .42f));
            Transform pelvis = Part(root.transform, "Pelvis", PrimitiveType.Cube, m.leather, new Vector3(0, .95f, 0), new Vector3(.72f, .35f, .42f));
            Transform head = Part(root.transform, "Head", PrimitiveType.Sphere, m.skin, new Vector3(0, 2.66f, .02f), new Vector3(.48f, .58f, .45f));
            Transform hair = Part(root.transform, "Hair", PrimitiveType.Sphere, m.hair, new Vector3(0, 2.96f, -.04f), new Vector3(.52f, .22f, .47f));
            Part(root.transform, "Backpack", PrimitiveType.Cube, m.leather, new Vector3(0, 1.65f, -.34f), new Vector3(.55f, .75f, .22f));
            Part(root.transform, "Belt", PrimitiveType.Cube, m.leather, new Vector3(0, 1.16f, .02f), new Vector3(.86f, .09f, .44f));

            var animator = root.AddComponent<ProceduralHeroAnimator>();
            animator.head = head;
            for (int side = -1; side <= 1; side += 2)
            {
                string tag = side < 0 ? "L" : "R";
                Part(root.transform, "Shoulder" + tag, PrimitiveType.Sphere, m.cloth, new Vector3(.55f*side, 2.02f, 0), Vector3.one*.24f);
                Transform arm = Part(root.transform, "Arm" + tag, PrimitiveType.Capsule, m.cloth, new Vector3(.66f*side, 1.50f, 0), new Vector3(.20f, .54f, .20f));
                Transform forearm = Part(root.transform, "Forearm" + tag, PrimitiveType.Capsule, m.skin, new Vector3(.67f*side, 1.04f, 0), new Vector3(.18f, .48f, .18f));
                Part(root.transform, "Hand" + tag, PrimitiveType.Sphere, m.skin, new Vector3(.67f*side, .70f, 0), Vector3.one*.17f);
                Transform leg = Part(root.transform, "Leg" + tag, PrimitiveType.Capsule, m.leather, new Vector3(.20f*side, .55f, 0), new Vector3(.20f, .63f, .20f));
                Transform boot = Part(root.transform, "Boot" + tag, PrimitiveType.Cube, m.leather, new Vector3(.20f*side, -.08f, .12f), new Vector3(.34f, .18f, .50f));
                if (side < 0) { animator.leftArm = arm; animator.leftLeg = leg; }
                else { animator.rightArm = arm; animator.rightLeg = leg; }
            }
            return root;
        }

        private Transform Part(Transform parent, string name, PrimitiveType type, Material mat, Vector3 localPos, Vector3 localScale)
        {
            var go = GameObject.CreatePrimitive(type);
            go.name = name;
            Destroy(go.GetComponent<Collider>()); // CharacterController is the only hero collider.
            go.transform.SetParent(parent, false);
            go.transform.localPosition = localPos;
            go.transform.localScale = localScale;
            go.GetComponent<MeshRenderer>().sharedMaterial = mat;
            return go.transform;
        }

        private Camera CreateCamera()
        {
            var go = new GameObject("Main Camera");
            var cam = go.AddComponent<Camera>();
            cam.tag = "MainCamera";
            cam.fieldOfView = 64f;
            cam.nearClipPlane = .08f;
            cam.farClipPlane = 220f;
            go.AddComponent<AudioListener>();
            return cam;
        }

        private MobileJoystick CreateJoystick()
        {
            var eventSystem = new GameObject("EventSystem");
            eventSystem.AddComponent<EventSystem>();
            eventSystem.AddComponent<StandaloneInputModule>();

            var canvasGo = new GameObject("Mobile HUD");
            var canvas = canvasGo.AddComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            canvasGo.AddComponent<CanvasScaler>().uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            canvasGo.GetComponent<CanvasScaler>().referenceResolution = new Vector2(1600, 900);
            canvasGo.AddComponent<GraphicRaycaster>();

            var baseImg = HudCircle("Joystick Base", canvasGo.transform, new Color(.08f,.09f,.08f,.55f), new Vector2(230, 210), 230);
            var knob = HudCircle("Joystick Knob", canvasGo.transform, new Color(.42f,.44f,.39f,.90f), new Vector2(230, 210), 92);
            var joy = baseImg.gameObject.AddComponent<MobileJoystick>();
            joy.baseCircle = baseImg.rectTransform;
            joy.knob = knob.rectTransform;
            joy.radius = 115f;
            joy.invertX = true; // fixes observed right/left inversion on device orientation.
            return joy;
        }

        private Image HudCircle(string name, Transform parent, Color color, Vector2 anchored, float size)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var img = go.AddComponent<Image>();
            img.color = color;
            img.sprite = Sprite.Create(Texture2D.whiteTexture, new Rect(0,0,1,1), new Vector2(.5f,.5f));
            img.type = Image.Type.Sliced;
            var rt = img.rectTransform;
            rt.anchorMin = new Vector2(0,0);
            rt.anchorMax = new Vector2(0,0);
            rt.anchoredPosition = anchored;
            rt.sizeDelta = new Vector2(size, size);
            return img;
        }

        private void CreateAudio(GameObject hero)
        {
            var step = hero.AddComponent<AudioSource>();
            step.clip = Resources.Load<AudioClip>("Audio/footstep");
            step.volume = .35f;
            hero.GetComponent<ProceduralHeroAnimator>().footstep = step;
            var windGo = new GameObject("Wind Ambience");
            var wind = windGo.AddComponent<AudioSource>();
            wind.clip = Resources.Load<AudioClip>("Audio/wind_loop");
            wind.loop = true;
            wind.volume = .18f;
            wind.Play();
        }

        private void CreateWorld(Transform hero, MaterialSet materials)
        {
            var world = new GameObject("Infinite Physics World").AddComponent<InfiniteChunkWorld>();
            world.player = hero;
            world.radius = 3;
            world.chunkSize = 72f;
            world.updatesPerFrame = 4;
            world.groundMaterial = materials.ground;
            world.rockMaterial = materials.stone;
            world.barkMaterial = materials.bark;
            world.leafMaterial = materials.leaves;
            world.grassMaterial = materials.ground;
        }

        private sealed class MaterialSet
        {
            public Material ground, stone, bark, leaves, cloth, skin, leather, hair, metal;
        }
    }
}
