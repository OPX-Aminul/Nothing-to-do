package com.hairgrowth.os

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.hairgrowth.os.data.Prefs
import com.hairgrowth.os.reminder.ReminderScheduler
import com.hairgrowth.os.ui.Fonts
import com.hairgrowth.os.ui.Palette
import com.hairgrowth.os.ui.Ui
import com.hairgrowth.os.ui.fragments.DashboardFragment
import com.hairgrowth.os.ui.fragments.HomeFragment
import com.hairgrowth.os.ui.fragments.Host
import com.hairgrowth.os.ui.fragments.RemindersFragment
import com.hairgrowth.os.ui.fragments.SettingsFragment
import com.hairgrowth.os.ui.views.BlobView
import com.hairgrowth.os.ui.views.ConfettiView
import com.hairgrowth.os.ui.views.SplashMarkView
import com.hairgrowth.os.util.Fx
import com.hairgrowth.os.util.SoundEngine

class MainActivity : AppCompatActivity(), Host {

    private lateinit var prefs: Prefs
    private lateinit var sounds: SoundEngine
    private var accent = Palette.GOLD

    private var containerId: Int = 0
    private var splashId: Int = 0
    private lateinit var header: LinearLayout
    private lateinit var dock: View
    private lateinit var toast: TextView
    private lateinit var confetti: ConfettiView
    private lateinit var contentRoot: LinearLayout

    private var statusTop = 0
    private var navBottom = 0

    private lateinit var clockView: TextView
    private lateinit var greetWord: TextView
    private val handler = Handler(Looper.getMainLooper())
    /** Empty until the first tab is shown; survives recreation (theme/language switch). */
    private var currentTab = ""

