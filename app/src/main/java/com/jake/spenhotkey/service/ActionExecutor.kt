package com.jake.spenhotkey.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.jake.spenhotkey.data.ActionSpec

/** Turns an [ActionSpec] into real system behavior via the accessibility APIs. */
class ActionExecutor(private val service: AccessibilityService) {

    fun perform(action: ActionSpec) {
        when (action) {
            ActionSpec.None -> Unit
            ActionSpec.RightClick -> rightClick()
            ActionSpec.Copy -> performNodeAction(AccessibilityNodeInfo.ACTION_COPY)
            ActionSpec.Cut -> performNodeAction(AccessibilityNodeInfo.ACTION_CUT)
            ActionSpec.Paste -> performNodeAction(AccessibilityNodeInfo.ACTION_PASTE)
            ActionSpec.SelectAll -> selectAll()
            ActionSpec.Back -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            ActionSpec.Home -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            ActionSpec.Recents -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            ActionSpec.Notifications -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            ActionSpec.QuickSettings -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            ActionSpec.Screenshot -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            ActionSpec.LockScreen -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            ActionSpec.SplitScreen -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            is ActionSpec.LaunchApp -> launchApp(action.packageName)
        }
    }

    private fun focusedNode(): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow ?: return null
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
    }

    /**
     * Emulates a mouse right-click. Prefers the node-level "context click" action (the
     * real accessibility equivalent of a right-click), falls back to long-click, and as a
     * last resort synthesizes a long-press touch gesture at the focused node's location.
     */
    private fun rightClick() {
        val node = focusedNode()
        if (node != null) {
            val actionIds = node.actionList.map { it.id }
            val contextClickId = AccessibilityNodeInfo.AccessibilityAction.ACTION_CONTEXT_CLICK.id
            when {
                contextClickId in actionIds -> {
                    node.performAction(contextClickId)
                    return
                }
                AccessibilityNodeInfo.ACTION_LONG_CLICK in actionIds -> {
                    node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                    return
                }
            }
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            if (!bounds.isEmpty) {
                dispatchLongPress(bounds.centerX().toFloat(), bounds.centerY().toFloat())
                return
            }
        }
        val root = service.rootInActiveWindow ?: return
        val bounds = Rect()
        root.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            dispatchLongPress(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
    }

    private fun selectAll() {
        val node = focusedNode() ?: return
        val text = node.text ?: return
        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
    }

    private fun performNodeAction(actionId: Int) {
        focusedNode()?.performAction(actionId)
    }

    private fun dispatchLongPress(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, LONG_PRESS_DURATION_MS)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        service.dispatchGesture(gesture, null, null)
    }

    private fun launchApp(packageName: String) {
        val intent = service.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        service.startActivity(intent)
    }

    companion object {
        private const val LONG_PRESS_DURATION_MS = 600L
    }
}
