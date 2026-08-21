<div align="center">

# 🛡️ VIGILANT GUARD

**Advanced Cyber-Security Engine for Android**

[![Build Status](https://github.com/PRACHI1284/cyber-security-project-/actions/workflows/build.yml/badge.svg)](https://github.com/PRACHI1284/cyber-security-project-/actions)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat&logo=kotlin)](https://kotlinlang.org/)

</div>

<br/>

VigilantGuard is a next-generation Android security application designed to analyze, detect, and neutralize threats in real-time. Built with Jetpack Compose and modern Android Architecture components, it provides end-to-end local cryptography, heuristic permission analysis, and extreme-speed malware signature scanning.

## 🚀 Features

* **Real-Time Malware Scanning**: Analyzes installed packages using multi-pattern signature detection.
* **Biometric Vault**: Hardware-backed AES-256-GCM encrypted data vault secured with user biometrics.
* **Heuristic Risk Scoring**: Analyzes application permission graphs to dynamically generate risk scores (0-100).
* **Threat Intelligence Dashboard**: Interactive visualizations showing weekly security posture and logs.
* **Fully Local**: 100% offline security scanning ensures privacy and zero data leakage.

## 🧠 Architecture & Algorithms

VigilantGuard prioritizes extreme performance using specialized data structures to minimize battery usage during background scanning.

### 1. Aho-Corasick Automaton
Used as our core **Signature Engine**. Instead of checking thousands of known malicious package signatures one by one, the Aho-Corasick algorithm builds a Trie and failure links to scan the target application against **all** signatures simultaneously in `O(N + M)` time complexity.

### 2. Bloom Filters
Before running the intensive Aho-Corasick automaton, applications are passed through a highly optimized Bloom Filter. This probabilistic data structure quickly discards 99% of safe applications with a time complexity of `O(k)`, saving critical CPU cycles on the device.

### 3. Room Persistence
Security event logs, quarantine history, and heuristic baselines are stored persistently using Android's Room database mapped to an underlying SQLite structure. Reactive data streams (`StateFlow`) ensure the UI remains fully synchronized with the database state.

## 🛠️ Build Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/Detox10/cyber-security-project-.git
   ```
2. Open the project in **Android Studio** (Koala or newer recommended).
3. The project uses standard Android Gradle Plugin (AGP). No local `debug.keystore` is required; Android Studio will automatically generate one for you.
4. Click **Run** (`Shift + F10`) to deploy to your emulator or physical device.

## 🛡️ CI/CD

This project is integrated with **GitHub Actions**. Every push to the `main` branch automatically triggers a fresh Android APK build using JDK 17. You can download the latest compiled `app-debug.apk` directly from the Actions Tab.

---
<div align="center">
<i>Securing the mobile frontier, one device at a time.</i>
</div>
