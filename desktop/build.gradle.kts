import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.net.URI

plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.bs"
version = "1.0.2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    // Same JSON stack as the backend (Jackson 3). Kept on the classpath;
    // only the JavaFX platform jars go on jpackage's --module-path below.
    implementation("tools.jackson.core:jackson-databind:3.1.5")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.1.5")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// The Boot plugin replaces the plain jar with a fat bootJar; but installDist/jpackage
// need the plain jar, so keep both (bootJar gets a classifier to avoid clashing).
tasks.named<BootJar>("bootJar") {
    archiveClassifier = "boot"
}
tasks.named<Jar>("jar") {
    enabled = true
}

javafx {
    version = "25"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass = "com.bs.lotp.desktop.DesktopApp"
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics")
}

// --- Shipping with jpackage (JDK built-in, no extra plugins) ---
// jpackage bundles the app + a trimmed JRE into a self-contained image/installer.
// Installers are platform-specific: run the build on the OS you want to ship for.
val appMainClass = "com.bs.lotp.desktop.DesktopApp"
val javafxAddModules = "javafx.base,javafx.controls,javafx.fxml,javafx.graphics"
// jpackage derives most JDK modules via jdeps, but misses ones only touched
// reflectively/lazily (e.g. java.net.http via Spring's request factory), so list them.
val jdkAddModules = "java.net.http,java.management,java.naming,java.sql,java.xml"
val javaToolchains = extensions.getByType<JavaToolchainService>()

fun registerJpackageTask(taskName: String, packageType: String, taskDescription: String) {
    tasks.register<Exec>(taskName) {
        group = "distribution"
        description = taskDescription
        dependsOn("installDist")

        // Everything is resolved at execution time so configuration stays cheap and safe.
        doFirst {
            // jpackage refuses to overwrite an existing image directory — but only
            // app-image writes a directory. Installer types (deb/msi/dmg) write a
            // single file into --dest, so deleting here would wipe a sibling
            // jpackageImage output that the release workflow zips up afterwards.
            if (packageType == "app-image") {
                delete(layout.buildDirectory.dir("jpackage/lotp-desktop").get().asFile)
            }

            val launcher = javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
            }.get()
            // Windows ships jpackage.exe; Unix-likes ship jpackage.
            val jpackageName = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "jpackage.exe" else "jpackage"
            val jpackageBin = launcher.metadata.installationPath.asFile.resolve("bin/$jpackageName")
            require(jpackageBin.isFile) { "jpackage not found in toolchain JDK: $jpackageBin" }

            val libDir = layout.buildDirectory.dir("install/desktop/lib").get().asFile
            require(libDir.isDirectory) { "installDist output missing: $libDir" }
            val mainJar = tasks.named<Jar>("jar").get().archiveFileName.get()

            // JavaFX platform jars (e.g. javafx-controls-25-linux.jar) are modular jars,
            // so jlink (invoked internally by jpackage) can use them as --module-path.
            // Exclude the classifier-less aggregator jars to avoid duplicate modules.
            val platformClassifier = Regex(".*-(linux|linux-aarch64|win|mac|mac-aarch64)\\.jar")
            val modulePath = configurations.runtimeClasspath.get().files
                .filter { it.name.startsWith("javafx-") && it.name.matches(platformClassifier) }
                .joinToString(File.pathSeparator) { it.absolutePath }
            require(modulePath.isNotEmpty()) { "No JavaFX platform jars found on runtimeClasspath" }

            executable = jpackageBin.absolutePath
            // Release builds pass -PappVersion=1.2.3 (tag); dev builds fall back to
            // project.version without the -SNAPSHOT suffix (jpackage requires X.Y[.Z]).
            val rawVersion = (findProperty("appVersion") as String?) ?: version.toString()
            val appVersion = rawVersion.removePrefix("v").removeSuffix("-SNAPSHOT")
            val jpackageArgs = mutableListOf(
                "--type", packageType,
                "--dest", layout.buildDirectory.dir("jpackage").get().asFile.absolutePath,
                "--name", "lotp-desktop",
                "--app-version", appVersion,
                "--vendor", "lotp",
                "--input", libDir.absolutePath,
                "--main-jar", mainJar,
                "--main-class", appMainClass,
                "--module-path", modulePath,
                "--add-modules", "$javafxAddModules,$jdkAddModules",
                "--java-options", "--enable-native-access=javafx.graphics"
            )
            // jpackage wants a per-OS icon format; use it only when present.
            val iconExt = when {
                org.gradle.internal.os.OperatingSystem.current().isWindows -> "ico"
                org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "icns"
                else -> "png"
            }
            val iconFile = projectDir.resolve("src/main/resources/icon.$iconExt")
            if (iconFile.isFile) jpackageArgs.addAll(listOf("--icon", iconFile.absolutePath))
            args = jpackageArgs
        }
    }
}

