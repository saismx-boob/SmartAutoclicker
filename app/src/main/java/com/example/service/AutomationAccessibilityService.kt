package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.engine.Humanizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoClickService"
        private var serviceRef: WeakReference<AutomationAccessibilityService>? = null

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        val instance: AutomationAccessibilityService?
            get() = serviceRef?.get()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceRef = WeakReference(this)
        _isServiceActive.value = true
        Log.d(TAG, "AutomationAccessibilityService connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can be used to listen to window content changes if needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "AutomationAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
        if (serviceRef?.get() == this) {
            serviceRef = null
        }
    }

    /**
     * Dispatches a single tap gesture at (x, y) with humanized duration.
     */
    fun performClick(x: Float, y: Float, durationMs: Long = 80L, onResult: (Boolean) -> Unit) {
        val path = Path()
        path.moveTo(x, y)
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val builder = GestureDescription.Builder()
        builder.addStroke(stroke)
        val gesture = builder.build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onResult(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onResult(false)
            }
        }, null)
    }

    /**
     * Dispatches a humanized swipe along a curved path.
     */
    fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 350L,
        onResult: (Boolean) -> Unit
    ) {
        val path = Humanizer.createHumanizedSwipePath(startX, startY, endX, endY)
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val builder = GestureDescription.Builder()
        builder.addStroke(stroke)
        val gesture = builder.build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onResult(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onResult(false)
            }
        }, null)
    }

    /**
     * Dispatches a 2-finger pinch/zoom gesture.
     */
    fun performPinch(
        centerX: Float,
        centerY: Float,
        zoomIn: Boolean = true,
        durationMs: Long = 400L,
        onResult: (Boolean) -> Unit
    ) {
        val distance = 250f
        val path1 = Path()
        val path2 = Path()

        if (zoomIn) {
            // Pinch out (fingers move away from center)
            path1.moveTo(centerX - 40f, centerY - 40f)
            path1.lineTo(centerX - distance, centerY - distance)
            path2.moveTo(centerX + 40f, centerY + 40f)
            path2.lineTo(centerX + distance, centerY + distance)
        } else {
            // Pinch in (fingers move toward center)
            path1.moveTo(centerX - distance, centerY - distance)
            path1.lineTo(centerX - 40f, centerY - 40f)
            path2.moveTo(centerX + distance, centerY + distance)
            path2.lineTo(centerX + 40f, centerY + 40f)
        }

        val stroke1 = GestureDescription.StrokeDescription(path1, 0, durationMs)
        val stroke2 = GestureDescription.StrokeDescription(path2, 0, durationMs)
        val builder = GestureDescription.Builder()
        builder.addStroke(stroke1)
        builder.addStroke(stroke2)

        dispatchGesture(builder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onResult(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onResult(false)
            }
        }, null)
    }

    /**
     * Inputs text into currently focused editable field.
     */
    fun inputText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return if (focused != null && focused.isEditable) {
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val res = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            focused.recycle()
            res
        } else {
            false
        }
    }

    /**
     * Finds center coordinates of first node containing the target text.
     */
    fun findTextCoordinates(text: String): PointF? {
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (nodes.isNullOrEmpty()) return null

        for (node in nodes) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                val point = PointF(rect.centerX().toFloat(), rect.centerY().toFloat())
                return point
            }
        }
        return null
    }

    /**
     * Scans and compiles a summary of visible elements for AI understanding.
     */
    fun getScreenSemanticSummary(): String {
        val root = rootInActiveWindow ?: return "Écran non accessible ou service inactif"
        val sb = StringBuilder()
        sb.append("Éléments visibles à l'écran :\n")
        traverseNodes(root, sb, 0)
        return sb.toString()
    }

    private fun traverseNodes(node: AccessibilityNodeInfo?, sb: StringBuilder, depth: Int) {
        if (node == null || depth > 8) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val isClickable = node.isClickable

        if (!text.isNullOrEmpty() || !desc.isNullOrEmpty() || isClickable) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            val label = text ?: desc ?: "Bouton sans texte"
            sb.append("- [")
            if (isClickable) sb.append("Cliquable: ")
            sb.append("\"$label\" à (${rect.centerX()}, ${rect.centerY()}) taille ${rect.width()}x${rect.height()}]\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNodes(child, sb, depth + 1)
        }
    }
}
