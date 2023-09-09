package com.kajendra.emotion
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.kajendra.emotion.Notification.*
import com.kajendra.emotion.databinding.ActivityReminderBinding
import java.util.*


class ReminderActivity : AppCompatActivity() {

    private lateinit var binding : ActivityReminderBinding
    private lateinit var auth: FirebaseAuth
    private var progressBar: ProgressBar? = null


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createNotificationChannel()
        binding.setButton.setOnClickListener { scheduleNotification() }


        auth = FirebaseAuth.getInstance()
        val email = intent.getStringExtra("email")
        progressBar = findViewById<ProgressBar>(R.id.signOutProgress)
        findViewById<TextView>(R.id.userName).text = email

    }

    private fun scheduleNotification() {

        val intent = Intent(applicationContext, Notification::class.java)
        val title = binding.titleName.text.toString()
        val desc = binding.noteDesc.text.toString()
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, desc)

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = getTime()
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
        showAlert(time, title, desc)
    }

    private fun showAlert(time: Long, title: String, desc: String) {

        val date = Date(time)
        val dateFormat = android.text.format.DateFormat.getDateFormat(applicationContext)
        val timeformat = android.text.format.DateFormat.getTimeFormat(applicationContext)

        AlertDialog.Builder(this)
            .setTitle("Notification Schedule")
            .setMessage("Title: " + title +
                        "\nMessage: " + desc +
                        "\nAt " + dateFormat.format(date) + " " + timeformat.format(date))
            .setPositiveButton("Okay"){_,_ ->}
            .show()


    }

    private fun getTime(): Long {

        val minute = binding.timePicker.minute
        val hour = binding.timePicker.hour
        val day = binding.datePicker.dayOfMonth
        val month = binding.datePicker.month
        val year = binding.datePicker.year

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute)
        return calendar.timeInMillis

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {

        val name ="Notification Channel"
        val desc = "A Description of the Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

    }
}
