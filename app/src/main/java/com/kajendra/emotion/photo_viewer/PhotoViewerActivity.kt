package com.kajendra.emotion.photo_viewer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import com.kajendra.emotion.Base.Cons
import com.kajendra.emotion.Base.PublicMethods
import com.kajendra.emotion.R
import com.kajendra.emotion.ReminderActivity
import org.greenrobot.eventbus.EventBus

class PhotoViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        EventBus.getDefault().post("Return")

        if (intent.hasExtra(Cons.IMG_EXTRA_KEY)) {
            val imageView = findViewById<ImageView>(R.id.image)
            val imagePath = intent.getStringExtra(Cons.IMG_EXTRA_KEY)
            imageView.setImageBitmap(PublicMethods.getBitmapByPath(imagePath, Cons.IMG_FILE))
        }

        val buttonRemind = findViewById<Button>(R.id.buttonRemind)
        buttonRemind.setOnClickListener {
            val intent = Intent(this, ReminderActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}