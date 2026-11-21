package com.safesphere.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.safesphere.data.ApiClient
import com.safesphere.data.EventsUploadScheduler
import com.safesphere.data.HeartbeatScheduler
import com.safesphere.data.PairGenerateRequest
import com.safesphere.data.TokenStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * Teen's phone: asks the server for a pairing code and shows it (valid 10 minutes).
 * Saves the teen token, starts heartbeat + events upload, then waits (polling pairState)
 * until the parent has entered the code, and moves on to Step 1 by itself.
 */
class TeenCodeActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val store by lazy { TokenStore(this) }
    private var busy = false
    private var timer: CountDownTimer? = null
    private var pollJob: Job? = null
    private var moved = false

    private lateinit var boxes: CodeBoxes
    private lateinit var status: TextView
    private lateinit var newCodeButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        val back = UiKit.roundIcon(ctx, "←", 40, "#E8EEFD", UiKit.INK, 18f).apply {
            setOnClickListener { finish() }
        }
        val title = UiKit.text(ctx, "Your pairing code", 24f, UiKit.INK, bold = true)
        UiKit.topMargin(ctx, title, 20)
        val subtitle = UiKit.text(
            ctx, "Ask your parent to type this code on their phone. This screen moves on by itself.", 14f, UiKit.MUTED
        )
        UiKit.topMargin(ctx, subtitle, 8)

        boxes = CodeBoxes(ctx, 6, masked = false)
        status = UiKit.text(ctx, "", 13f, UiKit.MUTED).apply { gravity = Gravity.CENTER }
        UiKit.topMargin(ctx, status, 16)
        val codeCard = UiKit.card(this).apply {
            addView(boxes.view)
            addView(status)
        }
        UiKit.topMargin(this, codeCard, 24)

        val note = UiKit.infoBox(
            ctx, "🔒",
            UiKit.text(ctx, "The code works once and expires after 10 minutes. Only your parent should have it.", 12f, UiKit.INK)
        )
        UiKit.topMargin(this, note, 16)

        newCodeButton = UiKit.button(this, "Get a new code", filled = false) { requestCode() }
        val doneButton = UiKit.button(this, "My parent entered the code  →", filled = true) {
            checkNow()
        }

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(back)
            addView(title)
            addView(subtitle)
            addView(codeCard)
            addView(note)
            addView(newCodeButton)
            addView(doneButton)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(ScrollView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
                addView(column)
            })
        }
        UiKit.padForSystemBars(root)
        setContentView(root)

        requestCode()
    }

    private fun requestCode() {
        if (busy) return
        busy = true
        timer?.cancel()
        pollJob?.cancel()
        boxes.setValue("")
        boxes.setError(false)
        setStatus("Connecting… the server can take up to a minute to wake up.", false)
        newCodeButton.alpha = 0.5f
        scope.launch {
            try {
                val res = ApiClient.apiService.pairGenerate(PairGenerateRequest(store.getOrCreateDeviceId()))
                store.saveTeenToken(res.token)
                HeartbeatScheduler.schedule(applicationContext)
                EventsUploadScheduler.schedule(applicationContext)
                showCode(res.code, res.expiresInSeconds)
                startPolling()
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                showFailure(
                    if (e.code() == 503) "The server is busy. Please try again in a moment."
                    else "Something went wrong (error ${e.code()}). Please try again."
                )
            } catch (e: IOException) {
                showFailure("Couldn't reach the server. Check your internet and try again.")
            } finally {
                busy = false
                newCodeButton.alpha = 1f
            }
        }
    }

    private fun showCode(code: String, seconds: Int) {
        boxes.setValue(code)
        timer?.cancel()
        timer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                val s = (ms / 1000).toInt()
                setStatus("Expires in %02d:%02d  ·  waiting for your parent…".format(s / 60, s % 60), false)
            }

            override fun onFinish() {
                boxes.setError(true)
                setStatus("This code has expired. Tap \"Get a new code\".", true)
            }
        }.start()
    }

    /** Every 3 seconds: has the parent entered the code yet? */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            val token = store.getTeenToken() ?: return@launch
            val deviceId = store.getOrCreateDeviceId()
            while (isActive) {
                delay(3000)
                try {
                    if (ApiClient.apiService.pairState(store.bearer(token), deviceId).paired) {
                        goToSteps()
                        return@launch
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: HttpException) {
                    if (e.code() == 401) {
                        // Our token is no longer valid: start fresh with a new device id and a new code.
                        store.resetTeenState()
                        requestCode()
                        return@launch
                    }
                } catch (e: IOException) {
                    // No internet right now: keep trying.
                }
            }
        }
    }

    /** The manual button: check once, right now. */
    private fun checkNow() {
        val token = store.getTeenToken()
        if (token == null) {
            Toast.makeText(this, "No code yet. Tap \"Get a new code\".", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                val paired = ApiClient.apiService.pairState(store.bearer(token), store.getOrCreateDeviceId()).paired
                if (paired) goToSteps()
                else Toast.makeText(
                    this@TeenCodeActivity, "Not paired yet. Ask your parent to enter the code.", Toast.LENGTH_LONG
                ).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(this@TeenCodeActivity, "Couldn't check right now. Try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun goToSteps() {
        if (moved) return
        moved = true
        timer?.cancel()
        pollJob?.cancel()
        Toast.makeText(this, "Paired with your parent", Toast.LENGTH_SHORT).show()
        startActivity(
            Intent(this, PermissionStepActivity::class.java)
                .putExtra(PermissionStepActivity.EXTRA_STEP, 1)
        )
        finish()
    }

    private fun showFailure(message: String) {
        boxes.setValue("")
        boxes.setError(true)
        setStatus(message, true)
    }

    private fun setStatus(text: String, isError: Boolean) {
        status.text = text
        status.setTextColor(Color.parseColor(if (isError) UiKit.RED else UiKit.MUTED))
    }

    override fun onDestroy() {
        timer?.cancel()
        pollJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}