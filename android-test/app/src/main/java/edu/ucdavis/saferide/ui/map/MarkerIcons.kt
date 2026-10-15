package edu.ucdavis.saferide.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import edu.ucdavis.saferide.R
import edu.ucdavis.saferide.ui.theme.AggieBlue
import edu.ucdavis.saferide.ui.theme.AggieGold

/** A marker bitmap plus the vertical anchor (0 = top, 1 = bottom) that sits on the coordinate. */
class MarkerIcon(val drawable: Drawable, val anchorV: Float)

/** Map marker bitmaps drawn in code so they match the reference design and UC Davis palette exactly. */
object MarkerIcons {

    private val CurrentBlue = Color(0xFF4285F4)
    private val LabelBorder = Color(0xFFD5D7DB)
    private val WalkerGrey = Color(0xFF5F6368)

    /** A rounded Aggie Blue badge with a gold border and the van number. */
    fun van(context: Context, number: Int): MarkerIcon = square(context, 38f) { canvas, px, d ->
        val stroke = 3f * d
        val rect = RectF(stroke / 2, stroke / 2, px - stroke / 2, px - stroke / 2)
        val radius = 10f * d
        canvas.drawRoundRect(rect, radius, radius, fill(AggieBlue))
        canvas.drawRoundRect(rect, radius, radius, outline(AggieGold, stroke))
        drawCenteredText(canvas, "$number", px / 2, px / 2, 17f * d, Color.White)
    }

    /** Navy ring with a white center, like the reference "Pickup Spot" marker, with an optional callout label. */
    fun pickup(context: Context, label: String?): MarkerIcon {
        val d = context.resources.displayMetrics.density
        val dot = 30f * d
        if (label == null) return square(context, 30f) { canvas, px, _ -> drawPickupDot(canvas, px / 2, px / 2, dot, d) }

        val text = textPaint(16f * d, Color.Black, bold = false)
        val padH = 12f * d
        val labelW = text.measureText(label) + padH * 2
        val labelH = 38f * d
        val pointer = 7f * d
        val gap = 4f * d
        val width = maxOf(labelW, dot)
        val height = labelH + pointer + gap + dot
        val bitmap = Bitmap.createBitmap(width.toInt() + 2, height.toInt() + 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val left = (width - labelW) / 2 + 1
        val box = RectF(left, 1f, left + labelW, labelH)
        canvas.drawRoundRect(box, 3f * d, 3f * d, fill(Color.White))
        canvas.drawRoundRect(box, 3f * d, 3f * d, outline(LabelBorder, 1f * d))
        val tip = Path().apply {
            moveTo(width / 2 - pointer, labelH - 1)
            lineTo(width / 2, labelH + pointer)
            lineTo(width / 2 + pointer, labelH - 1)
            close()
        }
        canvas.drawPath(tip, fill(Color.White))
        drawCenteredText(canvas, label, width / 2 + 1, labelH / 2, 16f * d, Color.Black, bold = false)
        drawPickupDot(canvas, width / 2 + 1, height - dot / 2, dot, d)

        return MarkerIcon(BitmapDrawable(context.resources, bitmap), (height - dot / 2) / bitmap.height)
    }

    /** Navy circle with a white location pin: where the ride ends. */
    fun dropoff(context: Context): MarkerIcon = square(context, 32f) { canvas, px, d ->
        canvas.drawCircle(px / 2, px / 2, px / 2, fill(AggieBlue))
        drawGlyph(context, canvas, R.drawable.ic_pickup, px / 2, px / 2, 18f * d, Color.White)
    }

    /** White circle with a walking figure: where the rider is now. */
    fun start(context: Context): MarkerIcon = square(context, 34f) { canvas, px, d ->
        canvas.drawCircle(px / 2, px / 2, px / 2 - d, fill(Color.White))
        canvas.drawCircle(px / 2, px / 2, px / 2 - d, outline(LabelBorder, 1.5f * d))
        drawGlyph(context, canvas, R.drawable.ic_walk, px / 2, px / 2, 20f * d, WalkerGrey)
    }

    /** The familiar blue "you are here" dot with a soft halo. */
    fun currentLocation(context: Context): MarkerIcon = square(context, 44f) { canvas, px, d ->
        canvas.drawCircle(px / 2, px / 2, px / 2, fill(CurrentBlue.copy(alpha = 0.18f)))
        canvas.drawCircle(px / 2, px / 2, 10f * d, fill(Color.White))
        canvas.drawCircle(px / 2, px / 2, 7.5f * d, fill(CurrentBlue))
    }

    private fun drawPickupDot(canvas: Canvas, cx: Float, cy: Float, size: Float, d: Float) {
        canvas.drawCircle(cx, cy, size / 2, fill(AggieBlue))
        canvas.drawCircle(cx, cy, size / 2 - 7f * d, fill(Color.White))
        canvas.drawCircle(cx, cy, 3.5f * d, fill(AggieBlue))
    }

    private fun drawGlyph(
        context: Context,
        canvas: Canvas,
        @DrawableRes res: Int,
        cx: Float,
        cy: Float,
        size: Float,
        tint: Color,
    ) {
        val glyph = ContextCompat.getDrawable(context, res)!!.mutate()
        glyph.setTint(tint.toArgb())
        glyph.setBounds((cx - size / 2).toInt(), (cy - size / 2).toInt(), (cx + size / 2).toInt(), (cy + size / 2).toInt())
        glyph.draw(canvas)
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        size: Float,
        color: Color,
        bold: Boolean = true,
    ) {
        val paint = textPaint(size, color, bold)
        canvas.drawText(text, cx, cy - (paint.descent() + paint.ascent()) / 2, paint)
    }

    private fun textPaint(size: Float, color: Color, bold: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    private fun square(context: Context, sizeDp: Float, block: (Canvas, Float, Float) -> Unit): MarkerIcon {
        val density = context.resources.displayMetrics.density
        val px = (sizeDp * density).toInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        block(Canvas(bitmap), px.toFloat(), density)
        return MarkerIcon(BitmapDrawable(context.resources, bitmap), 0.5f)
    }

    private fun fill(color: Color) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        style = Paint.Style.FILL
    }

    private fun outline(color: Color, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = width
    }
}
