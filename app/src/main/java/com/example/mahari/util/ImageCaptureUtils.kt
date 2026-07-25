package com.example.mahari.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ImageCaptureUtils {

    fun generateAndShareCardImage(
        context: Context,
        title: String,
        amountText: String,
        subtitle: String,
        isExpense: Boolean,
        isDarkMode: Boolean
    ) {
        val width = 1080
        val height = 720

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Colors
        val bgColor = if (isDarkMode) Color.parseColor("#16181A") else Color.parseColor("#F4F3EF")
        val cardColor = if (isDarkMode) Color.parseColor("#1F2226") else Color.parseColor("#FFFFFF")
        val textColorPrimary = if (isDarkMode) Color.parseColor("#F5F5F4") else Color.parseColor("#1C1F1E")
        val textColorSecondary = if (isDarkMode) Color.parseColor("#8B9089") else Color.parseColor("#6B6F6C")
        val accentColor = if (isDarkMode) Color.parseColor("#34D399") else Color.parseColor("#059669")
        val metalColor = if (isDarkMode) Color.parseColor("#B08D57") else Color.parseColor("#8C6D3F")

        // 1. Draw Background
        val bgPaint = Paint().apply { color = bgColor }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Draw Card Surface
        val cardMargin = 60f
        val cardRect = RectF(cardMargin, cardMargin, width - cardMargin, height - cardMargin)
        val cardPaint = Paint().apply {
            color = cardColor
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

        // Card Border Stroke (Warm Metal)
        val borderPaint = Paint().apply {
            color = metalColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, borderPaint)

        // 3. Draw Branding Header
        val brandPaint = Paint().apply {
            color = metalColor
            textSize = 36f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("🐚 MAHARI", cardMargin + 40f, cardMargin + 80f, brandPaint)

        val subBrandPaint = Paint().apply {
            color = textColorSecondary
            textSize = 22f
            isAntiAlias = true
        }
        canvas.drawText("M-Pesa Financial Intelligence", cardMargin + 40f, cardMargin + 115f, subBrandPaint)

        // 4. Draw Title
        val titlePaint = Paint().apply {
            color = textColorSecondary
            textSize = 28f
            isAntiAlias = true
        }
        canvas.drawText(title.uppercase(), cardMargin + 40f, cardMargin + 220f, titlePaint)

        // 5. Draw Amount Figure
        val amountPaint = Paint().apply {
            color = if (title.contains("OVER") || isExpense) textColorPrimary else accentColor
            textSize = 72f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(amountText, cardMargin + 40f, cardMargin + 320f, amountPaint)

        // 6. Draw Subtitle / Context
        val subtitlePaint = Paint().apply {
            color = textColorSecondary
            textSize = 26f
            isAntiAlias = true
        }
        canvas.drawText(subtitle, cardMargin + 40f, cardMargin + 400f, subtitlePaint)

        // 7. Footer Branding Line
        val footerPaint = Paint().apply {
            color = metalColor
            textSize = 20f
            isAntiAlias = true
        }
        canvas.drawText("Mahari • Private & Offline Financial Intelligence", cardMargin + 40f, height - cardMargin - 40f, footerPaint)

        // Save & Launch Share Intent via FileProvider
        shareBitmap(context, bitmap)
    }

    private fun shareBitmap(context: Context, bitmap: Bitmap) {
        try {
            val cachePath = File(context.cacheDir, "shares")
            cachePath.mkdirs()
            val file = File(cachePath, "mahari_share.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Mahari Insight"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
