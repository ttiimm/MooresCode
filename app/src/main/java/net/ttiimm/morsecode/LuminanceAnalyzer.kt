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
import net.ttiimm.morsecode.ui.Frame
import net.ttiimm.morsecode.ui.FrameMetrics
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.pow

private const val DO_PBP = false
private const val THRESHOLD = 250F

private const val TAG = "LuminanceAnalyzer"

class LuminanceAnalyzer(imageAnalysis: ImageAnalysis, cameraViewModel: CameraViewModel) {

    private val executor: Executor = Executors.newSingleThreadExecutor()

    init {
        imageAnalysis.setAnalyzer(executor) { image ->
            CoroutineScope(Dispatchers.Default).launch {
                image.use {
                    val bitmap = image.toBitmap()
                    // convert to grayscale
                    val convertedImage = if (DO_PBP) convertNaive(bitmap) else convert(bitmap)
                    val pixels = getPixels(convertedImage)
                    // XXX: filter based on location or size?
                    val luminance = if (DO_PBP) sumBitmapValuesNaive(bitmap) else sumBitmapValues(pixels)
                    val sumOfValues = luminance
                    val mean = sumOfValues.toFloat() / (bitmap.width * bitmap.height)
                    val stddev = calcStdDev(pixels, mean)
                    val threshold = mean + (1 * stddev)
                    val totalAbnormallyBright = pixels.filter { it > threshold  }.size
                    Log.d(TAG, "L = $sumOfValues mean = $mean stddev= $stddev abnormal = $totalAbnormallyBright")
                    val frame = Frame(convertedImage, FrameMetrics(luminance))
                    cameraViewModel.onPreviewChange(frame)
                }
            }
        }
    }

    private fun calcStdDev(pixels: IntArray, mean: Float): Double {
        val sumOfSquaredDifferences = pixels.sumOf { ((it - mean).pow(2.0F)).toDouble() }
        return (sumOfSquaredDifferences / pixels.size).pow(.5)
    }

    private fun convert(image: Bitmap): Bitmap {
        val converted = Bitmap.createBitmap(image.height, image.width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(converted)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // px/py is the pivot point
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
        canvas.drawBitmap(image, 0F, 0F, paint)

//        val thresholdMatrix = ColorMatrixColorFilter(
//            floatArrayOf(
//                85F, 85F, 85F, 0F, -255F * THRESHOLD,
//                85F, 85F, 85F, 0F, -255F * THRESHOLD,
//                85F, 85F, 85F, 0F, -255F * THRESHOLD,
//                0F, 0F, 0F, 1F, 0F
//            )
//        )
//        paint.colorFilter = thresholdMatrix
//        canvas.drawBitmap(image, 0F, 0F, paint)
        return converted
    }

    private fun convertNaive(image: Bitmap): Bitmap {
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

    private fun sumBitmapValues(pixels: IntArray): Int {
        return pixels.sumOf {
            it
        }
    }

    private fun getPixels(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.map { it and 0xFF }.toIntArray()
    }

    private fun sumBitmapValuesNaive(bitmap: Bitmap): Int {
        var sum = 0
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val rgb = bitmap.getPixel(x, y)
                val grayness = rgb and 0xFF
                sum += grayness
            }
        }
        return sum
    }
}
