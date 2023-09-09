package com.kajendra.emotion.Common
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.kajendra.emotion.UIComponents.GraphicsOverlay

class CameraImageGraphic(overlay: GraphicsOverlay?, private val bitmap: Bitmap) : GraphicsOverlay.Graphic(overlay!!) {

    override fun draw(canvas: Canvas?) {
        canvas!!.drawBitmap(bitmap, null, Rect(0, 0, canvas!!.width, canvas!!.height), null)
    }
}