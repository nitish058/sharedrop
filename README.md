# ShareDrop

ShareDrop is a local file sharing app that works across Android, Mac, Windows, Linux, iOS, and Web. No internet, no account, no cloud — just connect to the same WiFi and share files directly between your devices.

It's built with Kotlin Multiplatform and Compose Multiplatform, so most of the code is written once and runs everywhere.

---

## Screenshots

| Desktop | Android |
|---------|---------|
| ![Desktop App](docs/screenshots/ShareDrop_Mac_v1.png) | ![Android App](docs/screenshots/ShareDrop_Android_V1.jpg) |

---

## Why I built this

I wanted to send files between my Mac and Android phone without using Google Drive or a USB cable every time. Existing apps either required an account or felt overly complicated for something this simple. So I built ShareDrop as a learning project while exploring Kotlin Multiplatform — and it actually works.

---

## How it works

Discovery and transfer are both pretty straightforward.

For discovery, every device running the app sends a small UDP broadcast packet every 2 seconds. Any other device on the same network that's listening on port 8888 picks it up and shows that device in the list.

For transfer, once you select a device and pick a file, the app opens a direct TCP connection to that device on port 8080 and sends the file bytes. The receiving device saves it straight to the Downloads folder.

```
Sender                            Receiver
  |                                   |
  |  UDP: "SHAREDROP:Mac-nitish:8080" |
  | --------------------------------> | (discovery, every 2s)
  |                                   |
  |  TCP: filename + file bytes       |
  | --------------------------------> | (transfer)
  |                                   | saves to Downloads
```

Nothing goes through a server. The two devices talk directly to each other.

---

## Platform support

| Platform | Status |
|----------|--------|
| Android | Working |
| macOS | Working |
| Windows | Working |
| Linux | Working |
| iOS | In progress |
| Web | In progress |

Desktop (Mac, Windows, Linux) all run through the same JVM target, so if it works on one it works on all three.

---

## Tech stack

- Kotlin Multiplatform
- Compose Multiplatform
- UDP sockets for device discovery
- TCP sockets for file transfer
- No third party networking libraries

---

## Getting started

### What you need

- Android Studio (latest version)
- Xcode 15 or later (only needed for iOS or macOS builds)
- JDK 17 or higher
- Kotlin Multiplatform plugin in Android Studio

### Clone and open

```bash
git clone https://github.com/nitish058/ShareDrop.git
cd ShareDrop
```

Open the project in Android Studio and let Gradle sync finish.

### Run on Desktop (Mac, Windows, Linux)

Create a run configuration in Android Studio:

- Type: Gradle
- Name: Desktop
- Run: `hotRunJvm -DmainClass=org.nitish.project.sharedrop.MainKt`
- Gradle project: ShareDrop (root)

Hit Run.

### Run on Android

Either run directly from Android Studio with a connected device, or build manually:

```bash
./gradlew assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Both devices need to be on the same WiFi network for discovery to work.

---

## Project structure

```
ShareDrop/
├── composeApp/
│   └── src/
│       ├── commonMain/       # Shared code — UI, interfaces, data models
│       │   └── kotlin/
│       │       ├── App.kt
│       │       ├── DeviceDiscovery.kt
│       │       ├── DeviceAdvertiser.kt
│       │       ├── FileReceiver.kt
│       │       ├── FileSender.kt
│       │       ├── FilePicker.kt
│       │       └── FileSaver.kt
│       ├── androidMain/      # Android implementations
│       ├── jvmMain/          # Desktop implementations (Mac, Windows, Linux)
│       ├── iosMain/          # iOS (work in progress)
│       ├── jsMain/           # Web JS (work in progress)
│       └── wasmJsMain/       # WebAssembly (work in progress)
└── iosApp/                   # iOS app entry point
```

The pattern used throughout is Kotlin's `expect/actual` — `commonMain` defines what each feature should do, and each platform folder provides the actual implementation. This keeps platform-specific code isolated and the shared code clean.

---

## Known issues

- Both devices must be on the same WiFi network. It won't work over mobile data or across different networks.
- iOS and Web are not fully working yet.
- There's no encryption on the transfer right now. Anyone on the same network running the app could receive files sent to them. Encryption is planned.
- Very large files may be slow depending on your network.

---

## Roadmap

- Send multiple files at once
- Encrypted transfer
- iOS support
- Web support
- Choose where received files are saved
- Send text/clipboard content directly

---

## Contributing

If you want to contribute, you're welcome to. The codebase is relatively small and easy to navigate.

Good places to start:

- Implement iOS discovery using the Network framework or Bonjour
- Add a screen that shows transfer history
- Dark mode
- Any of the items in the roadmap above

To contribute:

1. Fork the repo
2. Create a branch: `git checkout -b feature/what-youre-adding`
3. Make your changes and test on at least one platform
4. Open a pull request with a clear description of what you changed and why

For bigger changes, open an issue first so we can discuss before you spend time on it.

---

## Inspiration

Built with inspiration from [LocalSend](https://github.com/localsend/localsend), which does something similar and does it really well. ShareDrop is a from-scratch Kotlin Multiplatform take on the same idea.

---

## Author

Nitish — [github.com/nitish058](https://github.com/nitish058)

---

## License

MIT — see [LICENSE](LICENSE) for details.
