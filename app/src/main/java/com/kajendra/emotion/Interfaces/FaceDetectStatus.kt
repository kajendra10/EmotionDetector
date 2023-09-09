package com.kajendra.emotion.Interfaces

import com.kajendra.emotion.Models.RectModel

interface FaceDetectStatus {
    fun onFaceLocated(rectModel: RectModel?)
    fun onFaceNotLocated()
}