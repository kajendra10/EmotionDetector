package com.kajendra.emotion.Common
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PreviewCallback
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import com.google.android.gms.common.images.Size
import com.kajendra.emotion.UIComponents.GraphicsOverlay
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import kotlin.jvm.Throws

@SuppressLint("MissingPermission")
class CameraSource(protected var activity: Activity, private val graphicOverlay: GraphicsOverlay) {

    private var camera: Camera? = null
    var cameraFacing = CAMERA_FACING_FRONT
    private var rotation = 0
    var previewSize: Size? = null
        private set
    private val requestedFps = 20.0f
    private val requestedAutoFocus = true
    private var dummySurfaceTexture: SurfaceTexture? = null
    private var usingSurfaceTexture = false
    private var processingThread: Thread? = null
    private val processingRunnable: FrameProcessingRunnable
    private val processorLock = Any()
    private var frameProcessor: VisionImageProcessor? = null
    private val bytesToByteBuffer: MutableMap<ByteArray, ByteBuffer> = IdentityHashMap()

    fun release() {
        synchronized(processorLock) {
            stop()
            processingRunnable.release()
            cleanScreen()
            if (frameProcessor != null) {
                frameProcessor!!.stop()
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.CAMERA)
    @Synchronized
    @Throws(IOException::class)
    fun start(): CameraSource {
        if (camera != null) {
            return this
        }
        camera = createCamera()
        dummySurfaceTexture = SurfaceTexture(DUMMY_TEXTURE_NAME)
        camera!!.setPreviewTexture(dummySurfaceTexture)
        usingSurfaceTexture = true
        camera!!.startPreview()
        processingThread = Thread(processingRunnable)
        processingRunnable.setActive(true)
        processingThread!!.start()
        return this
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @Synchronized
    @Throws(IOException::class)
    fun start(surfaceHolder: SurfaceHolder?): CameraSource {
        if (camera != null) {
            return this
        }
        camera = createCamera()
        camera!!.setPreviewDisplay(surfaceHolder)
        camera!!.startPreview()
        processingThread = Thread(processingRunnable)
        processingRunnable.setActive(true)
        processingThread!!.start()
        usingSurfaceTexture = false
        return this
    }


    @Synchronized
    fun stop() {
        processingRunnable.setActive(false)
        if (processingThread != null) {
            try {

                processingThread!!.join()
            } catch (e: InterruptedException) {
                Log.d(TAG, "Frame processing thread interrupted on release.")
            }
            processingThread = null
        }
        if (camera != null) {
            camera!!.stopPreview()
            camera!!.setPreviewCallbackWithBuffer(null)
            try {
                if (usingSurfaceTexture) {
                    camera!!.setPreviewTexture(null)
                } else {
                    camera!!.setPreviewDisplay(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear camera preview: $e")
            }
            camera!!.release()
            camera = null
        }

        // Release the reference to any image buffers, since these will no longer be in use.
        bytesToByteBuffer.clear()
    }

    @Synchronized
    fun setFacing(facing: Int) {
        require(!(facing != CAMERA_FACING_BACK && facing != CAMERA_FACING_FRONT)) { "Invalid camera: $facing" }
        cameraFacing = facing
    }

    @SuppressLint("InlinedApi")
    @Throws(IOException::class)
    private fun createCamera(): Camera {
        val requestedCameraId = getIdForRequestedCamera(cameraFacing)
        if (requestedCameraId == -1) {
            throw IOException("Could not find requested camera.")
        }
        val camera = Camera.open(requestedCameraId)
        val sizePair = selectSizePair(camera, requestedPreviewWidth, requestedPreviewHeight)
            ?: throw IOException("Could not find suitable preview size.")
        val pictureSize = sizePair.pictureSize()
        previewSize = sizePair.previewSize()
        val previewFpsRange = selectPreviewFpsRange(camera, requestedFps)
            ?: throw IOException("Could not find suitable preview frames per second range.")
        val parameters = camera.parameters
        if (pictureSize != null) {
            parameters.setPictureSize(pictureSize.width, pictureSize.height)
        }
        parameters.setPreviewSize(previewSize!!.width, previewSize!!.height)
        parameters.setPreviewFpsRange(
            previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
            previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX])
        parameters.previewFormat = ImageFormat.NV21
        setRotation(camera, parameters, requestedCameraId)
        if (requestedAutoFocus) {
            if (parameters
                    .supportedFocusModes
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
            } else {
                Log.i(TAG, "Camera auto focus is not supported on this device.")
            }
        }
        camera.parameters = parameters
        camera.setPreviewCallbackWithBuffer(CameraPreviewCallback())
        camera.addCallbackBuffer(createPreviewBuffer(previewSize))
        camera.addCallbackBuffer(createPreviewBuffer(previewSize))
        camera.addCallbackBuffer(createPreviewBuffer(previewSize))
        camera.addCallbackBuffer(createPreviewBuffer(previewSize))
        return camera
    }

    private class SizePair internal constructor(
        previewSize: Camera.Size,
        pictureSize: Camera.Size?) {
        private val preview: Size
        private var picture: Size? = null

        fun previewSize(): Size {
            return preview
        }

        fun pictureSize(): Size? {
            return picture
        }

        init {
            preview = Size(previewSize.width, previewSize.height)
            if (pictureSize != null) {
                picture = Size(pictureSize.width, pictureSize.height)
            }
        }
    }


    private fun setRotation(camera: Camera, parameters: Camera.Parameters, cameraId: Int) {
        val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var degrees = 0
        val rotation = windowManager.defaultDisplay.rotation
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
            else -> Log.e(TAG, "Bad rotation value: $rotation")
        }
        val cameraInfo = CameraInfo()
        Camera.getCameraInfo(cameraId, cameraInfo)
        val angle: Int
        val displayAngle: Int
        if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            angle = (cameraInfo.orientation + degrees) % 360
            displayAngle = (360 - angle) % 360 // compensate for it being mirrored
        } else { // back-facing
            angle = (cameraInfo.orientation - degrees + 360) % 360
            displayAngle = angle
        }

        // This corresponds to the rotation constants.
        this.rotation = angle / 90
        camera.setDisplayOrientation(displayAngle)
        parameters.setRotation(angle)
    }

    @SuppressLint("InlinedApi")
    private fun createPreviewBuffer(previewSize: Size?): ByteArray {
        val bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21)
        val sizeInBits = previewSize!!.height.toLong() * previewSize.width * bitsPerPixel
        val bufferSize = Math.ceil(sizeInBits / 8.0).toInt() + 1

        val byteArray = ByteArray(bufferSize)
        val buffer = ByteBuffer.wrap(byteArray)
        check(!(!buffer.hasArray() || buffer.array() != byteArray)) {

            "Failed to create valid buffer for camera source."
        }
        bytesToByteBuffer[byteArray] = buffer
        return byteArray
    }


    private inner class CameraPreviewCallback : PreviewCallback {
        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            processingRunnable.setNextFrame(data, camera)
        }
    }

