package com.rageblocker.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rageblocker.R
import com.rageblocker.service.UsageMonitorService
import com.rageblocker.utils.UsageStatsManagerWrapper
import java.util.Random
import java.util.Timer
import java.util.TimerTask

class RageOverlayActivity : AppCompatActivity() {

    private lateinit var tvRageMessage: TextView
    private lateinit var tvBlockedAppName: TextView
    private lateinit var tvUsageTime: TextView
    private lateinit var btnDone: View
    private lateinit var vibrationIndicator: View
    private lateinit var blastContainer: FrameLayout

    private var appName: String = ""
    private var blockedPackageName: String = ""
    private var usageMinutes: Int = 0
    private var doneClickCount: Int = 0
    private val requiredClicks = 3

    private val handler = Handler(Looper.getMainLooper())
    private var messageTimer: Timer? = null
    private var vibrationTimer: Timer? = null
    private var blastTimer: Timer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var animator: ObjectAnimator? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up fullscreen overlay
        setupWindowFlags()
        
        setContentView(R.layout.activity_rage_overlay)
        
        // Get intent data
        getIntentData()
        
        // Initialize views
        initViews()
        
        // Start the rage experience
        startRageExperience()
    }

    private fun setupWindowFlags() {
        window.apply {
            addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attributes.layoutInDisplayCutoutMode = 
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        
        // Hide system UI
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    private fun getIntentData() {
        appName = intent.getStringExtra("app_name") ?: ""
        blockedPackageName = intent.getStringExtra("package_name") ?: ""
        usageMinutes = intent.getIntExtra("usage_minutes", 0)
    }

    private fun initViews() {
        tvRageMessage = findViewById(R.id.tvRageMessage)
        tvBlockedAppName = findViewById(R.id.tvBlockedAppName)
        tvUsageTime = findViewById(R.id.tvUsageTime)
        btnDone = findViewById(R.id.btnDone)
        vibrationIndicator = findViewById(R.id.vibrationIndicator)
        blastContainer = findViewById(R.id.blastContainer)

        // Set initial text
        tvBlockedAppName.text = appName.uppercase()
        tvUsageTime.text = formatUsageDisplay()
        
        // Set initial rage message
        setRandomRageMessage()
        
        // Set up button click
        btnDone.setOnClickListener {
            onDoneClicked()
        }
    }

    private fun startRageExperience() {
        // Start text animations
        startTextAnimation()
        
        // Start message rotation
        startMessageRotation()
        
        // Start vibration
        startVibration()
        
        // Start alarm sound
        startAlarmSound()
        
        // Start usage time updates
        startTimeUpdates()
        
        // Start blast text animation
        startBlastTexts()
    }

    private fun startTextAnimation() {
        animator = ObjectAnimator.ofFloat(tvRageMessage, "scaleX", 0.8f, 1.2f, 1.0f).apply {
            duration = 500
            interpolator = BounceInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
        
        // Add shake animation
        val shakeAnimator = ObjectAnimator.ofFloat(tvRageMessage, "translationX", -20f, 20f, -20f).apply {
            duration = 200
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    private fun startMessageRotation() {
        messageTimer = Timer()
        messageTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    setRandomRageMessage()
                    // Trigger a quick animation
                    animator?.cancel()
                    animator = ObjectAnimator.ofFloat(tvRageMessage, "scaleX", 0.8f, 1.2f, 1.0f).apply {
                        duration = 300
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                }
            }
        }, 3000, 3000) // Change message every 3 seconds
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
        tvRageMessage.text = if (msg.contains("%s")) {
            String.format(msg, formatUsageShort())
        } else {
            msg
        }
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

        blastContainer.addView(tv, params)

        tv.post {
            val maxX = (blastContainer.width - tv.width).coerceAtLeast(1)
            val maxY = (blastContainer.height - tv.height).coerceAtLeast(1)
            tv.x = random.nextInt(maxX).toFloat()
            tv.y = random.nextInt(maxY).toFloat()
            tv.rotation = -45f + random.nextFloat() * 90f
            tv.pivotX = tv.width / 2f
            tv.pivotY = tv.height / 2f

            tv.scaleX = 0f
            tv.scaleY = 0f
            tv.animate()
                .scaleX(1f).scaleY(1f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(BounceInterpolator())
                .start()

            handler.postDelayed({
                tv.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction {
                        try { blastContainer.removeView(tv) } catch (_: Exception) {}
                    }
                    .start()
            }, 1500 + random.nextInt(1500).toLong())
        }

        if (blastContainer.childCount > 15) {
            try { blastContainer.removeViewAt(0) } catch (_: Exception) {}
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

    private fun startTimeUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                try {
                    val currentUsage = UsageStatsManagerWrapper.getTodayUsageStats(
                        this@RageOverlayActivity, blockedPackageName
                    )
                    usageMinutes = UsageStatsManagerWrapper.getUsageTimeInMinutes(currentUsage)
                    tvUsageTime.text = formatUsageDisplay()
                } catch (_: Exception) {}
                handler.postDelayed(this, 5000)
            }
        })
    }

    private fun startVibration() {
        vibrationTimer = Timer()
        vibrationTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    vibrate()
                    // Show vibration indicator
                    vibrationIndicator.visibility = View.VISIBLE
                    handler.postDelayed({
                        vibrationIndicator.visibility = View.GONE
                    }, 500)
                }
            }
        }, 0, 3000) // Vibrate every 3 seconds
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    500, // Vibrate for 500ms
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private fun startAlarmSound() {
        try {
            val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            
            if (alarmUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    setDataSource(this@RageOverlayActivity, alarmUri)
                    isLooping = true
                    setVolume(1.0f, 1.0f)
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        try { mp.start() } catch (e: Exception) { /* ignore */ }
                    }
                }
            }
        } catch (e: Exception) {
            // If sound fails, continue without it
            e.printStackTrace()
        }
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
            tvRageMessage.text = doneTaunts[random.nextInt(doneTaunts.size)]
            vibrate()
            
            val shakeAnimator = ObjectAnimator.ofFloat(btnDone, "translationX", -10f, 10f, -10f).apply {
                duration = 100
                repeatCount = 5
                repeatMode = ObjectAnimator.RESTART
                start()
            }
            (btnDone as? android.widget.Button)?.text = "I'M DONE (${requiredClicks - doneClickCount} more)"
            return
        }
        
        // After enough clicks, dismiss
        finishRageOverlay()
    }

    private fun finishRageOverlay() {
        // Stop all timers and animations
        messageTimer?.cancel()
        vibrationTimer?.cancel()
        blastTimer?.cancel()
        animator?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) { /* ignore */ }
        mediaPlayer = null
        
        // Notify service that overlay is dismissed
        UsageMonitorService.instance?.onOverlayDismissed()
        
        finish()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back button - user must use the "I'M DONE" button
        onDoneClicked()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up resources
        messageTimer?.cancel()
        vibrationTimer?.cancel()
        blastTimer?.cancel()
        animator?.cancel()
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-hide system UI when we regain focus
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }
}
