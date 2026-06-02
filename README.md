# BT-KB-Mouse

Professional-grade Bluetooth Mouse & Keyboard control application for Android with a modern, polished UI.

## Features

### Device Management
- Scan for nearby Bluetooth HID devices (mouse + keyboard only)
- Pair / unpair devices
- Save trusted devices list
- Auto-reconnect to last connected device
- Device battery status display
- Connection strength indicator

### Mouse Controls
- Adjustable DPI sensitivity slider
- Scroll speed control
- Left/right-handed mode toggle
- Smooth acceleration toggle
- Custom button mapping (if device supports extra buttons)

### Keyboard Controls
- Key mapping editor
- Layout selection (UK/US/ISO support)
- Function key mode toggle (F1–F12 behavior)
- Media key support (volume, play/pause, etc.)
- Repeat rate + delay configuration

### Performance & Stability
- Low-latency input pipeline
- Background service for persistent connection
- Auto-reconnect on disconnect
- Battery optimization mode

### UI/UX
- Dark theme by default (modern neon/glow style)
- Compact, responsive layout
- Device cards with status indicators
- Smooth animations on connect/disconnect
- Professional "tech utility" look
- Light theme option

## Screenshots

<div align="center">
  <img src="./screenshots/main.png" width="200" alt="Main Screen" />
  <img src="./screenshots/settings.png" width="200" alt="Settings" />
  <img src="./screenshots/about.png" width="200" alt="About" />
</div>

## Version

**v1.0.0** - Built 2026-06-02

Made by jnetai.com

## GitHub Actions Setup

### Required Secrets

Configure these secrets in your GitHub repository under **Settings → Secrets and variables → Actions**:

| Secret Name | Description | How to Get |
|-------------|-------------|-------------|
| `KEYSTORE_BASE64` | Base64 encoded release keystore | `base64 -w 0 app/ciphervault-release-keystore.jks` |
| `KEYSTORE_PASSWORD` | Keystore password | (from password-vault) |
| `KEY_ALIAS` | Key alias name | `ciphervault` |
| `KEY_PASSWORD` | Key password | (from password-vault) |

### Generate Keystore Base64

```bash
# Navigate to project directory
cd /home/jay/Documents/Scripts/AI/Agent-Zero/BT-KB-Mouse

# Generate base64 encoded keystore
base64 -w 0 app/ciphervault-release-keystore.jks
```

Copy the output and add it as the `KEYSTORE_BASE64` secret.

### Workflow Triggers

- **Push to main/master**: Builds both debug and release APKs
- **Pull requests**: Builds debug APK
- **Manual dispatch**: Available via GitHub Actions tab

### Build Outputs

- `debug-apk`: Debug build artifacts (7 days retention)
- `release-apk`: Release signed APK (30 days retention)
- `all-apks`: Combined APK artifacts

## Building Locally

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing.properties)
./gradlew assembleRelease

# Clean build
./gradlew clean
```

## Project Structure

```
BT-KB-Mouse/
├── app/
│   ├── src/main/
│   │   ├── java/com/jnetai/btkbmouse/
│   │   │   ├── BTKBMouseApp.kt          # Application class
│   │   │   ├── data/                    # Data layer (Room, models)
│   │   │   ├── repository/              # Repository layer
│   │   │   ├── service/                 # HID foreground service
│   │   │   └── ui/                      # Activities & ViewModels
│   │   └── res/                         # Resources (layouts, themes, etc.)
│   ├── build.gradle                     # App-level build config
│   └── ciphervault-release-keystore.jks # Release signing keystore
├── .github/workflows/
│   └── build.yml                        # GitHub Actions workflow
├── build.gradle                          # Root build config
├── settings.gradle                       # Project settings
└── gradle.properties                     # Gradle properties
```

## Tech Stack

- **Language**: Kotlin 1.9
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room
- **Async**: Kotlin Coroutines + Flow
- **Preferences**: DataStore
- **UI**: Material Design 3 + View Binding

## License

MIT License - See LICENSE file for details.
