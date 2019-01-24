package n.elshaarawyx.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import android.os.Bundle
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.google.android.things.contrib.driver.ssd1306.Ssd1306
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.Pwm
import com.google.zxing.common.BitMatrix
import com.google.zxing.MultiFormatWriter
import com.google.zxing.EncodeHintType
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.integration.android.IntentIntegrator
import java.io.IOException


/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : Activity() {
    private val handler = Handler()
    private lateinit var pwm: Pwm
    private var isPulseIncreasing = true
    private var activePulseDuration: Double = 0.0

    private val changePWMRunnable = object : Runnable {
        override fun run() {
            // Change the duration of the active PWM pulse, but keep it between the minimum and
            // maximum limits.
            // The direction of the change depends on the isPulseIncreasing variable, so the pulse
            // will bounce from MIN to MAX.
            if (isPulseIncreasing) {
                activePulseDuration += PULSE_CHANGE_PER_STEP_MS
            } else {
                activePulseDuration -= PULSE_CHANGE_PER_STEP_MS
            }

            // Bounce activePulseDuration back from the limits
            if (isPulseIncreasing) {
                activePulseDuration = MAX_ACTIVE_PULSE_DURATION_MS
                isPulseIncreasing = !isPulseIncreasing
            } else {
                activePulseDuration = MIN_ACTIVE_PULSE_DURATION_MS
                isPulseIncreasing = !isPulseIncreasing
            }

            Log.d(TAG, "Changing PWM active pulse duration to ${activePulseDuration} ms")

            try {

                // Duty cycle is the percentage of active (on) pulse over the total duration of the
                // PWM pulse
                pwm.setPwmDutyCycle(100 * activePulseDuration / PULSE_PERIOD_MS)

                // Reschedule the same runnable in {@link #INTERVAL_BETWEEN_STEPS_MS} milliseconds
                if (isPulseIncreasing)
                    handler.postDelayed(this, INTERVAL_BETWEEN_STEPS_MS)
                else
                {
                    pwm.close()
                    button.isEnabled = true
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error on PeripheralIO API", e)
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "Starting PwmActivity")

        activePulseDuration = MIN_ACTIVE_PULSE_DURATION_MS

        pwm = PeripheralManager.getInstance().openPwm(BoardDefaults.pwmPort)

        // Always set frequency and initial duty cycle before enabling PWM
        pwm.setPwmFrequencyHz(1000 / PULSE_PERIOD_MS)
        pwm.setPwmDutyCycle(0.0)
        pwm.setEnabled(true)

        // Post a Runnable that continuously change PWM pulse width, effectively changing the
        // servo position
        Log.d(TAG, "Start changing PWM pulse")
        handler.post(changePWMRunnable)


        button.setOnClickListener {
            it.isEnabled = false
            activePulseDuration = MIN_ACTIVE_PULSE_DURATION_MS

            pwm = PeripheralManager.getInstance().openPwm(BoardDefaults.pwmPort)

            // Always set frequency and initial duty cycle before enabling PWM
            pwm.setPwmFrequencyHz(1000 / PULSE_PERIOD_MS)
//            pwm.setPwmDutyCycle(activePulseDuration)
            pwm.setEnabled(true)

            // Post a Runnable that continuously change PWM pulse width, effectively changing the
            // servo position
            Log.d(TAG, "Start changing PWM pulse")
            handler.post(changePWMRunnable)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        // Remove pending Runnable from the handler.
        handler.removeCallbacks(changePWMRunnable)
        // Close the PWM port.
        Log.i(TAG, "Closing port")
        pwm.close()
    }

    companion object {
        private val TAG = "MY_TAG"

        // Parameters of the servo PWM
        const private val MIN_ACTIVE_PULSE_DURATION_MS = 1.0
        const private val MAX_ACTIVE_PULSE_DURATION_MS = 2.0
        const private val PULSE_PERIOD_MS = 20.0  // Frequency of 50Hz (1000/20)

        // Parameters for the servo movement over time
        const private val PULSE_CHANGE_PER_STEP_MS = 0.2
        const private val INTERVAL_BETWEEN_STEPS_MS: Long = 500
    }
}