package com.kajendra.emotion
import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.google.firebase.auth.FirebaseAuth


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser

        Handler().postDelayed({

            if (user != null) {
                val dtIntent = Intent(this, DetectionActivity::class.java)
                startActivity(dtIntent)
                finish()
            } else {
                val mnIntent = Intent(this, MainActivity::class.java)
                startActivity(mnIntent)
                finish()
            }

        }, 5000)

    }
}