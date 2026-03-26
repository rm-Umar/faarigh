package com.faarigh.app.service.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects short-form video content (YouTube Shorts, Instagram Reels, Snapchat Spotlight)
 * by inspecting the accessibility node tree for known UI patterns.
 *
 * These patterns may change with app updates. The resource ID patterns
 * are kept as configurable lists so they can be updated without code changes.
 */
class ShortsDetector {

    companion object {
        private const val TAG = "ShortsDetector"

        // Max depth for tree traversal — YouTube and Instagram have deep view hierarchies
        private const val MAX_DEPTH = 15

        // Packages that are entirely short-form (intercept at app level instead)
        val FULL_SHORT_FORM_PACKAGES = setOf(
            "com.zhiliaoapp.musically",        // TikTok (international)
            "com.ss.android.ugc.trill",        // TikTok (alternate package)
            "com.ss.android.ugc.aweme",        // TikTok Lite / Douyin
        )

        // All packages the shorts detector monitors
        val MONITORED_PACKAGES = FULL_SHORT_FORM_PACKAGES + setOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.snapchat.android",
        )
    }

    // ── YouTube Shorts patterns ─────────────────────────────────────
    // Resource ID substrings found in YouTube Shorts UI
    private val youtubeResourceIdPatterns = listOf(
        "reel_player_page_container",  // Main shorts player container
        "reel_recycler",               // Shorts vertical scroll recycler
        "reel_multi_format_link",      // Link in shorts
        "reel_header_container",       // Header in shorts
        "shorts_channel_header",       // Channel header in shorts
        "shorts_player",               // Shorts player
        "shorts_shelf",                // Shorts shelf on home
        "reel_watch_player",           // Watch player for reels/shorts
    )

    // Content descriptions that indicate Shorts UI
    private val youtubeContentDescPatterns = listOf(
        "shorts",
        "short video",
    )

    // ── Instagram Reels patterns ────────────────────────────────────
    private val instagramResourceIdPatterns = listOf(
        "clips_viewer_view_pager",     // Reels viewer pager
        "clips_tab",                   // Reels tab
        "clips_viewer",                // Reels viewer container
        "reel_viewer_subtitle",        // Reel viewer subtitle
        "reel_viewer_title",           // Reel viewer title
        "reels_viewer",                // Reels viewer (newer versions)
        "ig_reels",                    // Reels container
    )

    private val instagramContentDescPatterns = listOf(
        "reels",
        "reel",
    )

    // ── Snapchat Spotlight patterns ─────────────────────────────────
    private val snapchatResourceIdPatterns = listOf(
        "spotlight",
        "spotlight_feed",
        "spotlight_viewer",
    )

    private val snapchatContentDescPatterns = listOf(
        "spotlight",
    )

    data class DetectionResult(
        val detected: Boolean,
        val platform: String = "",
        val detail: String = "",
    )

    fun checkForShorts(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
    ): DetectionResult {
        if (rootNode == null) return DetectionResult(false)

        val packageName = event.packageName?.toString() ?: return DetectionResult(false)

        // TikTok — the whole app is short-form
        if (packageName in FULL_SHORT_FORM_PACKAGES) {
            Log.d(TAG, "Full short-form app detected: $packageName")
            return DetectionResult(true, "TikTok", "Full short-form app")
        }

        return when (packageName) {
            "com.google.android.youtube" -> checkYouTubeShorts(rootNode)
            "com.instagram.android" -> checkInstagramReels(rootNode)
            "com.snapchat.android" -> checkSnapchatSpotlight(rootNode)
            else -> DetectionResult(false)
        }
    }

    // ── YouTube ─────────────────────────────────────────────────────

    private fun checkYouTubeShorts(root: AccessibilityNodeInfo): DetectionResult {
        // 1. Check for Shorts-specific resource IDs in the node tree
        // This is the most reliable signal — if a reel/shorts container exists, user is in shorts
        val matchedResId = findNodeWithResourceIdContaining(root, youtubeResourceIdPatterns)
        if (matchedResId != null) {
            Log.d(TAG, "YouTube Shorts detected via resource ID: $matchedResId")
            return DetectionResult(true, "YouTube", "Shorts resource ID: $matchedResId")
        }

        // 2. Use findAccessibilityNodeInfosByText for fast text-based search
        val shortNodes = root.findAccessibilityNodeInfosByText("Shorts")
        if (shortNodes != null && shortNodes.isNotEmpty()) {
            // Check if any of these nodes is selected (means user is ON the Shorts tab)
            for (node in shortNodes) {
                if (node.isSelected) {
                    Log.d(TAG, "YouTube Shorts detected via selected text node")
                    return DetectionResult(true, "YouTube", "Shorts tab selected (text search)")
                }
                // Also check parent — YouTube sometimes sets selected on parent
                val parent = node.parent
                if (parent != null && parent.isSelected) {
                    parent.recycle()
                    Log.d(TAG, "YouTube Shorts detected via selected parent node")
                    return DetectionResult(true, "YouTube", "Shorts tab parent selected")
                }
                parent?.recycle()
            }
        }

        // 3. Check content descriptions for "Shorts" with selected/focused state
        val inShorts = findSelectedOrFocusedWithContentDesc(root, "shorts")
        if (inShorts) {
            Log.d(TAG, "YouTube Shorts detected via selected content desc")
            return DetectionResult(true, "YouTube", "Shorts tab selected")
        }

        // 4. Check if "Shorts" tab is selected (text-based tree walk)
        if (findSelectedTabWithText(root, "Shorts")) {
            Log.d(TAG, "YouTube Shorts detected via selected tab text")
            return DetectionResult(true, "YouTube", "Shorts tab selected (text)")
        }

        return DetectionResult(false)
    }

    // ── Instagram ───────────────────────────────────────────────────

    // Resource IDs that confirm the user is actively IN the reels viewer (not just that the tab exists)
    private val instagramActiveReelsResourceIds = listOf(
        "clips_viewer_view_pager",     // Reels viewer pager
        "clips_viewer",                // Reels viewer container
        "reel_viewer_subtitle",        // Reel viewer subtitle
        "reel_viewer_title",           // Reel viewer title
        "reels_viewer",                // Reels viewer (newer versions)
        "ig_reels",                    // Reels container
    )

    private fun checkInstagramReels(root: AccessibilityNodeInfo): DetectionResult {
        // 1. Check for Reels viewer resource IDs (NOT just clips_tab — that only means the tab exists)
        // Only detect if we find resource IDs that confirm the user is IN the reels player
        val matchedResId = findNodeWithResourceIdContaining(root, instagramActiveReelsResourceIds)
        if (matchedResId != null) {
            Log.d(TAG, "Instagram Reels detected via active viewer resource ID: $matchedResId")
            return DetectionResult(true, "Instagram", "Reels viewer resource ID: $matchedResId")
        }

        // 2. Use findAccessibilityNodeInfosByText for fast text-based search
        val reelNodes = root.findAccessibilityNodeInfosByText("Reels")
        if (reelNodes != null && reelNodes.isNotEmpty()) {
            for (node in reelNodes) {
                if (node.isSelected) {
                    Log.d(TAG, "Instagram Reels detected via selected text node")
                    return DetectionResult(true, "Instagram", "Reels tab selected (text search)")
                }
                val parent = node.parent
                if (parent != null && parent.isSelected) {
                    parent.recycle()
                    Log.d(TAG, "Instagram Reels detected via selected parent node")
                    return DetectionResult(true, "Instagram", "Reels tab parent selected")
                }
                parent?.recycle()
            }
        }

        // 3. Check if "Reels" tab is selected (tab-like element with text "Reels" that is selected)
        if (findSelectedTabWithText(root, "Reels")) {
            Log.d(TAG, "Instagram Reels detected via selected tab text")
            return DetectionResult(true, "Instagram", "Reels tab selected")
        }

        return DetectionResult(false)
    }

    // ── Snapchat ────────────────────────────────────────────────────

    private fun checkSnapchatSpotlight(root: AccessibilityNodeInfo): DetectionResult {
        val matchedResId = findNodeWithResourceIdContaining(root, snapchatResourceIdPatterns)
        if (matchedResId != null) {
            Log.d(TAG, "Snapchat Spotlight detected via resource ID: $matchedResId")
            return DetectionResult(true, "Snapchat", "Spotlight resource ID: $matchedResId")
        }

        val matchedDesc = findNodeWithContentDescContaining(root, snapchatContentDescPatterns)
        if (matchedDesc != null) {
            Log.d(TAG, "Snapchat Spotlight detected via content desc: $matchedDesc")
            return DetectionResult(true, "Snapchat", "Spotlight content desc")
        }

        if (findSelectedTabWithText(root, "Spotlight")) {
            Log.d(TAG, "Snapchat Spotlight detected via selected tab text")
            return DetectionResult(true, "Snapchat", "Spotlight tab selected")
        }

        return DetectionResult(false)
    }

    // ── Tree traversal helpers ──────────────────────────────────────

    /**
     * Find a node whose viewIdResourceName contains any of the given patterns.
     * Returns the matched resource ID string, or null if not found.
     */
    private fun findNodeWithResourceIdContaining(
        node: AccessibilityNodeInfo,
        patterns: List<String>,
        depth: Int = 0,
    ): String? {
        if (depth > MAX_DEPTH) return null

        val resourceId = node.viewIdResourceName
        if (resourceId != null) {
            for (pattern in patterns) {
                if (resourceId.contains(pattern, ignoreCase = true)) {
                    return resourceId
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeWithResourceIdContaining(child, patterns, depth + 1)
            child.recycle()
            if (result != null) return result
        }

        return null
    }

    /**
     * Find a node whose contentDescription contains any of the given patterns.
     * Returns the matched content description, or null.
     */
    private fun findNodeWithContentDescContaining(
        node: AccessibilityNodeInfo,
        patterns: List<String>,
        depth: Int = 0,
    ): String? {
        if (depth > MAX_DEPTH) return null

        val desc = node.contentDescription?.toString()
        if (desc != null) {
            for (pattern in patterns) {
                if (desc.contains(pattern, ignoreCase = true)) {
                    return desc
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeWithContentDescContaining(child, patterns, depth + 1)
            child.recycle()
            if (result != null) return result
        }

        return null
    }

    /**
     * Check if there's a node that is selected/focused whose contentDescription
     * contains the given text. This differentiates "seeing the Shorts tab" from
     * "being IN the Shorts feed".
     */
    private fun findSelectedOrFocusedWithContentDesc(
        node: AccessibilityNodeInfo,
        text: String,
        depth: Int = 0,
    ): Boolean {
        if (depth > MAX_DEPTH) return false

        val desc = node.contentDescription?.toString()
        if (desc != null && desc.contains(text, ignoreCase = true)) {
            if (node.isSelected || node.isFocused || node.isAccessibilityFocused) {
                return true
            }
            // Also check className — if it's a ViewPager or RecyclerView containing shorts,
            // the user is actively viewing shorts content
            val className = node.className?.toString() ?: ""
            if (className.contains("ViewPager", ignoreCase = true) ||
                className.contains("RecyclerView", ignoreCase = true)
            ) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findSelectedOrFocusedWithContentDesc(child, text, depth + 1)) {
                child.recycle()
                return true
            }
            child.recycle()
        }

        return false
    }

    /**
     * Find a selected tab node whose text matches the given tab text.
     * Checks both text and contentDescription, and accepts various tab-like class names.
     */
    private fun findSelectedTabWithText(
        node: AccessibilityNodeInfo,
        tabText: String,
        depth: Int = 0,
    ): Boolean {
        if (depth > MAX_DEPTH) return false

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        val isSelected = node.isSelected
        val className = node.className?.toString() ?: ""

        // Check both text and contentDescription
        val textMatches = text != null && text.equals(tabText, ignoreCase = true)
        val descMatches = desc != null && desc.contains(tabText, ignoreCase = true)

        if ((textMatches || descMatches) && isSelected) {
            // Accept Tab, Button, ImageView (YouTube uses ImageView for bottom tabs)
            val isTabLike = className.contains("Tab", ignoreCase = true) ||
                className.contains("Button", ignoreCase = true) ||
                className.contains("ImageView", ignoreCase = true) ||
                className.contains("ViewGroup", ignoreCase = true) ||
                className.contains("BottomNavigationItemView", ignoreCase = true) ||
                className.contains("NavigationBarItemView", ignoreCase = true)

            if (isTabLike) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findSelectedTabWithText(child, tabText, depth + 1)) {
                child.recycle()
                return true
            }
            child.recycle()
        }

        return false
    }
}
