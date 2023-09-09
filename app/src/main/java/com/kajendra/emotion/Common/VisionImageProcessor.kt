package com.kajendra.emotion.Common

import android.graphics.Bitmap
import com.google.firebase.ml.common.FirebaseMLException
import com.kajendra.emotion.UIComponents.GraphicsOverlay
import java.nio.ByteBuffer


interface VisionImageProcessor {

    /** Processes the images with the underlying machine learning models.  */

    @Throws(FirebaseMLException::class)
    fun process(data: ByteBuffer?, frameMetadata: FrameMetadata?, graphicOverlay: GraphicsOverlay)

    /** Processes the bitmap images.  */

    fun process(bitmap: Bitmap?, graphicOverlay: GraphicsOverlay?)

    /** Stops the underlying machine learning model and release resources.  */
    fun stop()
}