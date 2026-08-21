import dev.deftu.gradle.utils.GameSide

plugins {
    java
    val dgtVersion = "2.33.3"
    id("dev.deftu.gradle.tools") version(dgtVersion)
    id("dev.deftu.gradle.tools.shadow") version(dgtVersion)
    id("dev.deftu.gradle.tools.minecraft.loom") version(dgtVersion)
    id("dev.deftu.gradle.tools.resources") version(dgtVersion)
}

toolkitLoomHelper {
    useForgeMixin(modData.id)
    useMixinRefMap(modData.id)
    useTweaker("org.spongepowered.asm.launch.MixinTweaker")
    useProperty("mixin.debug.export", "true", GameSide.CLIENT)
    disableRunConfigs(GameSide.SERVER)
    useDevAuth("1.2.1")
}

repositories {
    maven("https://repo.spongepowered.org/maven/")
    maven("https://maven.deftu.dev/releases")
    maven("https://maven.bawnorton.com/releases")
    mavenCentral()
}

dependencies {
    implementation(shade("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    })

    implementation(shade("org.java-websocket:Java-WebSocket:1.6.0")!!)
}