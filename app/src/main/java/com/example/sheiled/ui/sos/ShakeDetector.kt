import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    private val onConfirmedShake: () -> Unit
) : SensorEventListener {

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.9f
        private const val SHAKE_WINDOW_TIME_MS = 2000  // 2 seconds
        private const val REQUIRED_SHAKE_COUNT = 3
        private const val SHAKE_SLOP_TIME_MS = 400
    }

    private var shakeCount = 0
    private var firstShakeTime: Long = 0
    private var lastShakeTime: Long = 0

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {

        event ?: return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {

            val now = System.currentTimeMillis()

            // Ignore rapid duplicates
            if (lastShakeTime + SHAKE_SLOP_TIME_MS > now) return

            // Start new window if first shake
            if (shakeCount == 0) {
                firstShakeTime = now
            }

            shakeCount++
            lastShakeTime = now

            // If window exceeded → reset
            if (now - firstShakeTime > SHAKE_WINDOW_TIME_MS) {
                shakeCount = 1
                firstShakeTime = now
            }

            // Trigger only if intentional triple shake
            if (shakeCount >= REQUIRED_SHAKE_COUNT) {
                shakeCount = 0
                onConfirmedShake()
            }
        }
    }
}