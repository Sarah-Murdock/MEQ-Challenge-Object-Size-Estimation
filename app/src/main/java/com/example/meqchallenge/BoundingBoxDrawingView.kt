package com.example.meqchallenge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.util.LinkedList
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var results: List<DetectedObject>? = LinkedList()
    private var boxPaint = Paint()

    private var scaleFactor: Float = 1f

    init {
        initPaints()
    }

    private fun initPaints() {
        boxPaint.color = ContextCompat.getColor(context!!, R.color.teal_200)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if(results != null) {

            for (result in results!!) {
                val top = result.top * scaleFactor
                val bottom = result.bottom * scaleFactor
                val left = result.left * scaleFactor
                val right = result.right * scaleFactor

                // Draw bounding box around detected objects
                val drawableRect = RectF(left, top, right, bottom)
                canvas.drawRect(drawableRect, boxPaint)
            }
        }
    }

    fun setResults(detectionResults: List<DetectedObject>?, imageHeight: Int, imageWidth: Int, ) {
        results = detectionResults
        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }
}
