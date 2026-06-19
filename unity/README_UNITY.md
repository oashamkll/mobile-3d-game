# Unity Engine Version

This folder contains a Unity/URP direction for the game. Unity is a stronger commercial mobile 3D engine than the lightweight prototype and is suitable for better graphics, animation, terrain streaming and Android release builds.

Open this folder as a Unity project with Unity 2022.3 LTS or newer. The scripts implement:

- infinite chunk terrain with object pooling;
- smoother chunk streaming with preload radius;
- third-person mobile controller;
- fixed joystick input;
- procedural humanoid placeholder that can be replaced by a rigged FBX/GLTF model;
- URP-friendly runtime materials.

A GitHub Actions Unity build normally requires a Unity license activation secret, so this repo includes source and project structure; APK export should be run from Unity Editor or configured with GameCI secrets.
