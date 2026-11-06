package com.example.data.reminder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.core.content.ContextCompat
import com.musab.tiko.R
import java.util.Locale

object NotificationIconHelper {

    val COLOR_WORK = Color.parseColor("#6B4EFF")
    val COLOR_HEALTH = Color.parseColor("#FFB92E")
    val COLOR_STUDY = Color.parseColor("#BAE136")
    val COLOR_PERSONAL = Color.parseColor("#151515")
    val COLOR_STREAK = Color.parseColor("#FFB92E")
    val COLOR_DEFAULT = Color.parseColor("#6B4EFF")

    fun getCategoryColor(category: String?): Int {
        val clean = category?.trim()?.lowercase(Locale.US) ?: ""
        return when {
            clean.contains("streak") || clean.contains("habit") -> COLOR_STREAK
            clean.contains("work") || clean.contains("job") || clean.contains("office") -> COLOR_WORK
            clean.contains("health") || clean.contains("fitness") || clean.contains("gym") -> COLOR_HEALTH
            clean.contains("study") || clean.contains("learn") || clean.contains("read") || clean.contains("book") -> COLOR_STUDY
            clean.contains("personal") || clean.contains("self") || clean.contains("me") -> COLOR_PERSONAL
            else -> COLOR_DEFAULT
        }
    }

    private fun getCategoryIconRes(category: String?): Int {
        val clean = category?.trim()?.lowercase(Locale.US) ?: ""
        return when {
            clean.contains("streak") || clean.contains("habit") -> R.drawable.ic_category_streak
            clean.contains("work") || clean.contains("job") || clean.contains("office") -> R.drawable.ic_category_work
            clean.contains("health") || clean.contains("fitness") || clean.contains("gym") -> R.drawable.ic_category_health
            clean.contains("study") || clean.contains("learn") || clean.contains("read") || clean.contains("book") -> R.drawable.ic_category_study
            clean.contains("personal") || clean.contains("self") || clean.contains("me") -> R.drawable.ic_category_personal
            else -> R.drawable.ic_category_work
        }
    }

    fun createCategoryLargeIcon(context: Context, category: String?): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circleColor = getCategoryColor(category)
        val isDarkCircle = circleColor == COLOR_PERSONAL || circleColor == COLOR_WORK
        val iconTint = if (isDarkCircle) Color.WHITE else Color.parseColor("#151515")

        // Draw colored circle
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = circleColor
            style = Paint.Style.FILL
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        // Draw category icon centered in the circle
        val iconRes = getCategoryIconRes(category)
        val drawable = ContextCompat.getDrawable(context, iconRes)
        if (drawable != null) {
            val iconSize = (size * 0.52f).toInt()
            val left = (size - iconSize) / 2
            val top = (size - iconSize) / 2
            drawable.setBounds(left, top, left + iconSize, top + iconSize)
            drawable.colorFilter = PorterDuffColorFilter(iconTint, PorterDuff.Mode.SRC_IN)
            drawable.draw(canvas)
        }

        return bitmap
    }

    /**
     * Large icon for Streak Reminders:
     * Flame icon on the lime #BAE136 circle with #151515 tint.
     */
    fun createStreakLargeIcon(context: Context): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circleColor = Color.parseColor("#BAE136")
        val iconTint = Color.parseColor("#151515")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = circleColor
            style = Paint.Style.FILL
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_category_streak)
        if (drawable != null) {
            val iconSize = (size * 0.52f).toInt()
            val left = (size - iconSize) / 2
            val top = (size - iconSize) / 2
            drawable.setBounds(left, top, left + iconSize, top + iconSize)
            drawable.colorFilter = PorterDuffColorFilter(iconTint, PorterDuff.Mode.SRC_IN)
            drawable.draw(canvas)
        }

        return bitmap
    }
}
