package com.kajendra.emotion
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var onboardingItemsAdapter: OnboardingItemsAdapter
    private lateinit var indicatorsContainer: LinearLayout
    var sharedPreferences: SharedPreferences? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setOnboardingItems()
        setupIndicators()
        setCurrentIndicator(0)

        if(restorePrefData()) {
            val i= Intent(applicationContext, DetectionActivity::class.java)
            startActivity(i)
            finish()
        } else {
            (savePrefData())
        }
    }

    private fun setOnboardingItems() {
        onboardingItemsAdapter = OnboardingItemsAdapter(
            listOf(
                OnboardingMaterial(
                    onboardingImage = R.drawable.face,
                    title = "Realtime Detection",
                    description = "Detect your face emotions with our emotion detector app that uses on-device machine learning kit."
                ),
                OnboardingMaterial(
                    onboardingImage = R.drawable.save,
                    title = "Snap Your Image",
                    description = "Your detected facial image will be stored in our firebase realtime database storage which allows companies to track your emotions."
                ),
                OnboardingMaterial(
                    onboardingImage = R.drawable.time,
                    title = "Remind Yourself",
                    description = "While setting your own time reminder, you can always get notified and comeback to detect your face again."
                )
            )
        )
        val onboardingViewPager = findViewById<ViewPager2>(R.id.onboarding_viewpager)
        onboardingViewPager.adapter =  onboardingItemsAdapter
        onboardingViewPager.registerOnPageChangeCallback(object :
        ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
            }
        })
        (onboardingViewPager.getChildAt(0) as RecyclerView).overScrollMode =
            RecyclerView.OVER_SCROLL_NEVER
        findViewById<ImageView>(R.id.image_nxt).setOnClickListener {
            if(onboardingViewPager.currentItem+1 < onboardingItemsAdapter.itemCount) {
                onboardingViewPager.currentItem += 1
            } else {
                navigateToDetectionActivity()
            }
        }
        findViewById<TextView>(R.id.skip_txt).setOnClickListener {
            navigateToDetectionActivity()
        }
        findViewById<MaterialButton>(R.id.gts_button).setOnClickListener {
            navigateToDetectionActivity()
        }

    }

    private fun navigateToDetectionActivity() {
        startActivity(Intent(applicationContext,DetectionActivity::class.java))
        finish()
    }



    private fun setupIndicators() {
        indicatorsContainer = findViewById(R.id.indicator_cont)
        val indicators = arrayOfNulls<ImageView>(onboardingItemsAdapter.itemCount)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layoutParams.setMargins(8, 0, 8, 0)
        for(i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i]?.let {
                it.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.inactive_indicator_bg
                    )
                )
                it.layoutParams = layoutParams
                indicatorsContainer.addView(it)
            }
        }
    }

    private fun savePrefData() {

        sharedPreferences = applicationContext.getSharedPreferences("pref", MODE_PRIVATE)
        val editor = sharedPreferences!!.edit()
        editor.putBoolean("FirstTimeRun", true)
        editor.apply()
    }

    private fun restorePrefData(): Boolean {

        sharedPreferences = applicationContext.getSharedPreferences("pref", MODE_PRIVATE)
        return sharedPreferences!!.getBoolean("FirstTimeRun", false)
    }

    private fun setCurrentIndicator(position: Int) {
        val childCount = indicatorsContainer.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorsContainer.getChildAt(i) as ImageView
            if (i == position) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.active_indicator_bg
                    )
                )
            }else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.inactive_indicator_bg
                    )
                )
            }
        }
    }
}