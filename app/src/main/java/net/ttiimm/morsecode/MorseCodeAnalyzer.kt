package net.ttiimm.morsecode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.camera.core.ImageAnalysis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ttiimm.morsecode.ui.CameraViewModel
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val TAG = "MorseCodeAnalyzer"

class MorseCodeAnalyzer(imageAnalysis: ImageAnalysis, cameraViewModel: CameraViewModel) {

    private val executor: Executor = Executors.newSingleThreadExecutor()

    init {
        imageAnalysis.setAnalyzer(executor) { image ->
            CoroutineScope(Dispatchers.Default).launch {
                image.use {
                    val bitmap = image.toBitmap()
                    val grayscale = convert(bitmap)
                    cameraViewModel.onPreviewChange(grayscale)
                    // thresholding
                    // open cv
                    // contiguous? contour detection algo
//                    val average = average(grayscale)
//                    Log.i(TAG, "image width ${image.width} height ${image.height} avg $average")
                }
            }
        }
    }

    private fun convert(image: Bitmap): Bitmap {
        val copy = image.copy(Bitmap.Config.ARGB_8888, true)
        val copyGrayscale = Bitmap.createBitmap(copy.width, copy.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(copyGrayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val colorMatrixFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorMatrixFilter
        canvas.drawBitmap(copy, 0F, 0F, paint)
        return copyGrayscale
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
