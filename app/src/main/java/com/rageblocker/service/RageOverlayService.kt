package com.rageblocker.service

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.rageblocker.utils.UsageStatsManagerWrapper
import java.util.Random
import java.util.Timer
import java.util.TimerTask

class RageOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private var tvRageMessage: TextView? = null
    private var tvBlockedAppName: TextView? = null
    private var btnDone: Button? = null

    private var tvUsageTime: TextView? = null

    private var blockedAppName: String = ""
    private var blockedPackageName: String = ""
    private var usageMinutes: Int = 0
    private var doneClickCount: Int = 0
    private val requiredClicks = 3

    private var blastContainer: FrameLayout? = null

    private val handler = Handler(Looper.getMainLooper())
    private var messageTimer: Timer? = null
    private var vibrationTimer: Timer? = null
    private var blastTimer: Timer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var scaleAnimator: ObjectAnimator? = null
    private var shakeAnimator: ObjectAnimator? = null
    private val random = Random()

    private val blastColors = intArrayOf(
        Color.RED, Color.YELLOW, Color.WHITE,
        Color.parseColor("#FF4444"), Color.parseColor("#FF6600"),
        Color.parseColor("#FF0066"), Color.parseColor("#FFFF00")
    )

    private val rageMessages = arrayOf(
        "YOU WASTED %s ON THIS GARBAGE.",
        "THIS IS WHY YOUR LIFE IS STUCK.",
        "CLOSE THIS NOW.",
        "%s OF YOUR LIFE. GONE. FOREVER.",
        "YOUR ADDICTION IS SHOWING.",
        "IS THIS REALLY HOW YOU WANT TO LIVE?",
        "GO OUTSIDE. TOUCH GRASS.",
        "YOUR FUTURE SELF HATES YOU.",
        "%s TODAY. ARE YOU PROUD?",
        "SCROLLING YOUR LIFE AWAY.",
        "WAKE UP. THIS IS PATHETIC.",
        "PUT THE PHONE DOWN.",
        "YOU'RE BETTER THAN THIS.",
        "STOP WASTING YOUR LIFE.",
        "DO SOMETHING PRODUCTIVE."
    )

    companion object {
        private const val TAG = "RageOverlayService"

        @Volatile
        var isShowing = false
            private set

        fun show(context: Context, appName: String, packageName: String, usageMin: Int) {
            if (isShowing) return
            val intent = Intent(context, RageOverlayService::class.java).apply {
                putExtra("app_name", appName)
                putExtra("package_name", packageName)
                putExtra("usage_minutes", usageMin)
            }
            context.startService(intent)
        }

        fun dismiss(context: Context) {
            context.stopService(Intent(context, RageOverlayService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        blockedAppName = intent?.getStringExtra("app_name") ?: ""
        blockedPackageName = intent?.getStringExtra("package_name") ?: ""
        usageMinutes = intent?.getIntExtra("usage_minutes", 0) ?: 0

        if (overlayView == null) {
            showOverlay()
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        isShowing = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        overlayView = buildOverlayView()

        try {
            windowManager?.addView(overlayView, params)
            // Make it focusable so "I'M DONE" button works
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            windowManager?.updateViewLayout(overlayView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
            isShowing = false
            stopSelf()
            return
        }

        startRage()
    }

    private fun buildOverlayView(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        // Blast text layer - behind the main UI
        blastContainer = FrameLayout(this)
        root.addView(blastContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(64), dp(32), dp(64))
        }

        tvBlockedAppName = TextView(this).apply {
            text = blockedAppName.uppercase()
            setTextColor(Color.RED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        container.addView(tvBlockedAppName, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        tvUsageTime = TextView(this).apply {
            text = formatUsageDisplay()
            setTextColor(Color.RED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
        }
        container.addView(tvUsageTime, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(48) })

        tvRageMessage = TextView(this).apply {
            text = ""
            setTextColor(Color.RED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 42f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        container.addView(tvRageMessage, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { gravity = Gravity.CENTER })

        btnDone = Button(this).apply {
            text = "I'M DONE"
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.RED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { onDoneClicked() }
        }
        container.addView(btnDone, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(32) })

        root.addView(container, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))

        return root
    }

    private fun startRage() {
        setRandomRageMessage()
        startTextAnimation()
        startMessageRotation()
        startBlastTexts()
        startVibration()
        startAlarmSound()
        startTimeUpdates()
    }

    private fun startTextAnimation() {
        tvRageMessage?.let { tv ->
            scaleAnimator = ObjectAnimator.ofFloat(tv, "scaleX", 0.8f, 1.2f, 1.0f).apply {
                duration = 500
                interpolator = BounceInterpolator()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                start()
            }
            shakeAnimator = ObjectAnimator.ofFloat(tv, "translationX", -20f, 20f, -20f).apply {
                duration = 200
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                start()
            }
        }
    }

    private fun startMessageRotation() {
        messageTimer = Timer()
        messageTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post { setRandomRageMessage() }
            }
        }, 3000, 3000)
    }

    private fun startBlastTexts() {
        blastTimer = Timer()
        blastTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post { spawnBlastText() }
            }
        }, 500, 400)
    }

    private fun spawnBlastText() {
        val container = blastContainer ?: return
        val msg = getRandomRageText()

        val tv = TextView(this).apply {
            text = msg
            setTextColor(blastColors[random.nextInt(blastColors.size)])
            setTextSize(TypedValue.COMPLEX_UNIT_SP, (16 + random.nextInt(32)).toFloat())
            typeface = Typeface.DEFAULT_BOLD
            alpha = 0.7f + random.nextFloat() * 0.3f
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        container.addView(tv, params)

        // Position randomly after layout
        tv.post {
            val maxX = (container.width - tv.width).coerceAtLeast(1)
            val maxY = (container.height - tv.height).coerceAtLeast(1)
            tv.x = random.nextInt(maxX).toFloat()
            tv.y = random.nextInt(maxY).toFloat()
            tv.rotation = -45f + random.nextFloat() * 90f
            tv.pivotX = tv.width / 2f
            tv.pivotY = tv.height / 2f

            // Scale-in animation
            tv.scaleX = 0f
            tv.scaleY = 0f
            tv.animate()
                .scaleX(1f).scaleY(1f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(BounceInterpolator())
                .start()

            // Fade out and remove after a delay
            handler.postDelayed({
                tv.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction {
                        try { container.removeView(tv) } catch (_: Exception) {}
                    }
                    .start()
            }, 1500 + random.nextInt(1500).toLong())
        }

        // Cap max blast texts on screen
        if (container.childCount > 15) {
            try { container.removeViewAt(0) } catch (_: Exception) {}
        }
    }

    private fun getRandomRageText(): String {
        val msg = rageMessages[random.nextInt(rageMessages.size)]
        return if (msg.contains("%s")) {
            String.format(msg, formatUsageShort())
        } else {
            msg
        }
    }

    private fun formatUsageDisplay(): String {
        val hours = usageMinutes / 60
        val mins = usageMinutes % 60
        return if (hours > 0) "${hours}h ${mins}m wasted today" else "${mins}m wasted today"
    }

    private fun formatUsageShort(): String {
        val hours = usageMinutes / 60
        val mins = usageMinutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins} MINUTES"
    }

    private fun setRandomRageMessage() {
        val msg = rageMessages[random.nextInt(rageMessages.size)]
        tvRageMessage?.text = if (msg.contains("%s")) {
            String.format(msg, formatUsageShort())
        } else {
            msg
        }
    }

    private fun startVibration() {
        vibrationTimer = Timer()
        vibrationTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post { vibrate() }
            }
        }, 0, 3000)
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (alarmUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    setDataSource(this@RageOverlayService, alarmUri)
                    isLooping = true
                    setVolume(1.0f, 1.0f)
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        try { mp.start() } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm", e)
        }
    }

    private fun startTimeUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isShowing) return
                try {
                    val currentUsage = UsageStatsManagerWrapper.getTodayUsageStats(
                        this@RageOverlayService, blockedPackageName
                    )
                    usageMinutes = UsageStatsManagerWrapper.getUsageTimeInMinutes(currentUsage)
                    tvUsageTime?.text = formatUsageDisplay()
                } catch (_: Exception) {}
                handler.postDelayed(this, 5000)
            }
        })
    }

    private val doneTaunts = arrayOf(
        "YOU THINK IT'S THAT EASY?",
        "KEEP CLICKING. I DARE YOU.",
        "NOT YET. THINK ABOUT WHAT YOU DID.",
        "ONE MORE TIME...",
        "ARE YOU SURE YOU LEARNED YOUR LESSON?"
    )

    private fun onDoneClicked() {
        doneClickCount++
        
        if (doneClickCount < requiredClicks) {
            // Taunt the user
            tvRageMessage?.text = doneTaunts[random.nextInt(doneTaunts.size)]
            vibrate()
            btnDone?.let { btn ->
                ObjectAnimator.ofFloat(btn, "translationX", -10f, 10f, -10f).apply {
                    duration = 100; repeatCount = 5; repeatMode = ValueAnimator.RESTART; start()
                }
            }
            btnDone?.text = "I'M DONE (${requiredClicks - doneClickCount} more)"
            return
        }
        
        // After enough clicks, dismiss
        cleanup()
        stopSelf()
    }

    private fun cleanup() {
        isShowing = false
        messageTimer?.cancel()
        vibrationTimer?.cancel()
        blastTimer?.cancel()
        scaleAnimator?.cancel()
        shakeAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
        try { windowManager?.removeView(overlayView) } catch (_: Exception) {}
        overlayView = null
        UsageMonitorService.instance?.onOverlayDismissed()
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
        ).toInt()
    }
}