    private val dockButtons = HashMap<String, LinearLayout>()
    private val dockIndicators = HashMap<String, View>()

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            showToast(getString(if (granted) R.string.toast_notif_on else R.string.toast_notif_need))
            ReminderScheduler.sync(this)
        }

    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 15_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Resolve the day/night palette before any view is built.
        Palette.applyConfiguration(resources.configuration)
        prefs = Prefs(this)
        sounds = SoundEngine(this)
        accent = Palette.accent(prefs.accent)
        applySystemBarIcons()

        setContentView(buildRoot())

        ViewCompat.setOnApplyWindowInsetsListener(contentRoot) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            statusTop = bars.top
            navBottom = bars.bottom
            header.updatePadding(
                top = statusTop + Ui.dp(this, 14f),
                bottom = Ui.dp(this, 12f)
            )
            (dock.layoutParams as? FrameLayout.LayoutParams)?.let {
                it.bottomMargin = navBottom + Ui.dp(this, 14f)
                dock.layoutParams = it
            }
            insets
        }

        val restored = savedInstanceState?.getString(KEY_TAB).orEmpty()
        showTab(if (restored.isEmpty()) "home" else restored, animate = false)
        handler.post(clockTick)
        ReminderScheduler.sync(this)
        Handler(Looper.getMainLooper()).postDelayed({ dismissSplash() }, 2000L)
    }

    /** Light icons on the dark surface, dark icons on the light one. */
    private fun applySystemBarIcons() {
        try {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = !Palette.dark
            controller.isAppearanceLightNavigationBars = !Palette.dark
        } catch (_: Throwable) {
        }
    }

    // ---------------- root layout ----------------

    private fun buildRoot(): View {
        val root = FrameLayout(this).apply { setBackgroundColor(Palette.VOID) }

        root.addView(BlobView(this).apply { setAccent(prefs.accent) }, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        contentRoot = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        header = buildHeader()
        contentRoot.addView(header)
        containerId = View.generateViewId()
        contentRoot.addView(
            FrameLayout(this).apply { id = containerId },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        root.addView(contentRoot, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        root.addView(buildSplash(), FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        confetti = ConfettiView(this)
        root.addView(confetti, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        dock = buildDock()
        root.addView(dock, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = Ui.dp(this@MainActivity, 14f)
        })

        toast = TextView(this).apply {
            typeface = Fonts.bn(this@MainActivity)
            textSize = 12.5f
            setTextColor(Palette.TEXT)
            gravity = Gravity.CENTER
            setPadding(
                Ui.dp(this@MainActivity, 18f), Ui.dp(this@MainActivity, 12f),
                Ui.dp(this@MainActivity, 18f), Ui.dp(this@MainActivity, 12f)
            )
            background = Ui.round(Palette.TOAST_BG, Ui.fdp(this@MainActivity, 14f), 1, Palette.LINE_STRONG)
            alpha = 0f
            visibility = View.INVISIBLE
        }
        root.addView(toast, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = Ui.dp(this@MainActivity, 108f)
        })

        return root
    }

    private fun buildHeader(): LinearLayout {
        val ctx = this
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 18f), Ui.dp(ctx, 14f), Ui.dp(ctx, 18f), Ui.dp(ctx, 12f))
        }
        val left = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        clockView = TextView(ctx).apply {
            typeface = Fonts.sansSemi(ctx)
            textSize = 11f
            setTextColor(Palette.MUTED)
            letterSpacing = 0.04f
        }
        val greetRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, Ui.dp(ctx, 2f), 0, 0)
        }
        greetRow.addView(TextView(ctx).apply {
            text = getString(R.string.greeting_prefix)
            typeface = Fonts.bnSemi(ctx)
            textSize = 19f
            setTextColor(Palette.TEXT)
        })
        greetWord = TextView(ctx).apply {
            typeface = Fonts.bnSemi(ctx)
            textSize = 19f
            setTextColor(accent)
        }
        greetRow.addView(greetWord)
        left.addView(clockView)
        left.addView(greetRow)
        row.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val dot = FrameLayout(ctx).apply {
            background = Ui.circle(withAlpha(accent, 0x30), Ui.dp(ctx, 1f), withAlpha(accent, 0x66))
        }
        dot.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_dna)
            imageTintList = ColorStateList.valueOf(accent)
        }, FrameLayout.LayoutParams(Ui.dp(ctx, 18f), Ui.dp(ctx, 18f)).apply {
            gravity = Gravity.CENTER
        })
        row.addView(dot, LinearLayout.LayoutParams(Ui.dp(ctx, 38f), Ui.dp(ctx, 38f)))

        updateClock()
        return row
    }


    private fun buildDock(): View {
        val ctx = this
        val wrap = FrameLayout(ctx)
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 6f), Ui.dp(ctx, 6f), Ui.dp(ctx, 6f), Ui.dp(ctx, 6f))
            background = Ui.round(Palette.DOCK_BG, Ui.fdp(ctx, 26f), 1, Palette.LINE_STRONG)
            elevation = Ui.fdp(ctx, 12f)
        }
        val tabs = listOf(
            Triple("home", R.drawable.ic_home, getString(R.string.nav_home)),
            Triple("dashboard", R.drawable.ic_chart, getString(R.string.nav_dashboard)),
            Triple("reminders", R.drawable.ic_bell, getString(R.string.nav_reminders)),
            Triple("settings", R.drawable.ic_sliders, getString(R.string.nav_settings))
        )
        tabs.forEach { (tab, icon, label) ->
            val item = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, Ui.dp(ctx, 6f), 0, Ui.dp(ctx, 6f))
                isClickable = true
                setOnClickListener {
                    sounds.tap()
                    showTab(tab, animate = true)
                }
            }
            val indicator = View(ctx).apply {
                background = Ui.round(accent, Ui.fdp(ctx, 2f))
                visibility = View.INVISIBLE
            }
            item.addView(indicator, LinearLayout.LayoutParams(Ui.dp(ctx, 24f), Ui.dp(ctx, 3f)))
            val iv = ImageView(ctx).apply {
                setImageResource(icon)
                tag = "icon"
            }
            item.addView(iv, LinearLayout.LayoutParams(Ui.dp(ctx, 20f), Ui.dp(ctx, 20f)).apply {
                topMargin = Ui.dp(ctx, 7f)
            })
            val tv = TextView(ctx).apply {
                text = label
                typeface = Fonts.bn(ctx)
                textSize = 10f
                setTextColor(Palette.MUTED)
                tag = "label"
            }
            item.addView(tv, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = Ui.dp(ctx, 3f) })

            dockButtons[tab] = item
            dockIndicators[tab] = indicator
            bar.addView(item, LinearLayout.LayoutParams(Ui.dp(ctx, 78f), ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        wrap.addView(bar, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        return wrap
    }

    private fun updateDockState() {
        dockButtons.forEach { (tab, item) ->
            val active = tab == currentTab
            val icon = item.findViewWithTag<ImageView>("icon")
            val label = item.findViewWithTag<TextView>("label")
            icon.imageTintList = ColorStateList.valueOf(if (active) accent else Palette.MUTED)
            icon.animate().translationY(if (active) -Ui.fdp(this, 1f) else 0f)
                .scaleX(if (active) 1.08f else 1f)
                .scaleY(if (active) 1.08f else 1f)
                .setDuration(220).start()
            label.setTextColor(if (active) accent else Palette.MUTED)
            dockIndicators[tab]?.visibility = if (active) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun buildSplash(): View {
        val ctx = this
        val wrap = FrameLayout(ctx).apply {
            setBackgroundColor(Palette.VOID)
            id = View.generateViewId()
        }
        splashId = wrap.id
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val mark = SplashMarkView(ctx)
        col.addView(mark, LinearLayout.LayoutParams(Ui.dp(ctx, 130f), Ui.dp(ctx, 130f)))
        col.addView(TextView(ctx).apply {
            text = getString(R.string.app_name)
            typeface = Fonts.display(ctx)
            textSize = 27f
            setTextColor(Palette.GOLD_SOFT)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = Ui.dp(ctx, 20f) })
        col.addView(TextView(ctx).apply {
            text = getString(R.string.splash_sub)
            typeface = Fonts.sans(ctx)
            textSize = 10f
            letterSpacing = 0.34f
            setTextColor(Palette.MUTED)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = Ui.dp(ctx, 8f) })

        val bar = FrameLayout(ctx).apply { background = Ui.round(Palette.TRACK, Ui.fdp(ctx, 2f)) }
        val fill = View(ctx).apply {
            background = Ui.linear(intArrayOf(accent, Palette.ROSE), Ui.fdp(ctx, 2f))
        }
        bar.addView(fill, FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT))
        col.addView(bar, LinearLayout.LayoutParams(Ui.dp(ctx, 160f), Ui.dp(ctx, 2f)).apply {
            topMargin = Ui.dp(ctx, 22f)
        })
        wrap.addView(col, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER })

        mark.start()
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500L
            startDelay = 500L
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                val p = a.animatedValue as Float
                (fill.layoutParams as FrameLayout.LayoutParams).let {
                    it.width = (Ui.dp(ctx, 160f) * p).toInt()
                    fill.layoutParams = it
                }
            }
            start()
        }
        return wrap
    }

    private fun dismissSplash() {
        val splash = findViewById<View>(splashId) ?: return
        splash.animate().alpha(0f).setDuration(600L)
            .withEndAction { splash.visibility = View.GONE }
            .start()
    }

    // ---------------- navigation ----------------

    private fun showTab(tab: String, animate: Boolean) {
        if (tab == currentTab) return
        currentTab = tab
        val fragment: Fragment = when (tab) {
            "dashboard" -> DashboardFragment()
            "reminders" -> RemindersFragment()
            "settings" -> SettingsFragment()
            else -> HomeFragment()
        }
        val host = findViewById<View>(containerId)
        if (animate && host != null) {
            host.alpha = 0f
            host.translationY = Ui.fdp(this, 12f)
        }
        val tx = supportFragmentManager.beginTransaction()
        tx.replace(containerId, fragment)
        if (animate && host != null) {
            tx.runOnCommit {
                host.animate().alpha(1f).translationY(0f).setDuration(320)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
        }
        tx.commit()
        updateDockState()
        Fx.tap(this, 15)
    }

    /** The dock is always visible — scrolling never hides the navigation bar. */
    override fun dockOnScroll(deltaY: Int) {
        // intentionally empty
    }

    override fun goHomeForEditing() {
        showTab("home", animate = true)
    }

    private fun updateClock() {
        val cal = java.util.Calendar.getInstance()
        val hh = String.format("%02d", cal.get(java.util.Calendar.HOUR_OF_DAY))
        val mm = String.format("%02d", cal.get(java.util.Calendar.MINUTE))
        clockView.text = "$hh:$mm"
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        greetWord.text = getString(
            when {
                h in 5..11 -> R.string.greeting_morning
                h in 12..16 -> R.string.greeting_afternoon
                h in 17..20 -> R.string.greeting_evening
                else -> R.string.greeting_night
            }
        )
    }

    // ---------------- Host ----------------

    override fun showToast(msg: String) {
        toast.text = msg
        toast.visibility = View.VISIBLE
        toast.animate().alpha(1f).setDuration(250L).start()
        handler.removeCallbacks(hideToast)
        handler.postDelayed(hideToast, 2600L)
    }

    private val hideToast = Runnable {
        toast.animate().alpha(0f).setDuration(300L)
            .withEndAction { toast.visibility = View.INVISIBLE }.start()
    }

    override fun celebrate() {
        confetti.burst()
    }

    override fun playTone(kind: String) {
        when (kind) {
            "complete" -> sounds.complete()
            "celebrate" -> sounds.celebrate()
            "reminder" -> sounds.playAlarm()
            else -> sounds.tap()
        }
    }

    override fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ReminderScheduler.sync(this)
            showToast(getString(R.string.toast_notif_on))
        }
    }

    override fun openExactAlarmSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:$packageName"))
                )
            } else {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:$packageName"))
                )
            }
        } catch (_: Throwable) {
            showToast(getString(R.string.toast_settings_fail))
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTick)
        sounds.release()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_TAB, currentTab)
        super.onSaveInstanceState(outState)
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)

    private companion object {
        const val KEY_TAB = "hgos_tab"
    }
}
