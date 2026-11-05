package com.example.data.reminder

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import java.util.Locale
import kotlin.random.Random

object NotificationCopy {

    // Small message pools per category with warm, playful tone and at most 1 emoji
    val WORK_TITLES = listOf(
        "Heads up, focus time 💼",
        "Quick reminder for work ✨",
        "Your next work task is ready"
    )

    val HEALTH_TITLES = listOf(
        "Time to move 💪",
        "Little health check-in 🌿",
        "Your body says hi 👋"
    )

    val STUDY_TITLES = listOf(
        "Study time, let's go 📚",
        "Knowledge break? Now's a good time 🧠",
        "Your study session is up"
    )

    val PERSONAL_TITLES = listOf(
        "Something for you 🌟",
        "Friendly nudge 💛",
        "Don't forget this one"
    )

    val STREAK_TITLES = listOf(
        "Keep the streak alive 🔥"
    )

    val DEFAULT_TITLES = listOf(
        "Friendly reminder 🌟",
        "A quick nudge for you ✨",
        "Your next task is ready"
    )

    fun getNotificationTitle(
        category: String,
        reminderOffset: String? = null,
        isAtDueTime: Boolean = true,
        random: Random = Random.Default
    ): String {
        // Timing variants: if it was a before-offset and not fired at due time
        val offsetText = getBeforeOffsetSummary(reminderOffset)
        if (!isAtDueTime && offsetText != null) {
            return "Coming up in $offsetText ⏰"
        }

        val clean = category.trim().lowercase(Locale.US)
        val pool = when {
            clean.contains("streak") || clean.contains("habit") -> STREAK_TITLES
            clean.contains("work") || clean.contains("job") || clean.contains("office") -> WORK_TITLES
            clean.contains("health") || clean.contains("fitness") || clean.contains("gym") -> HEALTH_TITLES
            clean.contains("study") || clean.contains("learn") || clean.contains("read") || clean.contains("book") -> STUDY_TITLES
            clean.contains("personal") || clean.contains("self") || clean.contains("me") -> PERSONAL_TITLES
            else -> DEFAULT_TITLES
        }
        return pool[random.nextInt(pool.size)]
    }

    fun getBeforeOffsetSummary(reminderOffset: String?): String? {
        if (reminderOffset == null) return null
        val norm = reminderOffset.trim().lowercase(Locale.US)
        return when {
            norm.contains("15 min") || norm.contains("15 minutes") -> "15 min"
            norm.contains("30 min") || norm.contains("30 minutes") -> "30 min"
            norm.contains("1 hour") || norm.contains("1 hr") || norm.contains("60 min") -> "1 hour"
            norm.contains("0 min") || norm.contains("at time") || norm.contains("at due") -> null
            else -> null
        }
    }

    fun sanitizeTitle(title: String?): String {
        val trimmed = title?.trim() ?: ""
        return if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) {
            "Your task"
        } else {
            trimmed
        }
    }

    fun buildSubtext(category: String?, dueTime: String?): String {
        val cat = category?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Task"
        val time = dueTime?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        return if (time != null) {
            "$cat • $time"
        } else {
            cat
        }
    }

    fun buildNotificationBody(title: String?, category: String?, dueTime: String?): CharSequence {
        val safeTitle = sanitizeTitle(title)
        val subtext = buildSubtext(category, dueTime)
        val ssb = SpannableStringBuilder()
        ssb.append(safeTitle)
        ssb.setSpan(StyleSpan(Typeface.BOLD), 0, ssb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.append("\n")
        ssb.append(subtext)
        return ssb
    }

    fun buildExpandedNotesText(title: String?, category: String?, dueTime: String?, notes: String?): CharSequence {
        val ssb = SpannableStringBuilder()
        val safeTitle = sanitizeTitle(title)
        val subtext = buildSubtext(category, dueTime)
        ssb.append(safeTitle)
        ssb.setSpan(StyleSpan(Typeface.BOLD), 0, ssb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.append("\n")
        ssb.append(subtext)
        val cleanNotes = notes?.trim()
        if (!cleanNotes.isNullOrBlank() && !cleanNotes.equals("null", ignoreCase = true)) {
            ssb.append("\n\n")
            ssb.append(cleanNotes)
        }
        return ssb
    }
}
