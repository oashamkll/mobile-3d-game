# Defold Mobile Engine Version

The project now includes a Defold engine implementation under:

```text
defold/
```

Defold is a lightweight, production-grade engine optimized for mobile games and can build APKs in CI without Unity licensing.

Current Defold direction includes:

- landscape Android configuration;
- mobile joystick with corrected direction;
- lightweight infinite-world streaming logic;
- mobile FPS-first project settings;
- generated texture/audio assets prepared for model integration;
- GitHub Actions APK build through Defold Bob.

The immediate Defold version is a mobile-engine foundation. The next content pass should add real `.gltf/.dae` model components and custom render/material resources for full 3D visuals.