    fun setMachineLearningFrameProcessor(processor: VisionImageProcessor?) {
        synchronized(processorLock) {
            cleanScreen()
            if (frameProcessor != null) {
                frameProcessor!!.stop()
            }
            frameProcessor = processor
        }
    }

    private inner class FrameProcessingRunnable internal constructor() : Runnable {
        // This lock guards all of the member variables below.
        var lock = Object()

        private var active = true

        private var pendingFrameData: ByteBuffer? = null

        @SuppressLint("Assert")
        fun release() {
            assert(processingThread!!.state == Thread.State.TERMINATED)
        }

        fun setActive(active: Boolean) {
            synchronized(lock) {
                this.active = active
                lock.notifyAll()
            }
        }

        fun setNextFrame(data: ByteArray, camera: Camera) {
            synchronized(lock) {
                if (pendingFrameData != null) {
                    camera.addCallbackBuffer(pendingFrameData!!.array())
                    pendingFrameData = null
                }
                if (!bytesToByteBuffer.containsKey(data)) {
                    Log.d(
                        TAG, "Skipping frame. Could not find ByteBuffer associated with the image "
                                + "data from the camera.")
                    return
                }
                pendingFrameData = bytesToByteBuffer[data]

                lock.notifyAll()
            }
        }


        @SuppressLint("InlinedApi")
        override fun run() {
            var data: ByteBuffer?
            while (true) {
                synchronized(lock) {
                    while (active && pendingFrameData == null) {
                        try {

                            lock.wait()
                        } catch (e: InterruptedException) {
                            Log.d(TAG, "Frame processing loop terminated.", e)
                            return
                        }
                    }
                    if (!active) {

                        return
                    }

                    data = pendingFrameData
                    pendingFrameData = null
                }


                try {
                    synchronized(processorLock) {
                        Log.d(TAG, "Process an image")
                        frameProcessor!!.process(
                            data,
                            FrameMetadata.Builder()
                                .setWidth(previewSize!!.width)
                                .setHeight(previewSize!!.height)
                                .setRotation(rotation)
                                .setCameraFacing(cameraFacing)
                                .build(),
                            graphicOverlay)
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Exception thrown from receiver.", t)
                } finally {
                    camera!!.addCallbackBuffer(data!!.array())
                }
            }
        }
    }


    private fun cleanScreen() {
        graphicOverlay.clear()
    }

    companion object {
        @SuppressLint("InlinedApi")
        val CAMERA_FACING_BACK = CameraInfo.CAMERA_FACING_BACK

        @SuppressLint("InlinedApi")
        val CAMERA_FACING_FRONT = CameraInfo.CAMERA_FACING_FRONT
        private const val TAG = "MIFYPApp:CameraSource"

        private const val DUMMY_TEXTURE_NAME = 100

        private const val ASPECT_RATIO_TOLERANCE = 0.01f
        const val requestedPreviewWidth = 480
        const val requestedPreviewHeight = 360

        private fun getIdForRequestedCamera(facing: Int): Int {
            val cameraInfo = CameraInfo()
            for (i in 0 until Camera.getNumberOfCameras()) {
                Camera.getCameraInfo(i, cameraInfo)
                if (cameraInfo.facing == facing) {
                    return i
                }
            }
            return -1
        }

        private fun selectSizePair(camera: Camera, desiredWidth: Int, desiredHeight: Int): SizePair? {
            val validPreviewSizes = generateValidPreviewSizeList(camera)

            var selectedPair: SizePair? = null
            var minDiff = Int.MAX_VALUE
            for (sizePair in validPreviewSizes) {
                val size = sizePair.previewSize()
                val diff = Math.abs(size.width - desiredWidth) + Math.abs(size.height - desiredHeight)
                if (diff < minDiff) {
                    selectedPair = sizePair
                    minDiff = diff
                }
            }
            return selectedPair
        }

        private fun generateValidPreviewSizeList(camera: Camera): List<SizePair> {
            val parameters = camera.parameters
            val supportedPreviewSizes = parameters.supportedPreviewSizes
            val supportedPictureSizes = parameters.supportedPictureSizes
            val validPreviewSizes: MutableList<SizePair> = ArrayList()
            for (previewSize in supportedPreviewSizes) {
                val previewAspectRatio = previewSize.width.toFloat() / previewSize.height.toFloat()

                for (pictureSize in supportedPictureSizes) {
                    val pictureAspectRatio = pictureSize.width.toFloat() / pictureSize.height.toFloat()
                    if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                        validPreviewSizes.add(SizePair(previewSize, pictureSize))
                        break
                    }
                }
            }


            if (validPreviewSizes.size == 0) {
                Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size")
                for (previewSize in supportedPreviewSizes) {

                    validPreviewSizes.add(SizePair(previewSize, null))
                }
            }
            return validPreviewSizes
        }


        @SuppressLint("InlinedApi")
        private fun selectPreviewFpsRange(camera: Camera, desiredPreviewFps: Float): IntArray? {

            val desiredPreviewFpsScaled = (desiredPreviewFps * 1000.0f).toInt()

            var selectedFpsRange: IntArray? = null
            var minDiff = Int.MAX_VALUE
            val previewFpsRangeList = camera.parameters.supportedPreviewFpsRange
            for (range in previewFpsRangeList) {
                val deltaMin = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]
                val deltaMax = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
                val diff = Math.abs(deltaMin) + Math.abs(deltaMax)
                if (diff < minDiff) {
                    selectedFpsRange = range
                    minDiff = diff
                }
            }
            return selectedFpsRange
        }
    }

    init {
        graphicOverlay.clear()
        processingRunnable = FrameProcessingRunnable()
        if (Camera.getNumberOfCameras() == 1) {
            val cameraInfo = CameraInfo()
            Camera.getCameraInfo(0, cameraInfo)
            cameraFacing = cameraInfo.facing
        }
    }
}