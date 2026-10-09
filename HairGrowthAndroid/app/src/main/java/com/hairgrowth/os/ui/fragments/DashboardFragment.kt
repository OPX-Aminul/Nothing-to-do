package com.hairgrowth.os.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.hairgrowth.os.R
import com.hairgrowth.os.data.Day
import com.hairgrowth.os.data.Prefs
import com.hairgrowth.os.data.Schedule
import com.hairgrowth.os.data.ScheduleStore
import com.hairgrowth.os.ui.Fonts
import com.hairgrowth.os.ui.Palette
import com.hairgrowth.os.ui.Ui
import com.hairgrowth.os.ui.views.BarsView
import com.hairgrowth.os.ui.views.OrbView
import com.hairgrowth.os.ui.views.RingsView
import com.hairgrowth.os.ui.views.SparkView

class DashboardFragment : Fragment() {

    private lateinit var prefs: Prefs
    private var accent = Palette.GOLD
    private var accentSoft = Palette.GOLD_SOFT
    private lateinit var days: List<Day>
    private var lastScrollY = 0

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        prefs = Prefs(ctx)
        accent = Palette.accent(prefs.accent)
        accentSoft = Palette.accentSoft(prefs.accent)
        days = ScheduleStore(ctx).days()

        val sv = ScrollView(ctx).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, Ui.dp(ctx, 120f))
            setOnScrollChangeListener { _, _, y, _, _ ->
                (activity as? Host)?.dockOnScroll(y - lastScrollY)
                lastScrollY = y
            }
        }
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 18f), 0, Ui.dp(ctx, 18f), Ui.dp(ctx, 24f))
        }
        sv.addView(root)

        val done = prefs.doneCount()
        val total = days.sumOf { it.tasks.size }.coerceAtLeast(1)
        val pctPerDay = days.map { d ->
            Math.round(d.tasks.count { prefs.isDone(it.id) } * 100f / d.tasks.size.coerceAtLeast(1))
        }

        root.addView(buildTitle())
        root.addView(buildHero(done, total))
        root.addView(buildBento(done, total, pctPerDay))
        root.addView(buildSectionHead())
        root.addView(buildBarsPanel(pctPerDay))
        root.addView(buildStreakPanel())
        root.addView(buildRingsPanel(pctPerDay))
        Ui.animIn(root, dy = 10f)
        return sv
    }

    /** Page title with the animated accent underline used across the app. */
    private fun buildTitle(): View {
        val ctx = requireContext()
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, Ui.dp(ctx, 6f), 0, Ui.dp(ctx, 4f))
        }
        col.addView(TextView(ctx).apply {
            text = getString(R.string.nav_dashboard)
            typeface = Fonts.bnSemi(ctx)
            textSize = 22f
            setTextColor(Palette.TEXT)
        })
        col.addView(Ui.underline(ctx, accent), LinearLayout.LayoutParams(
            Ui.dp(ctx, 34f), Ui.dp(ctx, 2.5f)
        ).apply { topMargin = Ui.dp(ctx, 10f) })
        return col
    }

    private fun buildHero(done: Int, total: Int): View {
        val ctx = requireContext()
        val hero = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 220f)
            )
        }
        hero.addView(OrbView(ctx).apply {
            setAccent(prefs.accent)
            setProgress(done / total.toFloat())
        }, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        val center = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        center.addView(TextView(ctx).apply {
            text = "${Math.round(done * 100f / total)}%"
            typeface = Fonts.display(ctx)
            textSize = 42f
            setTextColor(accentSoft)
        })
        center.addView(TextView(ctx).apply {
            text = getString(R.string.dash_weekly)
            typeface = Fonts.bn(ctx)
            textSize = 11f
            setTextColor(Palette.MUTED)
            letterSpacing = 0.14f
        })
        hero.addView(center, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER })
        return hero
    }

    private fun cell(
        kIcon: Int, kText: String, kColor: Int,
        value: String, valueSuffix: String?, sub: String, valueColor: Int
    ): View {
        val ctx = requireContext()
        val cell = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 15f), Ui.dp(ctx, 15f), Ui.dp(ctx, 15f), Ui.dp(ctx, 15f))
            background = Ui.card(ctx, 16f)
            elevation = Ui.fdp(ctx, 4f)
        }
        val kRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        kRow.addView(ImageView(ctx).apply {
            setImageResource(kIcon)
            imageTintList = ColorStateList.valueOf(kColor)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 13f), Ui.dp(ctx, 13f)))
        kRow.addView(TextView(ctx).apply {
            text = "  $kText"
            typeface = Fonts.bn(ctx)
            textSize = 10.5f
            setTextColor(Palette.MUTED)
        })
        cell.addView(kRow)

        val vRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(0, Ui.dp(ctx, 8f), 0, 0)
        }
        vRow.addView(TextView(ctx).apply {
            text = value
            typeface = Fonts.display(ctx)
            textSize = 28f
            setTextColor(valueColor)
        })
        if (valueSuffix != null) {
            vRow.addView(TextView(ctx).apply {
                text = valueSuffix
                typeface = Fonts.bn(ctx)
                textSize = 12f
                setTextColor(Palette.MUTED)
                setPadding(Ui.dp(ctx, 4f), 0, 0, Ui.dp(ctx, 4f))
            })
        }
        cell.addView(vRow)
        cell.addView(TextView(ctx).apply {
            text = sub
            typeface = Fonts.bn(ctx)
            textSize = 10.5f
            setTextColor(Palette.MUTED)
            setPadding(0, Ui.dp(ctx, 4f), 0, 0)
        })
        return cell
    }

    private fun buildBento(done: Int, total: Int, pctPerDay: List<Int>): View {
        val ctx = requireContext()
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val lp1 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp1.marginEnd = Ui.dp(ctx, 5.5f)
        val lp2 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp2.marginStart = Ui.dp(ctx, 5.5f)
        row.addView(
            cell(
                R.drawable.ic_fire, getString(R.string.dash_streak), accent,
                prefs.streak.toString(), " " + getString(R.string.home_week), getString(R.string.dash_streak_sub), accentSoft
            ), lp1
        )
        row.addView(
            cell(
                R.drawable.ic_check_all, getString(R.string.dash_done), Palette.MINT,
                done.toString(), "/$total", getString(R.string.dash_done_sub), Palette.MINT
            ), lp2
        )
        val rowLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        rowLp.bottomMargin = Ui.dp(ctx, 11f)
        col.addView(row, rowLp)

        val sparkCell = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 15f), Ui.dp(ctx, 15f), Ui.dp(ctx, 15f), Ui.dp(ctx, 15f))
            background = Ui.card(ctx, 16f)
            elevation = Ui.fdp(ctx, 4f)
        }
        val kRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        kRow.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_line)
            imageTintList = ColorStateList.valueOf(Palette.ROSE)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 13f), Ui.dp(ctx, 13f)))
        kRow.addView(TextView(ctx).apply {
            text = "  " + getString(R.string.dash_progress)
            typeface = Fonts.bn(ctx)
            textSize = 10.5f
            setTextColor(Palette.MUTED)
        })
        sparkCell.addView(kRow)
        sparkCell.addView(SparkView(ctx).apply {
            setAccent(prefs.accent)
            setData(pctPerDay)
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 38f)
        ).apply { topMargin = Ui.dp(ctx, 12f) })
        col.addView(sparkCell)
        return col
    }

    private fun buildSectionHead(): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = Ui.dp(ctx, 8f)
                bottomMargin = Ui.dp(ctx, 14f)
            }
        }
        row.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_line)
            imageTintList = ColorStateList.valueOf(accent)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 16f), Ui.dp(ctx, 16f)))
        row.addView(TextView(ctx).apply {
            text = getString(R.string.dash_insights)
            typeface = Fonts.bnSemi(ctx)
            textSize = 18f
            setTextColor(Palette.TEXT)
            setPadding(Ui.dp(ctx, 9f), 0, 0, 0)
        })
        row.addView(Ui.underline(ctx, accent), LinearLayout.LayoutParams(
            Ui.dp(ctx, 30f), Ui.dp(ctx, 2.5f)
        ).apply { marginStart = Ui.dp(ctx, 10f) })
        row.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        row.addView(TextView(ctx).apply {
            text = getString(R.string.dash_7days)
            typeface = Fonts.bn(ctx)
            textSize = 10.5f
            setTextColor(Palette.MUTED)
            setPadding(Ui.dp(ctx, 10f), Ui.dp(ctx, 4f), Ui.dp(ctx, 10f), Ui.dp(ctx, 4f))
            background = Ui.round(Palette.PANEL, Ui.fdp(ctx, 20f), 1, Palette.LINE)
        })
        return row
    }

    private fun panel(titleIcon: Int, title: String): LinearLayout {
        val ctx = requireContext()
        val panel = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 16f), Ui.dp(ctx, 16f), Ui.dp(ctx, 16f), Ui.dp(ctx, 16f))
            background = Ui.card(ctx, 16f)
            elevation = Ui.fdp(ctx, 4f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = Ui.dp(ctx, 12f) }
        }
        val head = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        head.addView(ImageView(ctx).apply {
            setImageResource(titleIcon)
            imageTintList = ColorStateList.valueOf(accent)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 14f), Ui.dp(ctx, 14f)))
        head.addView(TextView(ctx).apply {
            text = "  $title"
            typeface = Fonts.bnSemi(ctx)
            textSize = 16f
            setTextColor(Palette.TEXT)
        })
        panel.addView(head, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = Ui.dp(ctx, 14f) })
        return panel
    }

    private fun buildBarsPanel(pctPerDay: List<Int>): View {
        val ctx = requireContext()
        val panel = panel(R.drawable.ic_column, getString(R.string.dash_daily))
        panel.addView(BarsView(ctx).apply {
            setAccent(prefs.accent)
            setData(days.map { Schedule.dayShort(ctx, it) }, pctPerDay, Schedule.todayIndex())
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 130f)
        ))
        return panel
    }

    private fun buildStreakPanel(): View {
        val ctx = requireContext()
        val panel = panel(R.drawable.ic_fire, getString(R.string.dash_streak))
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(ctx).apply {
            text = prefs.streak.toString()
            typeface = Fonts.display(ctx)
            textSize = 40f
            setTextColor(accentSoft)
        })
        row.addView(TextView(ctx).apply {
            text = "   " + getString(R.string.dash_streak_meta)
            typeface = Fonts.bn(ctx)
            textSize = 11.5f
            setTextColor(Palette.MUTED)
        })
        panel.addView(row)

        val derma = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 12f), Ui.dp(ctx, 14f), Ui.dp(ctx, 12f))
            background = Ui.round(
                withAlpha(Palette.ROSE, 0x1A), Ui.fdp(ctx, 14f), 1,
                withAlpha(Palette.ROSE, 0x42)
            )
        }
        derma.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_derma)
            imageTintList = ColorStateList.valueOf(Palette.ROSE)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 22f), Ui.dp(ctx, 22f)))
        val dCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 12f), 0, 0, 0)
        }
        dCol.addView(TextView(ctx).apply {
            text = getString(R.string.dash_derma_next)
            typeface = Fonts.bn(ctx)
            textSize = 11.5f
            setTextColor(Palette.MUTED)
        })
        dCol.addView(TextView(ctx).apply {
            text = getString(R.string.dash_derma_days, Schedule.daysToDerma())
            typeface = Fonts.bnSemi(ctx)
            textSize = 18f
            setTextColor(Palette.ROSE)
        })
        derma.addView(dCol)
        panel.addView(derma, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = Ui.dp(ctx, 14f) })
        return panel
    }

    private fun buildRingsPanel(pctPerDay: List<Int>): View {
        val ctx = requireContext()
        val panel = panel(R.drawable.ic_drop, getString(R.string.dash_effectiveness))
        panel.addView(RingsView(ctx).apply {
            setAccent(prefs.accent)
            setData(days.map { Schedule.dayShort(ctx, it) }, pctPerDay)
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 96f)
        ))
        return panel
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
}
