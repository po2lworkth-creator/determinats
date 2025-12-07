package com.example.wastesortmock.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * Простой конвертер YUV_420_888 -> Bitmap для CameraX ImageAnalysis
 */
class YuvToRgbConverter(private val context: Context) {
    private val TAG = "YuvToRgbConverter"

    fun toBitmap(image: ImageProxy): Bitmap {
        val planeY = image.planes[0].buffer
        val planeU = image.planes[1].buffer
        val planeV = image.planes[2].buffer

        val ySize = planeY.remaining()
        val uSize = planeU.remaining()
        val vSize = planeV.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        planeY.get(nv21, 0, ySize)
        planeV.get(nv21, ySize, vSize)
        planeU.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 90, out)
        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}

