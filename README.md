# Lurk 26.1.2 (decompiled reconstruction)

This project is a source reconstruction of `Lurk-26.1.2.jar` using CFR. It contains the recovered Java sources, resources, nested libraries, and a build configuration targeting Minecraft 26.1.2 / Java 25.

## Build

```powershell
.\gradlew.bat build
```

The build requires Java 25 and the locally generated compile-only Minecraft stub at `compile-libs/minecraft-patched.jar`. This game-derived file is excluded from the repository. Generate it from a local Minecraft 26.1.2 client jar with:

```powershell
javac -cp compile-libs/asm-9.10.1.jar -d tools tools/PatchCompileJar.java
java -cp "tools;compile-libs/asm-9.10.1.jar" PatchCompileJar C:/path/to/26.1.2.jar compile-libs/minecraft-patched.jar
```

The output mod JAR is written to `build/libs/`.

## Notes

- The original JAR did not contain its Gradle project files, so the build files were recreated from the embedded Fabric metadata.
- The default Gradle properties use repository-relative paths. Runtime testing needs a local Minecraft 26.1.2 installation; copy `gradle-local.properties.example` to `gradle-local.properties` and fill in the paths before running `runClient`.
- The original decompiler output, local logs, generated test instance, and bundled Gradle distributions are intentionally excluded from version control.
- The compile-only Minecraft stub is generated locally from a Minecraft 26.1.2 client jar with `tools/PatchCompileJar.java`; it is not redistributed here.
