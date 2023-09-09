package com.kajendra.emotion.Common

import android.graphics.Bitmap
import androidx.annotation.GuardedBy
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.kajendra.emotion.Common.BitmapUtils.getBitmap
import com.kajendra.emotion.UIComponents.GraphicsOverlay
import java.nio.ByteBuffer

abstract class VisionProcessorBase<T> : VisionImageProcessor {

    // keep the data of latest images and its meta data
    @GuardedBy("this")
    private var latestImage: ByteBuffer? = null

    @GuardedBy("this")
    private var latestImageMetaData: FrameMetadata? = null


    // keep the data of latest images and its meta data with are is under process
    @GuardedBy("this")
    private var processingImage: ByteBuffer? = null

    @GuardedBy("this")
    private var processingMetaData: FrameMetadata? = null


    override fun process(
        data: ByteBuffer?,
        frameMetadata: FrameMetadata?,
        graphicOverlay: GraphicsOverlay
    ) {
        latestImage = data
        latestImageMetaData = frameMetadata
        if (processingImage == null && processingMetaData == null) {
            processLatestImage(graphicOverlay!!)
        }
    }

    override fun process(bitmap: Bitmap?, graphicOverlay: GraphicsOverlay?) {
        detectInVisionImage(null /* bitmap */, FirebaseVisionImage.fromBitmap(bitmap!!), null,
            graphicOverlay!!)
    }

    @Synchronized
    private fun processLatestImage(graphicOverlay: GraphicsOverlay) {
        processingImage = latestImage
        processingMetaData = latestImageMetaData
        latestImage = null
        latestImageMetaData = null
        if (processingImage != null && processingMetaData != null) {
            processImage(processingImage!!, processingMetaData!!, graphicOverlay)
        }
    }

    private fun processImage(
        data: ByteBuffer, frameMetadata: FrameMetadata,
        graphicOverlay: GraphicsOverlay) {
        val metadata = FirebaseVisionImageMetadata.Builder()
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setWidth(frameMetadata.width)
            .setHeight(frameMetadata.height)
            .setRotation(frameMetadata.rotation)
            .build()
        val bitmap = getBitmap(data, frameMetadata)
        detectInVisionImage(
            bitmap, FirebaseVisionImage.fromByteBuffer(data, metadata), frameMetadata,
            graphicOverlay)
    }

    private fun detectInVisionImage(
        originalCameraImage: Bitmap?,
        image: FirebaseVisionImage,
        metadata: FrameMetadata?,
        graphicOverlay: GraphicsOverlay) {
        detectInImage(image)
            .addOnSuccessListener { results ->
                this@VisionProcessorBase.onSuccess(originalCameraImage, results,
                    metadata!!,
                    graphicOverlay)
                processLatestImage(graphicOverlay)
            }
            .addOnFailureListener { e -> this@VisionProcessorBase.onFailure(e) }
    }

    override fun stop() {}
    protected abstract fun detectInImage(image: FirebaseVisionImage?): Task<T>

    protected abstract fun onSuccess(
        originalCameraImage: Bitmap?,
        results: T,
        frameMetadata: FrameMetadata,
        graphicOverlay: GraphicsOverlay)

    protected abstract fun onFailure(e: Exception)
}