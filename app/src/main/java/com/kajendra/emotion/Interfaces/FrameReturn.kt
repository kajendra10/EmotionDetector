package com.kajendra.emotion.Interfaces

import android.graphics.Bitmap
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.kajendra.emotion.Common.FrameMetadata
import com.kajendra.emotion.UIComponents.GraphicsOverlay

interface FrameReturn {
    fun onFrame(
        image: Bitmap?,
        face: FirebaseVisionFace?,
        frameMetadata: FrameMetadata?,
        graphicOverlay: GraphicsOverlay?
    )
}