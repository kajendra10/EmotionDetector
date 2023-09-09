package com.kajendra.emotion.Visions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.kajendra.emotion.Interfaces.FaceDetectStatus
import com.kajendra.emotion.Models.RectModel
import com.kajendra.emotion.UIComponents.GraphicsOverlay

class FaceGraphic internal constructor(overlay: GraphicsOverlay?, private val firebaseVisionFace: FirebaseVisionFace, private val facing: Int, private val overlayBitmap: Bitmap) : GraphicsOverlay.Graphic(overlay!!) {
    private val facePositionPaint: Paint
    private val idPaint: Paint
    private val boxPaint: Paint

    var faceDetectStatus: FaceDetectStatus? = null

    override fun draw(canvas: Canvas?) {
        val face = firebaseVisionFace ?: return

        val x = translateX(face.boundingBox.centerX().toFloat())
        val y = translateY(face.boundingBox.centerY().toFloat())

        // Draws a bounding box around the face.
        val xOffset = scaleX(face.boundingBox.width() / 2.0f)
        val yOffset = scaleY(face.boundingBox.height() / 2.0f)
        val left = x - xOffset
        val top = y - yOffset
        val right = x + xOffset
        val bottom = y + yOffset
        canvas!!.drawRect(left, top, right, bottom, boxPaint)
        if (left < 190 && top < 450 && right > 850 && bottom > 1050) {
            if (faceDetectStatus != null) faceDetectStatus!!.onFaceLocated(RectModel(left, top, right, bottom))
        } else {
            if (faceDetectStatus != null) faceDetectStatus!!.onFaceNotLocated()
        }
    }

    companion object {
        private const val ID_TEXT_SIZE = 30.0f
        private const val BOX_STROKE_WIDTH = 5.0f
    }

    init {
        val selectedColor = Color.GREEN
        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor
        idPaint = Paint()
        idPaint.color = selectedColor
        idPaint.textSize = ID_TEXT_SIZE
        boxPaint = Paint()
        boxPaint.color = selectedColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH
    }
}