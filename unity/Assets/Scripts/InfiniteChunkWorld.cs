using System.Collections.Generic;
using UnityEngine;

namespace Wildland3D
{
    public sealed class InfiniteChunkWorld : MonoBehaviour
    {
        [Header("Streaming")]
        public Transform player;
        public int radius = 3;
        public float chunkSize = 64f;
        public int updatesPerFrame = 3;

        [Header("Rendering")]
        public Material groundMaterial;
        public Material rockMaterial;
        public Material barkMaterial;
        public Material leafMaterial;
        public Material grassMaterial;

        private readonly List<Chunk> chunks = new();
        private readonly Queue<Vector2Int> pending = new();
        private Vector2Int center = new(int.MinValue, int.MinValue);
        private Mesh cubeMesh;
        private Mesh sphereMesh;
        private Mesh cylinderMesh;

        private void Awake()
        {
            cubeMesh = CreatePrimitiveMesh(PrimitiveType.Cube);
            sphereMesh = CreatePrimitiveMesh(PrimitiveType.Sphere);
            cylinderMesh = CreatePrimitiveMesh(PrimitiveType.Cylinder);
            int count = (radius * 2 + 1) * (radius * 2 + 1);
            for (int i = 0; i < count; i++) chunks.Add(CreateChunk(i));
        }

        private void Update()
        {
            if (!player) return;
            var newCenter = new Vector2Int(Mathf.FloorToInt(player.position.x / chunkSize), Mathf.FloorToInt(player.position.z / chunkSize));
            if (newCenter != center)
            {
                center = newCenter;
                pending.Clear();
                var coords = new List<Vector2Int>();
                for (int z = -radius; z <= radius; z++)
                    for (int x = -radius; x <= radius; x++)
                        coords.Add(new Vector2Int(center.x + x, center.y + z));
                coords.Sort((a, b) => (a - center).sqrMagnitude.CompareTo((b - center).sqrMagnitude));
                foreach (var c in coords) pending.Enqueue(c);
            }
            for (int i = 0; i < updatesPerFrame && pending.Count > 0; i++)
            {
                int idx = chunks.Count - pending.Count;
                if (idx < 0 || idx >= chunks.Count) idx = i % chunks.Count;
                PositionChunk(chunks[idx], pending.Dequeue());
            }
        }

        private Chunk CreateChunk(int index)
        {
            var root = new GameObject($"Chunk_{index}").transform;
            root.SetParent(transform, false);
            var chunk = new Chunk { root = root };
            chunk.ground = CreatePart(root, "Ground", cubeMesh, groundMaterial);
            chunk.path = CreatePart(root, "Path", cubeMesh, rockMaterial);
            for (int i = 0; i < 2; i++) chunk.trees.Add(CreateTree(root, i));
            for (int i = 0; i < 4; i++) chunk.rocks.Add(CreatePart(root, $"Rock_{i}", sphereMesh, rockMaterial));
            for (int i = 0; i < 8; i++) chunk.grass.Add(CreatePart(root, $"Grass_{i}", cylinderMesh, grassMaterial));
            return chunk;
        }

        private Tree CreateTree(Transform root, int i)
        {
            var t = new GameObject($"Tree_{i}").transform;
            t.SetParent(root, false);
            return new Tree
            {
                root = t,
                trunk = CreatePart(t, "Trunk", cylinderMesh, barkMaterial),
                crown = CreatePart(t, "Crown", sphereMesh, leafMaterial),
                crown2 = CreatePart(t, "Crown2", sphereMesh, leafMaterial),
            };
        }

        private MeshRenderer CreatePart(Transform parent, string name, Mesh mesh, Material mat)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var mf = go.AddComponent<MeshFilter>(); mf.sharedMesh = mesh;
            var mr = go.AddComponent<MeshRenderer>(); mr.sharedMaterial = mat;
            return mr;
        }

        private void PositionChunk(Chunk c, Vector2Int coord)
        {
            c.root.position = new Vector3(coord.x * chunkSize, 0, coord.y * chunkSize);
            c.ground.transform.localPosition = new Vector3(0, -0.06f, 0);
            c.ground.transform.localScale = new Vector3(chunkSize + .2f, .08f, chunkSize + .2f);
            c.path.transform.localPosition = new Vector3(Signed(coord, 1) * 8f, .02f, Signed(coord, 2) * 8f);
            c.path.transform.localRotation = Quaternion.Euler(0, Rand(coord, 3) * 360f, 0);
            c.path.transform.localScale = new Vector3(7f, .08f, 4f);
            for (int i = 0; i < c.trees.Count; i++)
            {
                float s = .85f + Rand(coord, 10 + i) * .55f;
                var tr = c.trees[i].root;
                tr.localPosition = new Vector3(Signed(coord, 20+i) * 24f, 0, Signed(coord, 30+i) * 24f);
                tr.localRotation = Quaternion.Euler(0, Rand(coord, 40+i) * 360f, 0);
                tr.localScale = Vector3.one * s;
                c.trees[i].trunk.transform.localPosition = new Vector3(0, 2f, 0);
                c.trees[i].trunk.transform.localScale = new Vector3(.55f, 2f, .55f);
                c.trees[i].crown.transform.localPosition = new Vector3(0, 4.4f, 0);
                c.trees[i].crown.transform.localScale = new Vector3(4f, 2.8f, 4f);
                c.trees[i].crown2.transform.localPosition = new Vector3(.65f, 5.2f, -.35f);
                c.trees[i].crown2.transform.localScale = new Vector3(2.5f, 1.7f, 2.5f);
            }
            for (int i = 0; i < c.rocks.Count; i++)
            {
                c.rocks[i].transform.localPosition = new Vector3(Signed(coord, 50+i) * 26f, .3f, Signed(coord, 70+i) * 26f);
                float s = .6f + Rand(coord, 90+i) * 1.3f;
                c.rocks[i].transform.localScale = new Vector3(s, .45f + Rand(coord, 100+i) * .6f, s * .9f);
            }
            for (int i = 0; i < c.grass.Count; i++)
            {
                c.grass[i].transform.localPosition = new Vector3(Signed(coord, 130+i) * 29f, .25f, Signed(coord, 160+i) * 29f);
                c.grass[i].transform.localRotation = Quaternion.Euler(Signed(coord, 180+i) * 12f, Rand(coord, 200+i) * 360f, Signed(coord, 220+i) * 12f);
                c.grass[i].transform.localScale = new Vector3(.18f, .55f + Rand(coord, 240+i) * .7f, .18f);
            }
        }

        private static float Rand(Vector2Int p, int salt)
        {
            int h = p.x * 73428767 ^ p.y * 912931 ^ salt * 19349663;
            h ^= h >> 13; h *= 1274126177; h ^= h >> 16;
            return (h & 0x7fffffff) / (float)int.MaxValue;
        }
        private static float Signed(Vector2Int p, int salt) => Rand(p, salt) * 2f - 1f;
        private static Mesh CreatePrimitiveMesh(PrimitiveType type)
        {
            var temp = GameObject.CreatePrimitive(type);
            var mesh = temp.GetComponent<MeshFilter>().sharedMesh;
            Destroy(temp);
            return mesh;
        }
        private sealed class Chunk { public Transform root; public MeshRenderer ground, path; public List<Tree> trees = new(); public List<MeshRenderer> rocks = new(); public List<MeshRenderer> grass = new(); }
        private sealed class Tree { public Transform root; public MeshRenderer trunk, crown, crown2; }
    }
}
