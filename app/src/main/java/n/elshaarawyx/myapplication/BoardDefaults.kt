package n.elshaarawyx.myapplication

import android.os.Build

/**
 * Created by elshaarawy on 1/23/19.
 */
object BoardDefaults {
    private const val DEVICE_RPI3 = "rpi3"
    private const val DEVICE_IMX6UL_PICO = "imx6ul_pico"
    private const val DEVICE_IMX7D_PICO = "imx7d_pico"

    /**
     * Return the preferred PWM port for each board.
     */
    val pwmPort = when (Build.DEVICE) {
        DEVICE_RPI3 -> "PWM0"
        DEVICE_IMX6UL_PICO -> "PWM7"
        DEVICE_IMX7D_PICO -> "PWM1"
        else -> throw IllegalStateException("Unknown Build.DEVICE ${Build.DEVICE}")
    }
}