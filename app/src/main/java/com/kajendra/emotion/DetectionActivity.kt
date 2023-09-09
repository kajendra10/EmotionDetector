package com.kajendra.emotion
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.storage.FirebaseStorage
import com.hadi.emojiratingbar.EmojiRatingBar
import com.hadi.emojiratingbar.RateStatus
import com.kajendra.emotion.Base.BaseActivity
import com.kajendra.emotion.Base.Cons
import com.kajendra.emotion.Base.PublicMethods
import com.kajendra.emotion.Base.Screenshot
import com.kajendra.emotion.Common.CameraSource
import com.kajendra.emotion.Common.FrameMetadata
import com.kajendra.emotion.Interfaces.FaceDetectStatus
import com.kajendra.emotion.Interfaces.FrameReturn
import com.kajendra.emotion.Models.RectModel
import com.kajendra.emotion.UIComponents.CameraPreview
import com.kajendra.emotion.UIComponents.GraphicsOverlay
import com.kajendra.emotion.Visions.FaceDetectionProcessor
import com.kajendra.emotion.photo_viewer.PhotoViewerActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID

class DetectionActivity : BaseActivity(), ActivityCompat.OnRequestPermissionsResultCallback, FrameReturn,
    FaceDetectStatus {

    var originalImage: Bitmap? = null
    private var cameraSource: CameraSource? = null
    private var preview: CameraPreview? = null
    private var graphicOverlay: GraphicsOverlay? = null
    private var faceFrame: ImageView? = null
    private var test: ImageView? = null
    private var takePhoto: Button? = null
    private var smile_rating: EmojiRatingBar? = null

    private var firebaseStorage =  FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection)

        test = findViewById(R.id.test)
        preview = findViewById(R.id.preview)
        takePhoto = findViewById(R.id.takePhoto)
        graphicOverlay = findViewById(R.id.overlay)
        smile_rating = findViewById(R.id.smile_rating)

        if (PublicMethods.allPermissionsGranted(this)) {
            createCameraSource()
        } else {
            PublicMethods.getRuntimePermissions(this)
        }

        takePhoto!!.setOnClickListener(View.OnClickListener { v: View? -> takePhoto() })
    }



    @Subscribe
    fun OnAddSelected(add : String?) {
        if (add == "Return") {
            takePhoto!!.visibility = View.VISIBLE
            test!!.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this)
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (cameraSource != null) {
            cameraSource!!.release()
        }
        EventBus.getDefault().unregister(this)
    }

    private fun takePhoto() {
        takePhoto!!.visibility = View.GONE
        test!!.visibility = View.GONE

        val b = Screenshot.takescreenshotOfRootView(test!!)
        test!!.setImageBitmap(b)

        //Store image to firebase
        val path = PublicMethods.saveToInternalStorage(b, Cons.IMG_FILE, mActivity)


        //Image quality
        val stream = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val image = stream.toByteArray()

        //Receiving image from mobile and storing data into db server
        firebaseStorage.reference.child("emotion results/${UUID.randomUUID()}").putBytes(image).addOnSuccessListener {

        }.addOnFailureListener{

        }.addOnProgressListener{

        }

        startActivity(
            Intent(mActivity, PhotoViewerActivity::class.java)
                .putExtra(Cons.IMG_EXTRA_KEY, path))
    }

    private fun createCameraSource() {
        if (cameraSource == null) {
            cameraSource = CameraSource(this, graphicOverlay!!)
        }

        try {
            val processor = FaceDetectionProcessor(resources)
            processor.frameHandler = this
            processor.faceDetectStatus = this
            cameraSource!!.setMachineLearningFrameProcessor(processor)
        } catch (e: Exception) {
            Log.e(TAG, "Can not create image processor: $FACE_DETECTION", e)
            Toast.makeText(
                applicationContext,
                "Can not create image processor: " + e.message,
                Toast.LENGTH_LONG)
                .show()
        }
    }

    public override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        preview!!.stop()
    }

    companion object {
        private const val FACE_DETECTION = "Face Detection"
        private const val TAG = "MLKitTAG"
    }

    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                preview!!.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                cameraSource!!.release()
                cameraSource = null
            }
        }
    }


    override fun onFrame(image: Bitmap?, face: FirebaseVisionFace?, frameMetadata: FrameMetadata?, graphicOverlay: GraphicsOverlay?) {
        originalImage = image
        if (face!!.leftEyeOpenProbability < 0.4) {
            findViewById<View>(R.id.rightEyeStatus).visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.rightEyeStatus).visibility = View.INVISIBLE
        }
        if (face.rightEyeOpenProbability < 0.4) {
            findViewById<View>(R.id.leftEyeStatus).visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.leftEyeStatus).visibility = View.INVISIBLE
        }
        var smile = 0

        if (face.smilingProbability > .8) {
            smile = RateStatus.GREAT.ordinal
        } else if (face.smilingProbability <= .8 && face.smilingProbability > .6) {
            smile = RateStatus.GOOD.ordinal
        } else if (face.smilingProbability <= .6 && face.smilingProbability > .4) {
            smile = RateStatus.OKAY.ordinal
        } else if (face.smilingProbability <= .4 && face.smilingProbability > .2) {
            smile = RateStatus.BAD.ordinal
        } else if (face.smilingProbability <= .2 && face.smilingProbability > .1) {
            smile = RateStatus.AWFUL.ordinal
        } else {
            RateStatus.EMPTY.ordinal
        }
        smile_rating!!.setCurrentRateStatus(RateStatus.values()[smile])
    }


    override fun onFaceLocated(rectModel: RectModel?) {

    }

    override fun onFaceNotLocated() {

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (PublicMethods.allPermissionsGranted(this)) {
            createCameraSource()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}