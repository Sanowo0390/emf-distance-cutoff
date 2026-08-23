# EMF Distance Cutoff

A Fabric mod for Minecraft 1.21.11 that lets you disable EMF model rendering beyond a configurable distance.
### ⚠️This mod is created by AI⚠️

## Features

- Global distance cutoff (default: 24 blocks)
- Per-entity enable/disable settings
- Per-entity distance overrides
- Unconfigured entities inherit the global setting
- Scrollable entity list
- Entity ID/name search
- Mod Menu configuration screen
- Japanese and English localization
- Configuration saved to `config/emf_distance_cutoff.json`
- GitHub Actions build workflow

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.3+
- Fabric API
- Entity Model Features (EMF)
- Entity Texture Features (ETF)
- Mod Menu is optional and provides the configuration screen

## Configuration

Open Mod Menu and select **EMF Distance Cutoff → Configure**.

The global distance applies to entities without an individual override. An entity can be disabled completely or assigned its own distance.

## Building

Use Gradle 9.5.1 with Java 21:

```bat
gradle build
```

The output JAR is placed in `build/libs/`.

## License

See [LICENSE](LICENSE).
