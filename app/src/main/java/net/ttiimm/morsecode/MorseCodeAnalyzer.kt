package net.ttiimm.morsecode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ttiimm.morsecode.ui.CameraViewModel
import java.util.concurrent.Executor
import java.util.concurrent.Executors


private const val TAG = "MorseCodeAnalyzer"

private const val THRESHOLD = 250F

private data class SignalState (
    val isLight: Boolean,
    val ts: Long,
    var duration: Long,
)

class MorseCodeAnalyzer(imageAnalysis: ImageAnalysis, cameraViewModel: CameraViewModel) {

    private val executor: Executor = Executors.newSingleThreadExecutor()
    private var state: SignalState = SignalState(false, System.currentTimeMillis(), 0L)

    init {
        imageAnalysis.setAnalyzer(executor) { image ->
            CoroutineScope(Dispatchers.Default).launch {
                image.use {
                    val bitmap = image.toBitmap()
                    val grayscale = convert(bitmap)
                    cameraViewModel.onPreviewChange(grayscale)
                    val sum = sumBitmapValues(grayscale)
                    val now = System.currentTimeMillis()
                    if (!state.isLight && sum > 200_000) {
                        state.duration = now - state.ts

                        if (state.duration > 3900 && state.duration < 4100) {
                            cameraViewModel.onReceiving("  ")
                        } else if (state.duration > 1900 && state.duration < 2100) {
                            cameraViewModel.onReceiving(" ")
                        }

                        Log.i(TAG, state.toString())
                        state = SignalState(true, now, 0)
                    } else if (state.isLight && sum < 1000) {
                        state.duration = now - state.ts

                        if (state.duration > 400 && state.duration < 600) {
                            cameraViewModel.onReceiving(".")
                        } else if (state.duration > 1400 && state.duration < 1600) {
                            cameraViewModel.onReceiving("-")
                        }

                        Log.i(TAG, state.toString())
                        state = SignalState(false, now, 0)
                    }
//                    val average = average(grayscale)

                }
            }
        }
    }

    private fun convert(image: Bitmap): Bitmap {
        val converted = Bitmap.createBitmap(image.height, image.width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(converted)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.rotate(90f, converted.width / 2F, converted.height/ 2F)

        // solution from: https://stackoverflow.com/questions/22731653/android-real-time-black-white-threshold-image
        val grayscaleMatrix = ColorMatrixColorFilter(
            floatArrayOf(
                0.2989F, 0.5870F, 0.1140F, 0F, 0F,
                0.2989F, 0.5870F, 0.1140F, 0F, 0F,
                0.2989F, 0.5870F, 0.1140F, 0F, 0F,
                0F, 0F, 0F, 1F, 0F
            )
        )
        paint.colorFilter = grayscaleMatrix
        val thresholdMatrix = ColorMatrixColorFilter(
            floatArrayOf(
                85F, 85F, 85F, 0F, -255F * THRESHOLD,
                85F, 85F, 85F, 0F, -255F * THRESHOLD,
                85F, 85F, 85F, 0F, -255F * THRESHOLD,
                0F, 0F, 0F, 1F, 0F
            )
        )
        paint.colorFilter = thresholdMatrix
        canvas.drawBitmap(image, 0F, 0F, paint)
        return converted
    }

    private fun convertBinary(image: Bitmap): Bitmap {
        val binaryImage = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until image.width) {
            for (y in 0 until image.height){
                val pixelColor = image.getPixel(x, y)
                val brightness = brightness(pixelColor)
                val onOrOff = if (brightness > THRESHOLD) Color.WHITE else Color.BLACK
                binaryImage.setPixel(x, y, onOrOff)
            }
        }
        val matrix = Matrix()
        matrix.postRotate(90F, image.width / 2F, image.height / 2F)
        return Bitmap.createBitmap(binaryImage, 0, 0, image.width, image.height, matrix, true)
    }

    private fun brightness(color: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return (0.2126 * red + 0.7152 * green + 0.0722 * blue).toInt()
    }

    private fun sumBitmapValues(bitmap: Bitmap): Int {
        var sum = 0
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixelColor = bitmap.getPixel(x, y)
                sum += brightness(pixelColor)
            }
        }
        return sum
    }

    private fun average(image: Bitmap): Double {
        var total = 0L
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val rgb = image.getPixel(x, y)
                val grayness = rgb and 0xFF
                total += grayness
            }
        }
        return total.toDouble() / (image.width * image.height)
    }
}
