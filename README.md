# AppLock for Android 

Have you ever handed your phone to a friend to show them a photo, only to watch them immediately swipe into your messages or photos? 

**AppLock** is a lightweight, secure Android application designed to put a stop to that. It allows users to selectively lock individual applications on their device, requiring biometric authentication (like a fingerprint or face scan) before the app can be opened. 

## Key Features

* **Biometric Security:** Leverages Android's native `BiometricPrompt` API for seamless, hardware-level fingerprint and face unlock.
* **Zero-Lag Detection:** Uses Android's `AccessibilityService` to instantly detect when a protected app is launched, intercepting the view before sensitive data is exposed.
* **Tamper Resistance:** Integrates `DeviceAdminReceiver` policies to prevent unauthorized users from easily force-stopping or uninstalling the app locker to bypass security.
* **Clean UI:** A simple, intuitive `AppListActivity` to quickly toggle protection on or off for any installed application.

## Built With

* **Language:** Kotlin
* **Architecture:** Modern Android UI components 
* **Core APIs:** * `AccessibilityService` (for package detection)
  * `Biometric API` (for authentication)
  * `Device Administration API` (for tamper protection)

## How It Works Under the Hood

Building a reliable app locker on modern Android versions is notoriously difficult due to strict background execution limits. Instead of using a battery-draining background thread that constantly polls the "recent apps" list, this app takes a more efficient route:

1. The user grants the app **Accessibility permissions**.
2. The `AppLockAccessibilityService` listens quietly for window state changes.
3. When it detects the package name of a locked app coming to the foreground, it instantly launches `LockScreenActivity` over it.
4. If the biometric authentication succeeds, the lock screen finishes, and the user continues to their app.

## Installation

You can download and install the app directly to your Android device:

1. Go to the **Releases** section on the right side of this GitHub page (or click [here](https://github.com/Santosh-Gollapudi/App_Lock/releases/tag/v1)).
2. Under **Assets**, click on `AppLock-v1.0.apk` to download it.
3. After downloading, Android may not allow you to install it. For that, I suggest you to **turn off Play Protect temporarily** in you Play Store and can turn it on after installation. I promise you it wont harm you device or data.
4. Open the downloaded file on your phone to install it. *(Note: You may need to allow "Install from Unknown Sources" in your browser or file manager settings).*
5. **Crucial Step:** When you first launch the app, you will be prompted to enable **Accessibility Services** and **Device Admin** privileges. The app cannot function without these!

## Privacy Disclaimer

This app requires highly elevated system permissions (Accessibility and Device Admin) strictly to monitor app launches and prevent uninstallation. It does **not** connect to the internet, and it does **not** collect, store, or transmit any of your personal data or app usage history.

## Contributing

Contributions, bug reports, and pull requests are always welcome!

## Disclaimer!
There maybe some bugs and errors. I am still working on it.
Thankyou for your understanding
