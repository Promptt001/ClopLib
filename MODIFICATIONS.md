# Modifications in this fork

This repository is a fork of [WiIIiam278/ClopLib](https://github.com/WiIIiam278/ClopLib),
maintained to provide the ClopLib builds that
[HuskClaimsPlus](https://github.com/Promptt001/HuskClaimsPlus) needs. It is not
made, endorsed or supported by William278. Use upstream ClopLib for anything else.

Based on WiIIiam278/ClopLib `75c5004`.

## What changed

- Added Fabric nodes for Minecraft **26.2** and **26.3** (mojmap), built from the
  same tagged commit. Removed the retired `fabric/1.21.1`, `1.21.4`, `1.21.5`
  and `1.21.7` nodes.
- Ported the Fabric listener, event and mixin layer from Yarn/1.21.x to the 26.2
  API, including the mixin retargeting the port required.
- Operation fixes found while porting: placement is evaluated at the player's
  position; the correct block is reported on the `+X`, `+Z` and top faces; the
  highlighter offset is corrected; crop detection is fixed.
- `library_version` is `2.0.13.1` so the artifact sorts **above** upstream
  `2.0.13` in Fabric Loader's version comparison. Fabric Loader accepts any
  number of version core components, so `2.0.13.1 > 2.0.13`.
- Removed four duplicate Gradle wrapper files that upstream tracked by mistake
  (`gradle-wrapper.jar`, `gradle-wrapper.properties`, `gradle/wrapper/gradlew`,
  `gradle/wrapper/gradlew.bat`). The wrapper at `gradle/wrapper/` and `gradlew`
  at the repository root are unchanged and authoritative.
- Updated the Gradle wrapper to 9.4.1 and the build tooling versions the 26.x
  nodes require.

`/common` and `/bukkit` are unchanged from upstream. Paper continues to use
upstream `cloplib-bukkit` from `repo.william278.net`.

## Licence

ClopLib is licensed under the Apache License 2.0. This fork keeps that licence;
see `LICENSE`. Changes here are offered under the same terms, per section 4(b)
of the licence.
