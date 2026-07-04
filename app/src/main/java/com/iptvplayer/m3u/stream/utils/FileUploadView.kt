package com.iptvplayer.m3u.stream.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.R

class FileUploadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        const val MAX_FILES = 5
    }

    // ── public listener ──────────────────────────────────────────────────────
    var onFilesChanged: ((List<Uri>) -> Unit)? = null

    // ── private state ────────────────────────────────────────────────────────
    private val selectedFiles = mutableListOf<Uri>()
    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>>

    // ── views ────────────────────────────────────────────────────────────────
    private val containerLayout: LinearLayout
    private val uploadBox: LinearLayout
    private val labelText: TextView
    private val uploadIcon: ImageView
    private val recyclerView: RecyclerView
    private val counterText: TextView
    private val adapter: FileListAdapter

    init {

        fun Context.getAttrColor(attr: Int): Int {
            val typedValue = TypedValue()
            theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }

        orientation = VERTICAL
        setPadding(0, 0, 0, 0)

        // ── root container ───────────────────────────────────────────────────
        containerLayout = this

        // ── dashed upload box ────────────────────────────────────────────────
        uploadBox = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = createDashedBackground(context)
            isClickable = true
            isFocusable = true
        }

        // label
        labelText = TextView(context).apply {
            text = context.getString(R.string.text_no_file_selected)
            textSize = 14f
            setTextColor(
                context.getAttrColor(R.attr.myColorOnSurface)
            )
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }

        // upload icon  (use a vector or fallback to a simple drawable)
        uploadIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_upload_m3u)
            imageTintList = ColorStateList.valueOf("#045DCC".toColorInt())
            layoutParams = LayoutParams(dp(24), dp(24))
        }

        uploadBox.addView(labelText)
        uploadBox.addView(uploadIcon)

        // ── file list ────────────────────────────────────────────────────────
        adapter = FileListAdapter(selectedFiles) { position ->
            selectedFiles.removeAt(position)
            adapter.notifyItemRemoved(position)
            updateState()
        }

        recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@FileUploadView.adapter
            visibility = View.GONE
            setPadding(0, dp(8), 0, 0)
        }

        // ── counter ──────────────────────────────────────────────────────────
        counterText = TextView(context).apply {
            textSize = 11f
            setTextColor(context.getAttrColor(R.attr.textHint))
            visibility = View.GONE
            setPadding(dp(4), dp(2), 0, 0)
        }

        addView(uploadBox)
        addView(recyclerView)
        addView(counterText)

        // ── click handler ────────────────────────────────────────────────────
        uploadBox.setOnClickListener {
            if (selectedFiles.size < MAX_FILES) {
                openFilePicker()
            } else {
                Toast.makeText(context, "Maximum $MAX_FILES files allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── register launcher (call from Activity/Fragment) ──────────────────────
    fun registerLauncher(fragment: androidx.fragment.app.Fragment) {
        filePickerLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isEmpty()) return@registerForActivityResult
            val remaining = MAX_FILES - selectedFiles.size
            val toAdd = uris.take(remaining)
            if (uris.size > remaining) {
                Toast.makeText(context, "Only $remaining more file(s) can be added (max $MAX_FILES)", Toast.LENGTH_SHORT).show()
            }
            toAdd.forEach { uri ->
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val insertStart = selectedFiles.size
            selectedFiles.addAll(toAdd)
            adapter.notifyItemRangeInserted(insertStart, toAdd.size)
            updateState()
        }
    }

    // ── open picker ──────────────────────────────────────────────────────────
    private fun openFilePicker() {
        if (!::filePickerLauncher.isInitialized) {
            Toast.makeText(context, "Call registerLauncher(activity) first", Toast.LENGTH_SHORT).show()
            return
        }
        filePickerLauncher.launch(arrayOf("*/*"))
    }

    // ── sync UI state ────────────────────────────────────────────────────────
    private fun updateState() {
        val count = selectedFiles.size
        if (count == 0) {
            labelText.text = context.getString(R.string.text_no_file_selected)
            labelText.setTextColor(context.getAttrColor(R.attr.myColorOnSurface))
            recyclerView.visibility = View.GONE
            counterText.visibility = View.GONE
        } else {
            labelText.text = "$count file${if (count > 1) "s" else ""} selected"
            labelText.setTextColor(Color.parseColor("#045DCC"))
            recyclerView.visibility = View.VISIBLE
            counterText.visibility = View.VISIBLE
            counterText.text = context.getString(R.string.text_files, count, MAX_FILES)
        }

        // disable tap when full
        uploadBox.alpha = if (count >= MAX_FILES) 0.55f else 1f
        onFilesChanged?.invoke(selectedFiles.toList())
    }

    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    // ── drawable helpers ─────────────────────────────────────────────────────
    private fun createDashedBackground(context: Context): android.graphics.drawable.Drawable {
        // GradientDrawable doesn't support dashed strokes directly in code,
        // so we use a LayerDrawable with a DashPathEffect via a custom drawable.
        return DashedBorderDrawable(
            borderColor = Color.parseColor("#045DCC"),
            fillColor  = context.getAttrColor(R.attr.colorBgBox),
            cornerRadius = dp(10).toFloat(),
            dashWidth = dp(8).toFloat(),
            dashGap   = dp(5).toFloat(),
            strokeWidth = dp(1.5f).toFloat()
        )
    }

    private fun createUploadDrawable(): android.graphics.drawable.Drawable {
        // Simple programmatic upload arrow (box + arrow up)
        return object : android.graphics.drawable.Drawable() {
            private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#C8A84B")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 5f
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }

            override fun draw(canvas: android.graphics.Canvas) {
                val b = bounds
                val w = b.width().toFloat()
                val h = b.height().toFloat()
                val cx = w / 2f

                // arrow up
                canvas.drawLine(cx, h * 0.55f, cx, h * 0.05f, paint)
                // arrowhead
                canvas.drawLine(cx, h * 0.05f, cx - w * 0.22f, h * 0.27f, paint)
                canvas.drawLine(cx, h * 0.05f, cx + w * 0.22f, h * 0.27f, paint)

                // box (U-shape from left bottom)
                val path = android.graphics.Path().apply {
                    moveTo(w * 0.3f, h * 0.55f)
                    lineTo(w * 0.05f, h * 0.55f)
                    lineTo(w * 0.05f, h * 0.95f)
                    lineTo(w * 0.95f, h * 0.95f)
                    lineTo(w * 0.95f, h * 0.55f)
                    lineTo(w * 0.7f,  h * 0.55f)
                }
                canvas.drawPath(path, paint)
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: android.graphics.ColorFilter?) { paint.colorFilter = cf }
            @Deprecated("Deprecated in Java")
            override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
        }
    }

    private fun dp(value: Number) =
        (value.toFloat() * context.resources.displayMetrics.density + 0.5f).toInt()
}

