# RageBlocker 📱💢

**The only screen-time app that bullies you back.**

RageBlocker is an aggressive Android app that monitors your app usage and blocks you with full-screen shame overlays when you exceed your daily time limits.

## � Demo Video

Watch the demo on X:

[https://x.com/SelfdotInit/status/2027087556714828152/video/1](https://x.com/SelfdotInit/status/2027087556714828152/video/1)

## �🎯 Core Features

- **Real-time Usage Monitoring**: Uses `UsageStatsManager` to track app usage every 10 seconds
- **Aggressive Blocking**: Full-screen overlay with animated red text that shames you
- **Multi-sensory Assault**: Vibration every 3 seconds + loud alarm sounds
- **Rotating Shame Messages**: 10 different messages that rotate every 3 seconds
- **Auto-reset**: Daily usage automatically resets at midnight
- **Persistent Service**: Survives reboots and app kills

## 🔥 Rage Messages

When you exceed your limit, you'll see messages like:
- "YOU USED THIS FOR 4 HOURS."
- "THIS IS WHY YOUR LIFE IS STUCK."
- "CLOSE THIS NOW."
- "YOUR ADDICTION IS SHOWING."
- "GO OUTSIDE. TOUCH GRASS."
- And more...

## 🛠 Technical Architecture

### Core Components
- **MainActivity**: App selection and settings UI
- **UsageMonitorService**: Background monitoring service
- **RageOverlayActivity**: Full-screen blocking overlay
- **UsageStatsManagerWrapper**: Usage stats API wrapper
- **PreferencesRepository**: SharedPreferences management

### Key Technologies
- **Kotlin**: Modern Android development
- **MVVM Architecture**: Clean separation of concerns
- **Foreground Service**: Persistent background monitoring
- **System Overlay**: TYPE_APPLICATION_OVERLAY for blocking
- **UsageStatsManager**: Official Android usage tracking

## 📋 Requirements

- **Android SDK**: API 29+ (Android 10+)
- **Permissions Required**:
  - `PACKAGE_USAGE_STATS` - Usage access
  - `SYSTEM_ALERT_WINDOW` - Overlay permission
  - `FOREGROUND_SERVICE` - Background monitoring
  - `VIBRATE` - Haptic feedback
  - `POST_NOTIFICATIONS` - Service notification

## 🚀 Installation

### 1. Clone & Build
```bash
git clone <repository-url>
cd RageBlocker
./gradlew assembleDebug
```

### 2. Install APK
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Grant Permissions
The app will guide you through granting these permissions:
1. **Usage Stats Access**: Settings → Apps → RageBlocker → Usage access
2. **Overlay Permission**: Settings → Apps → RageBlocker → Display over other apps
3. **Battery Optimization**: Disable for reliable monitoring

## ⚙️ Usage

1. **Select Apps**: Choose which apps to monitor from the list
2. **Set Time Limits**: Default is 240 minutes (4 hours) per app
3. **Start Monitoring**: Toggle the monitoring switch
4. **Get Blocked**: When you exceed the limit, the rage overlay appears

## 🎨 UI/UX Design

- **Dark Theme**: Minimal, aggressive design
- **Bold Typography**: Large, attention-grabbing text
- **Red Color Scheme**: Warning/shame color psychology
- **Full-screen Blocking**: No escape until you close the target app

## 🔧 Configuration

### Time Limits
- Default: 240 minutes (4 hours)
- Per-app customization
- Daily reset at midnight

### Monitoring Interval
- Checks usage every 10 seconds
- Balances battery usage vs responsiveness

### Overlay Behavior
- Full-screen black background
- Animated red text (shake + zoom)
- Vibration every 3 seconds
- Alarm sound loop
- Must exit target app to dismiss

## 🧠 Advanced Features

### Service Persistence
- Auto-starts on boot
- Survives app kills
- Restarts after system updates

### Edge Case Handling
- Permission denial recovery
- Battery optimization exemption
- System UI compatibility
- Multi-window support

### Optional Enhancements
- Camera selfie mode (behind shame text)
- Real-time usage counter
- Streak counter (days under limit)
- Usage summary notifications

## 🐛 Troubleshooting

### Service Not Starting
- Check all permissions are granted
- Disable battery optimization
- Restart device

### Overlay Not Showing
- Verify overlay permission
- Check if other overlay apps are blocking
- Test on different Android versions

### Usage Stats Not Working
- Grant usage access permission
- Wait a few minutes for stats to populate
- Check if device has usage restrictions

## 📱 Compatibility

- **Minimum**: Android 10 (API 29)
- **Target**: Android 14 (API 34)
- **Tested**: Various Android 10-14 devices

## 🛡 Privacy & Security

- **No Data Collection**: All data stored locally
- **No Internet Access**: App works completely offline
- **Minimal Permissions**: Only requests what's necessary
- **Open Source**: Fully transparent codebase

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

MIT License - feel free to use, modify, and distribute.

## 🎯 Market Potential

This could be marketed as:
- "The anti-doomscroll app"
- "Digital wellness with tough love"
- "For people who hate themselves enough to try this"

## 🔮 Future Roadmap

- [ ] Widget for quick monitoring toggle
- [ ] Weekly/monthly usage reports
- [ ] Social sharing of "shame screenshots"
- [ ] AI-powered personalized shame messages
- [ ] Integration with digital wellbeing APIs
- [ ] Wear OS companion app

---

**Built with rage and frustration.** 💢

*Disclaimer: This app is designed to be annoying. Use at your own risk.*
