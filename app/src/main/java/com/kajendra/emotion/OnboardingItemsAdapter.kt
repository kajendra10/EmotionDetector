package com.kajendra.emotion
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingItemsAdapter(private val onboardingItems: List<OnboardingMaterial>) :
RecyclerView.Adapter<OnboardingItemsAdapter.OnboardingItemViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingItemViewHolder {
        return OnboardingItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.onboarding_material_container,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OnboardingItemViewHolder, position: Int) {
        holder.bind(onboardingItems[position])
    }

    override fun getItemCount(): Int {
        return onboardingItems.size
    }

    inner class OnboardingItemViewHolder(view: View): RecyclerView.ViewHolder(view) {

        private val imageOnboarding = view.findViewById<ImageView>(R.id.image_onboarding)
        private val textTitle = view.findViewById<TextView>(R.id.text_title)
        private val textDesc = view.findViewById<TextView>(R.id.text_desc)

        fun bind(onboardingMaterial: OnboardingMaterial){
            imageOnboarding.setImageResource(onboardingMaterial.onboardingImage)
            textTitle.text = onboardingMaterial.title
            textDesc.text = onboardingMaterial.description
        }
    }
}