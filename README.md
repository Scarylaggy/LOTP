# LOTP - Lord of the Plugins

A simple but overcomplicated desktop app for finding and managing plugins for *The Lord of the Rings Online*.
Browse the plugin catalog, install plugins into your LOTRO `Plugins` folder, and keep
track of what's installed.
This mostly exists, because the age old Version of the Plugin Compendium, that is Linux compatible just didn't feel right to me.

App is based on Java because i felt like it, but no Java installation needed: every download below is self-contained (app + runtime).

## Download

Get the latest release from the
[Releases page](https://github.com/Scarylaggy/LOTP/releases).

| System  | File                          | Notes                                     |
|---------|-------------------------------|-------------------------------------------|
| Linux   | `LOTP-<ver>-linux-x86_64.deb`    | Installer (Debian/Ubuntu and derivatives) |
| Linux   | `LOTP-<ver>-linux-x86_64.AppImage` | Portable single file, no install needed  |
| Windows | `LOTP-<ver>-windows-x86_64.zip`  | Portable: unzip and run                   |

## Install & run

### Linux (.deb)

```bash
sudo apt install ./LOTP-<ver>-linux-x86_64.deb
LOTP
```

### Linux (AppImage)

```bash
chmod +x LOTP-<ver>-linux-x86_64.AppImage
./LOTP-<ver>-linux-x86_64.AppImage
```

No root required. Your settings live in `~/.lotp/`.

### Windows (zip)

1. Unzip the folder anywhere (e.g. `Documents`).
2. Run `LOTP\LOTP.exe`.

No installation, no admin rights needed.

#### "Windows protected your PC" (SmartScreen)

This is a free, unsigned open-source App, so Windows doesn't recognize the
publisher and shows a warning. To proceed:

1. Click **More info**.
2. Click **Run anyway**.

The warning appears once per download. 
(Removing it entirely would require a paid code-signing
certificate, which this project doesn't have.)

## Usage

1. **Find your Plugins folder automatically.** On first start the app looks in the
   usual places:
   - Windows: `Documents\The Lord of the Rings Online\Plugins`
   - Steam/Proton and Wine prefixes on Linux
2. **Override it if needed.** If auto-detection misses (custom install, unusual
   prefix), point the app at your `Plugins` folder in the settings and it
   remembers your choice.
3. **Browse and install.** Pick plugins from the catalog to download them into
   your `Plugins` folder; the table shows what's installed.
4. **Theme.** Switch between light and dark in the UI; your choice is remembered,
   along with the window size and position.

Settings are stored as plain YAML in the config folder (`~/.lotp/`
on Linux, `%USERPROFILE%\.lotp\` on Windows).

## Building from source

Needs JDK 25 and nothing else:

```bash
./gradlew :desktop:run            # run directly
./gradlew :desktop:build          # compile + unit tests
./gradlew :desktop:jpackageImage  # self-contained app image for your OS
./gradlew :desktop:appImage       # AppImage (Linux only)
```

Tagged `v*` pushes build everything via GitHub Actions (Linux + Windows) and
publish a release automatically.

## Issues

Something broken or misdetected? Open an issue with your OS, the app version,
and (for folder-detection problems) where your LOTRO `Plugins` folder lives.
