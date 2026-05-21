package com.shortsblockerkids.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.shortsblockerkids.MainActivity
import com.shortsblockerkids.R
import java.util.Locale
import kotlin.math.roundToInt

class BlockOverlayController(
    private val service: AccessibilityService,
    private val onOverlayDismissed: () -> Unit = {},
    private val onPinEntryRequested: () -> Unit = {},
    private val onShortsCloseCompleted: () -> Unit = {},
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var isYouTubeHomeNavigationInProgress = false

    val isOverlayVisible: Boolean
        get() = overlayView != null

    fun showBlockedOverlay(): Boolean {
        if (overlayView != null) {
            return false
        }

        val view = buildOverlayView()
        val params =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.CENTER
                }

        return runCatching {
            windowManager.addView(view, params)
            overlayView = view
            true
        }.getOrDefault(false)
    }

    fun dismissOverlay() {
        val view = overlayView
        overlayView = null
        isYouTubeHomeNavigationInProgress = false
        if (view == null) {
            return
        }

        runCatching {
            windowManager.removeViewImmediate(view)
        }
        onOverlayDismissed()
    }

    private fun buildOverlayView(): View {
        val root =
            FrameLayout(service).apply {
                setBackgroundColor(OVERLAY_BACKGROUND_COLOR)
            }
        val scrollView =
            ScrollView(service).apply {
                isFillViewport = true
                clipToPadding = false
                setPadding(20.dp(), 20.dp(), 20.dp(), 20.dp())
            }
        val container =
            LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(4.dp(), 16.dp(), 4.dp(), 16.dp())
            }

        container.addView(appNameText(), textLayoutParams(bottomMarginDp = 20))
        container.addView(titleText(), textLayoutParams(bottomMarginDp = 12))
        container.addView(messageText(), textLayoutParams(bottomMarginDp = 28))
        container.addView(buttonsContainer(), matchWidthWrapHeight())
        scrollView.addView(container, scrollContentLayoutParams())
        root.addView(scrollView, frameMatchParent())
        return root
    }

    private fun appNameText(): TextView =
        TextView(service).apply {
            text = service.getString(R.string.app_name)
            setTextColor(BRAND_COLOR)
            textSize = if (isCompactHeight()) 16f else 18f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }

    private fun titleText(): TextView =
        TextView(service).apply {
            text = service.getString(R.string.blocking_overlay_title)
            setTextColor(Color.rgb(24, 31, 44))
            textSize = if (isCompactHeight()) 26f else 32f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }

    private fun messageText(): TextView =
        TextView(service).apply {
            text = service.getString(R.string.blocking_overlay_message)
            setTextColor(Color.rgb(54, 65, 82))
            textSize = if (isCompactHeight()) 16f else 18f
            gravity = Gravity.CENTER
            includeFontPadding = true
            setLineSpacing(3.dp().toFloat(), 1f)
        }

    private fun buttonsContainer(): LinearLayout =
        LinearLayout(service).apply {
            orientation =
                if (isLandscape()) {
                    LinearLayout.HORIZONTAL
                } else {
                    LinearLayout.VERTICAL
                }
            gravity = Gravity.CENTER
            addView(exitShortsButton(), buttonLayoutParams(isFirst = true))
            addView(enterPinButton(), buttonLayoutParams(isFirst = false))
        }

    private fun exitShortsButton(): Button =
        overlayButton(
            text = service.getString(R.string.blocking_overlay_exit_shorts),
            backgroundColor = BRAND_COLOR,
            textColor = Color.WHITE,
        ).apply {
            setOnClickListener {
                isEnabled = false
                closeShorts(this)
            }
        }

    private fun enterPinButton(): Button =
        overlayButton(
            text = service.getString(R.string.blocking_overlay_enter_pin),
            backgroundColor = Color.WHITE,
            textColor = BRAND_COLOR,
        ).apply {
            background =
                roundedBackground(
                    color = Color.WHITE,
                    strokeColor = BRAND_COLOR,
                )
            setOnClickListener {
                onPinEntryRequested()
                openAppForPinEntry()
                dismissOverlay()
            }
        }

    private fun overlayButton(
        text: String,
        backgroundColor: Int,
        textColor: Int,
    ): Button =
        Button(service).apply {
            this.text = text
            isAllCaps = false
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            minHeight = 48.dp()
            setTextColor(textColor)
            background = roundedBackground(color = backgroundColor)
            setPadding(16.dp(), 10.dp(), 16.dp(), 10.dp())
        }

    private fun openAppForPinEntry() {
        val intent =
            Intent(service, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
                putExtra(MainActivity.EXTRA_OPEN_TEMPORARY_ALLOW_PIN, true)
            }
        service.startActivity(intent)
    }

    private fun closeShorts(button: Button) {
        if (isYouTubeHomeNavigationInProgress) {
            return
        }

        isYouTubeHomeNavigationInProgress = true
        val navigationStarted = openYouTubeHome() || service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        if (!navigationStarted) {
            isYouTubeHomeNavigationInProgress = false
            button.isEnabled = true
            return
        }

        mainHandler.postDelayed(
            {
                dismissOverlay()
                onShortsCloseCompleted()
            },
            YOUTUBE_HOME_NAVIGATION_DELAY_MS,
        )
    }

    private fun openYouTubeHome(): Boolean {
        val root = findYouTubeRoot() ?: return false
        val homeNode = root.findHomeNode() ?: return false
        return homeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun findYouTubeRoot(): AccessibilityNodeInfo? =
        service.windows
            .asSequence()
            .mapNotNull { window -> window.root }
            .firstOrNull { root -> root.packageName?.toString() == YouTubeShortsDetector.YOUTUBE_PACKAGE }
            ?: service.rootInActiveWindow?.takeIf { root ->
                root.packageName?.toString() == YouTubeShortsDetector.YOUTUBE_PACKAGE
            }

    private fun AccessibilityNodeInfo.findHomeNode(): AccessibilityNodeInfo? {
        if (isVisibleToUser && isYouTubeHomeNode()) {
            return findClickableTarget()
        }

        for (index in 0 until childCount) {
            val match = getChild(index)?.findHomeNode()
            if (match != null) {
                return match
            }
        }

        return null
    }

    private fun AccessibilityNodeInfo.isYouTubeHomeNode(): Boolean {
        val label = contentDescription?.toString() ?: text?.toString() ?: return false
        val normalized = label.lowercase(Locale.ROOT)
        return YOUTUBE_HOME_LABELS.any { homeLabel -> normalized.contains(homeLabel) }
    }

    private fun AccessibilityNodeInfo.findClickableTarget(): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = this
        while (current != null) {
            if (current.isClickable) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun frameMatchParent(): FrameLayout.LayoutParams =
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )

    private fun scrollContentLayoutParams(): FrameLayout.LayoutParams =
        FrameLayout
            .LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.CENTER
            }

    private fun matchWidthWrapHeight(): LinearLayout.LayoutParams =
        LinearLayout
            .LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )

    private fun textLayoutParams(bottomMarginDp: Int): LinearLayout.LayoutParams =
        matchWidthWrapHeight().apply {
            bottomMargin = bottomMarginDp.dp()
        }

    private fun buttonLayoutParams(isFirst: Boolean): LinearLayout.LayoutParams {
        val margin = 10.dp()
        return LinearLayout
            .LayoutParams(buttonWidth(), LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                if (isLandscape()) {
                    weight = 1f
                    leftMargin = if (isFirst) 0 else margin
                    rightMargin = if (isFirst) margin else 0
                } else {
                    topMargin = if (isFirst) 0 else margin
                }
            }
    }

    private fun buttonWidth(): Int =
        if (isLandscape()) {
            0
        } else {
            LinearLayout.LayoutParams.MATCH_PARENT
        }

    private fun roundedBackground(
        color: Int,
        strokeColor: Int? = null,
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = 12.dp().toFloat()
            strokeColor?.let { setStroke(1.dp(), it) }
        }

    private fun isLandscape(): Boolean = service.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun isCompactHeight(): Boolean = service.resources.displayMetrics.heightPixels < 520.dp()

    private fun Int.dp(): Int = (this * service.resources.displayMetrics.density).roundToInt()

    companion object {
        private val OVERLAY_BACKGROUND_COLOR = Color.rgb(248, 249, 252)
        private val BRAND_COLOR = Color.rgb(36, 87, 166)
        private const val YOUTUBE_HOME_NAVIGATION_DELAY_MS = 250L
        private val YOUTUBE_HOME_LABELS = setOf("home", "головна", "главная", "startseite")
    }
}
