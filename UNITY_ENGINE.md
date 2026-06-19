# Unity Primary Version

The project is now primarily a Unity mobile 3D game under:

```text
unity/
```

## Implemented

- Unity 2022.3 LTS project structure
- URP package dependency
- Runtime scene bootstrap
- Infinite chunk world with pooling
- Physics colliders for ground, path, trees and rocks
- `CharacterController` player so the hero does not pass through objects
- Fixed mobile joystick UI
- Horizontal/right-left joystick inversion fix
- Third-person camera controller
- Procedural humanoid with walking animation
- Footstep and wind audio
- Generated texture pack imported as Unity Resources
- Android build script: `Wildland3D.Editor.AndroidBuild.PerformBuild`

## GitHub Actions build requirement

Unity builds on GitHub require a Unity license secret. Add this repository secret:

```text
UNITY_LICENSE
```

Then run:

```text
Actions -> Build Unity Android APK -> Run workflow
```

The APK artifact will be named:

```text
wildland-3d-unity-apk
```

## Local build

Open `unity/` in Unity 2022.3 LTS and run the editor method:

```text
Wildland3D.Editor.AndroidBuild.PerformBuild
```

or build Android from Build Settings.
