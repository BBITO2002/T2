package com.penia.appt2.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object ImageUtils {

    /**
     * Convierte una imagen desde URI a string Base64
     * @param context Contexto de la aplicación
     * @param imageUri URI de la imagen seleccionada
     * @param maxWidth Ancho máximo de la imagen (para compresión)
     * @param maxHeight Alto máximo de la imagen (para compresión)
     * @return String Base64 de la imagen o null si hay error
     */
    fun uriToBase64(
        context: Context,
        imageUri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 600
    ): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            // Redimensionar la imagen para reducir el tamaño
            val resizedBitmap = resizeBitmap(bitmap, maxWidth, maxHeight)

            // Corregir orientación si es necesario
            val correctedBitmap = correctImageOrientation(context, imageUri, resizedBitmap)

            bitmapToBase64(correctedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convierte un Bitmap a string Base64
     * @param bitmap Bitmap a convertir
     * @param quality Calidad de compresión JPEG (0-100)
     * @return String Base64 del bitmap
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Convierte un string Base64 a Bitmap
     * @param base64String String Base64 de la imagen
     * @return Bitmap de la imagen o null si hay error
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Redimensiona un bitmap manteniendo la proporción
     * @param bitmap Bitmap original
     * @param maxWidth Ancho máximo
     * @param maxHeight Alto máximo
     * @return Bitmap redimensionado
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    /**
     * Corrige la orientación de la imagen basada en los datos EXIF
     * @param context Contexto de la aplicación
     * @param imageUri URI de la imagen
     * @param bitmap Bitmap a corregir
     * @return Bitmap con orientación corregida
     */
    private fun correctImageOrientation(context: Context, imageUri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            inputStream.close()

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Rota un bitmap por los grados especificados
     * @param bitmap Bitmap a rotar
     * @param degrees Grados de rotación
     * @return Bitmap rotado
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Valida si un string es una imagen Base64 válida
     * @param base64String String a validar
     * @return true si es válido, false en caso contrario
     */
    fun isValidBase64Image(base64String: String): Boolean {
        return try {
            if (base64String.isEmpty()) return false
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap != null
        } catch (e: Exception) {
            false
        }
    }
}