// Portable folder: build/jpackage/lotp-desktop/bin/lotp-desktop — zip this for ad-hoc sharing.
registerJpackageTask(
    "jpackageImage",
    "app-image",
    "Builds a self-contained app image (no installer, works everywhere)."
)
// Native installer for the current OS. jpackage installers are platform-specific:
// run the build on the OS you want to ship for (CI does this via a build matrix).
// Linux 'deb' needs dpkg-deb, Windows 'msi' needs WiX Toolset, macOS 'dmg' needs nothing extra.
val installerType = when {
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "msi"
    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "dmg"
    else -> "deb"
}
registerJpackageTask(
    "jpackageInstaller",
    installerType,
    "Builds a native installer for the current OS (deb on Linux, msi on Windows, dmg on macOS)."
)

// --- AppImage (Linux single-file bundle, no root needed) ---
// jpackage has no AppImage type, so we wrap the app-image output in an AppDir
// (AppRun + .desktop + icon) and pack it with appimagetool. Linux-only.
val isLinuxHost = org.gradle.internal.os.OperatingSystem.current().isLinux
val appImageArch = when (System.getProperty("os.arch")) {
    "aarch64", "arm64" -> "aarch64"
    else -> "x86_64"
} // matches the runner arch automatically (x86_64 runner -> x86_64 AppImage)
val appImageVersion = ((findProperty("appVersion") as String?) ?: version.toString())
    .removePrefix("v").removeSuffix("-SNAPSHOT")
val appimagetoolVersion = "1.9.1"
val appimagetoolUrl =
    "https://github.com/AppImage/appimagetool/releases/download/$appimagetoolVersion/appimagetool-$appImageArch.AppImage"

tasks.register("prepareAppDir") {
    group = "distribution"
    description = "Assembles the AppDir wrapping the jpackage app-image."
    dependsOn("jpackageImage")
    onlyIf { isLinuxHost }

    doLast {
        val appImage = layout.buildDirectory.dir("jpackage/lotp-desktop").get().asFile
        require(appImage.isDirectory) { "jpackage app-image missing: $appImage" }
        val appDir = layout.buildDirectory.dir("appimage/lotp-desktop.AppDir").get().asFile
        delete(appDir)
        copy {
            from(appImage)
            into(appDir)
        }
        // AppRun: AppImages execute this; $APPDIR is set by the runtime.
        appDir.resolve("AppRun").writeText(
            "#!/bin/sh\n" + "exec \"\$APPDIR/bin/lotp-desktop\" \"\$@\"\n"
        )
        appDir.resolve("AppRun").setExecutable(true)
        appDir.resolve("lotp-desktop.desktop").writeText(
            """
            [Desktop Entry]
            Name=lotp Desktop
            Exec=lotp-desktop
            Icon=lotp-desktop
            Type=Application
            Categories=Utility;
            Comment=lotp desktop client
            """.trimIndent() + "\n"
        )
        val icon = projectDir.resolve("src/main/resources/icon.png")
        require(icon.isFile) { "Missing icon: $icon" }
        icon.copyTo(appDir.resolve("lotp-desktop.png"))
        icon.copyTo(appDir.resolve(".DirIcon"))
    }
}

tasks.register<Exec>("appImage") {
    group = "distribution"
    description = "Builds build/appimage/lotp-desktop-<version>-<arch>.AppImage (downloads appimagetool once, Linux only)."
    dependsOn("prepareAppDir")
    onlyIf { isLinuxHost }

    doFirst {
        val toolsDir = layout.buildDirectory.dir("appimage/tools").get().asFile.apply { mkdirs() }
        val tool = toolsDir.resolve("appimagetool-$appImageArch.AppImage")
        if (!tool.isFile) {
            logger.lifecycle("Downloading appimagetool $appimagetoolVersion ...")
            URI(appimagetoolUrl).toURL().openStream().use { input ->
                tool.outputStream().use { output -> input.copyTo(output) }
            }
            tool.setExecutable(true)
        }
        val appDir = layout.buildDirectory.dir("appimage/lotp-desktop.AppDir").get().asFile
        val outFile = layout.buildDirectory
            .dir("appimage").get().asFile
            .resolve("lotp-desktop-$appImageVersion-$appImageArch.AppImage")
        outFile.delete()

        executable = tool.absolutePath
        args = listOf(appDir.absolutePath, outFile.absolutePath)
        environment("ARCH", appImageArch)
        // Lets appimagetool run where FUSE mounts are unavailable.
        environment("APPIMAGE_EXTRACT_AND_RUN", "1")
    }
}
