package com.safesphere

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.safesphere.ml.NotificationAccess
import com.safesphere.ml.TextClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var classifier: TextClassifier? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var tvPermissionStatus: TextView
    private lateinit var btnGrantPermission: Button
    private lateinit var etMessageInput: EditText
    private lateinit var btnSampleSafe: Button
    private lateinit var btnSampleAdult: Button
    private lateinit var btnClassify: Button
    private lateinit var tvClassificationResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        etMessageInput = findViewById(R.id.etMessageInput)
        btnSampleSafe = findViewById(R.id.btnSampleSafe)
        btnSampleAdult = findViewById(R.id.btnSampleAdult)
        btnClassify = findViewById(R.id.btnClassify)
        tvClassificationResult = findViewById(R.id.tvClassificationResult)

        // Initialize classifier in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                classifier = TextClassifier(applicationContext)
                withContext(Dispatchers.Main) {
                    tvClassificationResult.text = "Classifier model loaded and ready! (Threshold = ${classifier?.threshold})"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvClassificationResult.text = "Error loading classifier: ${e.message}"
                }
            }
        }

        btnGrantPermission.setOnClickListener {
            NotificationAccess.openSettings(this)
        }

        btnSampleSafe.setOnClickListener {
            etMessageInput.setText("Bhai physics ke notes bhej dena kal exam hai")
        }

        btnSampleAdult.setOnClickListener {
            etMessageInput.setText("nudes bhej na, kisi ko nahi bataunga private pic send kar")
        }

        btnClassify.setOnClickListener {
            val text = etMessageInput.text.toString().trim()
            if (text.isEmpty()) {
                tvClassificationResult.text = "Please enter some text to classify."
                return@setOnClickListener
            }

            val model = classifier
            if (model == null) {
                tvClassificationResult.text = "Classifier is still loading, please wait..."
                return@setOnClickListener
            }

            scope.launch {
                val startTime = System.currentTimeMillis()
                val score = withContext(Dispatchers.Default) {
                    model.classify(text)
                }
                val latency = System.currentTimeMillis() - startTime
                val isExplicit = score > model.threshold

                val resultText = buildString {
                    append("Message: \"$text\"\n")
                    append("Explicit Score: %.4f (Threshold: %.2f)\n".format(score, model.threshold))
                    append("Decision: %s\n".format(if (isExplicit) "🚨 FLAGGED AS ADULT / EXPLICIT" else "✅ NORMAL / SAFE"))
                    append("Inference Time: ${latency} ms")
                }

                tvClassificationResult.text = resultText
                if (isExplicit) {
                    tvClassificationResult.setTextColor(Color.parseColor("#C53030"))
                } else {
                    tvClassificationResult.setTextColor(Color.parseColor("#22543D"))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val granted = NotificationAccess.isGranted(this)
        if (granted) {
            tvPermissionStatus.text = "Status: ✅ Notification Access GRANTED"
            tvPermissionStatus.setTextColor(Color.parseColor("#38A169"))
            btnGrantPermission.text = "Permission Granted (Active)"
            btnGrantPermission.isEnabled = false
        } else {
            tvPermissionStatus.text = "Status: ❌ Notification Access NOT GRANTED"
            tvPermissionStatus.setTextColor(Color.parseColor("#E53E3E"))
            btnGrantPermission.text = "Grant Notification Access"
            btnGrantPermission.isEnabled = true
        }
    }

    override fun onDestroy() {
        classifier?.close()
        super.onDestroy()
    }
}
