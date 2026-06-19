# Mobile 3D Game

A Java/Kotlin Android 3D adventure prototype built with **LibGDX**.

## Features

- Android mobile game project, Java core + Kotlin Android launcher
- Third-person 3D camera
- Touch controls:
  - left side: virtual joystick movement
  - right side: camera rotation
- Procedural 3D hero model assembled from textured body parts
- Generated texture pack included in `android/assets/textures`
- 3D environment with grass terrain, stone path, trees and rocks
- Offline, no server required

## Generated texture pack

Textures were generated for this repo and are stored as PNG assets:

```text
android/assets/textures/character/
  hero_cloth_albedo.png
  hero_cloth_normal.png
  hero_hair_albedo.png
  hero_leather_albedo.png
  hero_leather_normal.png
  hero_skin_albedo.png

android/assets/textures/environment/
  grass_albedo.png
  grass_normal.png
  stone_albedo.png
  stone_normal.png
  wood_albedo.png
  wood_normal.png
  sky_gradient.png
```

The current prototype uses the albedo textures directly in LibGDX materials. Normal maps are included for future shader/PBR upgrades.

## Build

Open the project in Android Studio and run the `android` module.

Or from terminal with Gradle installed:

```bash
gradle :android:assembleDebug
```

The debug APK will be produced under:

```text
android/build/outputs/apk/debug/
```

## Project structure

```text
core/                         Java game logic
android/                      Android/Kotlin launcher and assets
android/assets/textures/      Generated texture pack
android/assets/models/        Reserved for future GLTF/OBJ assets
android/assets/levels/        Reserved for level data
```

## Next upgrades

- Add GLTF/FBX-style rigged hero model
- Add animations and combat/interactions
- Add collision/navigation
- Use generated normal maps in a custom shader
- Add level progression, collectibles and enemies

## Godot 4 complex-engine version

A new Godot 4 implementation lives in:

```text
godot/
```

This is now the serious-engine direction for the project: realistic darker graphics, infinite procedural chunk world, improved joystick, mobile renderer, audio, and animated procedural humanoid. See [GODOT_ENGINE.md](GODOT_ENGINE.md).

## Unity primary version

The game is now being developed primarily in Unity. See [UNITY_ENGINE.md](UNITY_ENGINE.md).

Unity source is in:

```text
unity/
```

Unity GitHub Actions build requires the `UNITY_LICENSE` repository secret.
