# Godot 4 Complex Engine Version

This repository now includes a Godot 4 implementation of the mobile 3D adventure game under:

```text
godot/
```

## Why Godot 4

Godot 4 is a full game engine with a real 3D renderer, lighting, environment/fog, materials, scenes, mobile renderer, audio, input system and Android export pipeline. It is much closer to a serious game-engine workflow than the earlier LibGDX prototype.

## Implemented in Godot version

- Full Godot 4 project
- Mobile renderer configuration
- Filmic tonemapping and darker realistic color grading
- Fog/atmospheric environment
- Directional sun light with shadows
- Procedural realistic humanoid assembled from 3D meshes
- Animated walking/breathing
- Fixed-base mobile joystick
- Camera look control
- Infinite procedural chunk world
- Object pooling: chunks are reused around player for FPS
- Generated realistic texture pack reused from repository assets
- Sound effects: footsteps, wind loop, chime

## Run in Godot Editor

1. Install Godot 4.3+ or 4.4+.
2. Open the folder:

```text
godot/
```

3. Press Play.

## Android Export

Open `godot/project.godot` in Godot, install Android export templates, then export using the Android preset.

The Godot version is now the main direction for a more serious 3D mobile game. The older LibGDX project remains in `android/` and `core/` for comparison and fallback builds.