// ─────────────────────────────────────────────
// DashedBorderDrawable.kt
// ─────────────────────────────────────────────
class DashedBorderDrawable(
    private val borderColor: Int,
    private val fillColor: Int,
    private val cornerRadius: Float,
    private val dashWidth: Float,
    private val dashGap: Float,
    private val strokeWidth: Float
) : android.graphics.drawable.Drawable() {

    private val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = android.graphics.Paint.Style.FILL
    }

    private val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = this@DashedBorderDrawable.strokeWidth
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
    }

    override fun draw(canvas: android.graphics.Canvas) {
        val rect = android.graphics.RectF(bounds)
        val inset = strokeWidth / 2f
        rect.inset(inset, inset)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha; borderPaint.alpha = alpha
    }
    override fun setColorFilter(cf: android.graphics.ColorFilter?) {
        fillPaint.colorFilter = cf; borderPaint.colorFilter = cf
    }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
}

// ─────────────────────────────────────────────
// FileListAdapter.kt
// ─────────────────────────────────────────────
class FileListAdapter(
    private val files: List<Uri>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<FileListAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(android.R.id.text1)
        val removeBtn: ImageButton = view.findViewById(android.R.id.button1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 4), dp(ctx, 6), dp(ctx, 4), dp(ctx, 6))
            id = View.generateViewId()
        }

        val name = TextView(ctx).apply {
            id = android.R.id.text1
            textSize = 12f
            setTextColor(resources.getColor(R.color.color_primary))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btn = ImageButton(ctx).apply {
            id = android.R.id.button1
            setImageDrawable(createCloseDrawable(ctx))
            imageTintList = ColorStateList.valueOf(ctx.getAttrColor(R.attr.textHint))
            background = null
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 30), dp(ctx, 30))
        }

        row.addView(name)
        row.addView(btn)
        return VH(row)
    }

    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val uri = files[position]
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
        holder.nameText.text = "• $name"
        holder.removeBtn.setOnClickListener { onRemove(holder.bindingAdapterPosition) }
    }

    override fun getItemCount() = files.size

    private fun dp(ctx: Context, v: Int) =
        (v * ctx.resources.displayMetrics.density + 0.5f).toInt()

    private fun createCloseDrawable(ctx: Context): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            private val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#A0A8B0")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 4f
                strokeCap = android.graphics.Paint.Cap.ROUND
            }

            override fun draw(c: android.graphics.Canvas) {
                val b = bounds
                val pad = b.width() * 0.2f
                c.drawLine(b.left + pad, b.top + pad, b.right - pad, b.bottom - pad, p)
                c.drawLine(b.right - pad, b.top + pad, b.left + pad, b.bottom - pad, p)
            }

            override fun setAlpha(a: Int) { p.alpha = a }
            override fun setColorFilter(cf: android.graphics.ColorFilter?) { p.colorFilter = cf }
            @Deprecated("Deprecated in Java")
            override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
        }
    }



}
