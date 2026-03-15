# NetMusic Mod for MITE (FML 3.4.2)

This repository is used to port the NetMusic mod to MITE 1.6.4 + FishModLoader 3.4.2.

## Current Status

- Source package: `com.github.tartaricacid.netmusic` (81 Java files)
- Resources and metadata are in place (`fml.mod.json`, `assets/netmusic`, `data/netmusic`)
- Build is currently blocked by unported modern Minecraft API usage (`net.minecraft.world.*`, `net.minecraft.core.*`, etc.)

## Quick Verify

```powershell
.\gradlew.bat build --no-daemon
```

Expected right now: compile errors in unported classes.

## Migration Notes

- This project is no longer the AnimalRefresh example.
- Porting work should focus on replacing modern 1.20-style APIs with MITE 1.6.4/FML-compatible APIs module by module.
- MITE 1.6.4 localization uses `assets/netmusic/lang/*.lang` (`en_US.lang`, `zh_CN.lang`), not modern JSON language files.
