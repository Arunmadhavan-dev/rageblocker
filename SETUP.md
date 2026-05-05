# RageBlocker - Quick Setup Guide

## 🚀 Quick Start

1. **Open Android Studio**
2. **Import Project**: Select this directory
3. **Sync Gradle**: Let Android Studio download dependencies
4. **Connect Device**: Enable USB debugging
5. **Run App**: Click the green play button

## 📋 Required Permissions Setup

The app will automatically guide you through these permissions:

### 1. Usage Stats Access
- Go to Settings → Apps → RageBlocker → Usage access
- Enable the toggle

### 2. Overlay Permission  
- Go to Settings → Apps → RageBlocker → Display over other apps
- Enable the toggle

### 3. Battery Optimization (Recommended)
- Go to Settings → Apps → RageBlocker → Battery
- Select "Don't optimize"

## ⚡ First Time Use

1. **Select Apps**: Choose which apps you want to monitor
2. **Set Time Limit**: Default is 240 minutes (4 hours)
3. **Start Monitoring**: Toggle the switch to begin
4. **Test It**: Open a monitored app and wait for the rage!

## 🔧 Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Clean build
./gradlew clean
```

## 🐛 Common Issues

**Service not starting?**
- Check all permissions are granted
- Disable battery optimization
- Restart device

**Overlay not showing?**
- Verify overlay permission is granted
- Check if other overlay apps are blocking
- Test on different Android versions

**Usage stats not working?**
- Grant usage access permission  
- Wait a few minutes for stats to populate
- Check device usage restrictions

## 📱 Tested Devices

- ✅ Pixel 4a (Android 13)
- ✅ Samsung S21 (Android 12)
- ✅ OnePlus 9 (Android 11)
- ✅ Nokia 7.2 (Android 10)

---

**Built to annoy you into productivity.** 💢
