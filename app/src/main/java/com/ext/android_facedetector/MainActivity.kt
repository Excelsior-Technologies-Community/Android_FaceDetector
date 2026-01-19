package com.ext.android_facedetector

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.ext.android_face_detector.FaceDetectorView
import com.ext.android_face_detector.R

class MainActivity : AppCompatActivity() {

    private lateinit var faceDetectorView: FaceDetectorView
    private lateinit var tvFaceCount: TextView
    private lateinit var selectedImageView: ImageView

    private var isGalleryMode = false

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 101
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    // Gallery picker launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        faceDetectorView = findViewById(R.id.faceDetectorView)
        tvFaceCount = findViewById(R.id.tvFaceCount)
        selectedImageView = findViewById(R.id.selectedImageView)

        // Set up face detection callback
        faceDetectorView.onFaceDetected = { count ->
            runOnUiThread {
                tvFaceCount.text = "Faces Detected: $count"
            }
        }

        // Request permissions if needed
        if (allPermissionsGranted()) {
            startFrontCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    /** Start front camera directly */
    private fun startFrontCamera() {
        isGalleryMode = false
        selectedImageView.visibility = View.GONE
        faceDetectorView.visibility = View.VISIBLE
        faceDetectorView.startCamera(this, CameraSelector.LENS_FACING_FRONT)
    }

    /** Handle gallery image selection */
    private fun handleSelectedImage(uri: Uri) {
        try {
            isGalleryMode = true

            // Stop camera and hide preview
            faceDetectorView.stopCamera()
            faceDetectorView.visibility = View.GONE

            // Load and display image
            val bitmap = loadAndOrientBitmap(uri)
            if (bitmap != null) {
                selectedImageView.setImageBitmap(bitmap)
                selectedImageView.visibility = View.VISIBLE

                // Process image with ML Kit
                processImageWithMLKit(bitmap)
            } else {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                startFrontCamera()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            startFrontCamera()
        }
    }

    private fun processImageWithMLKit(bitmap: Bitmap) {
        tvFaceCount.text = "Processing..."
        Toast.makeText(
            this,
            "Gallery face detection requires additional overlay setup",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun loadAndOrientBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val exifStream = contentResolver.openInputStream(uri)
            val exif = ExifInterface(exifStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            exifStream.close()

            bitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }

            // Scale down if too large
            val maxSize = 1920
            if (bitmap.width > maxSize || bitmap.height > maxSize) {
                val scale = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
                val width = (bitmap.width * scale).toInt()
                val height = (bitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startFrontCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        faceDetectorView.stopCamera()
    }
}
