package com.ext.android_face_detector

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceDetectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "FaceDetectorView"

    private val previewView = PreviewView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        scaleType = PreviewView.ScaleType.FIT_CENTER
    }

    private var camera: androidx.camera.core.Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null
    private var isFrontCamera = true

    private var imageWidth = 0
    private var imageHeight = 0
    private var rotationDegrees = 0

    private val detectedFaces = mutableListOf<Face>()
    private var lastToastTime = 0L

    var onFaceDetected: ((Int) -> Unit)? = null

    private val faceDetector: FaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .enableTracking()
                .build()
        )
    }

    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#00E5FF")
        isAntiAlias = true
    }

    private val pointPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        isAntiAlias = true
    }

    private val pointGlowPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4000E5FF")
        isAntiAlias = true
    }

    private val overlayView = object : android.view.View(context) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            drawFaceMesh(canvas)
        }
    }

    init {
        addView(previewView)
        addView(overlayView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, lensFacing: Int = CameraSelector.LENS_FACING_FRONT) {
        isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, lensFacing)
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                post {
                    Toast.makeText(context, "Camera failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner, lensFacing: Int) {
        val cameraProvider = cameraProvider ?: return

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // Preview
        val preview = Preview.Builder()
            .build()

        // Image Analysis
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor!!) { imageProxy ->
                    detectFaces(imageProxy)
                }
            }

        try {
            cameraProvider.unbindAll()

            // Bind use cases to lifecycle
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

            // Set surface provider AFTER binding
            preview.setSurfaceProvider(previewView.surfaceProvider)

            Log.d(TAG, "Camera bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun detectFaces(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // Store the original image dimensions and rotation
        imageWidth = imageProxy.width
        imageHeight = imageProxy.height
        rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            rotationDegrees
        )

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                synchronized(detectedFaces) {
                    detectedFaces.clear()
                    detectedFaces.addAll(faces)
                }

                // Show toast when face detected
                val currentTime = System.currentTimeMillis()
                if (faces.isNotEmpty() && currentTime - lastToastTime > 3000) {
                    lastToastTime = currentTime
                    post {
                        Toast.makeText(context, "Face detected successfully!", Toast.LENGTH_SHORT).show()
                    }
                }

                onFaceDetected?.invoke(faces.size)
                post { overlayView.invalidate() }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun drawFaceMesh(canvas: Canvas) {
        synchronized(detectedFaces) {
            if (detectedFaces.isEmpty()) return

            for (face in detectedFaces) {
                drawGeometricStructure(canvas, face)

                val contours = listOf(
                    FaceContour.FACE,
                    FaceContour.LEFT_EYEBROW_TOP,
                    FaceContour.LEFT_EYEBROW_BOTTOM,
                    FaceContour.RIGHT_EYEBROW_TOP,
                    FaceContour.RIGHT_EYEBROW_BOTTOM,
                    FaceContour.LEFT_EYE,
                    FaceContour.RIGHT_EYE,
                    FaceContour.NOSE_BRIDGE,
                    FaceContour.NOSE_BOTTOM,
                    FaceContour.UPPER_LIP_TOP,
                    FaceContour.UPPER_LIP_BOTTOM,
                    FaceContour.LOWER_LIP_TOP,
                    FaceContour.LOWER_LIP_BOTTOM
                )

                contours.forEach { type ->
                    face.getContour(type)?.points?.let { points ->
                        drawContour(canvas, points)
                    }
                }
            }
        }
    }

    private fun drawGeometricStructure(canvas: Canvas, face: Face) {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
        val leftMouth = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val rightMouth = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        val bottomMouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position

        val faceOval = face.getContour(FaceContour.FACE)?.points

        if (leftEye != null && rightEye != null && noseBase != null &&
            leftMouth != null && rightMouth != null && faceOval != null && faceOval.size > 10) {

            val le = translatePoint(leftEye)
            val re = translatePoint(rightEye)
            val nose = translatePoint(noseBase)
            val lm = translatePoint(leftMouth)
            val rm = translatePoint(rightMouth)

            val forehead = translatePoint(faceOval[faceOval.size / 2])
            val chin = if (bottomMouth != null) translatePoint(bottomMouth) else nose
            val leftCheek = translatePoint(faceOval[faceOval.size / 4])
            val rightCheek = translatePoint(faceOval[(3 * faceOval.size) / 4])

            canvas.drawLine(forehead.x, forehead.y, nose.x, nose.y, linePaint)
            canvas.drawLine(nose.x, nose.y, chin.x, chin.y, linePaint)
            canvas.drawLine(leftCheek.x, leftCheek.y, le.x, le.y, linePaint)
            canvas.drawLine(le.x, le.y, re.x, re.y, linePaint)
            canvas.drawLine(re.x, re.y, rightCheek.x, rightCheek.y, linePaint)
            canvas.drawLine(le.x, le.y, nose.x, nose.y, linePaint)
            canvas.drawLine(re.x, re.y, nose.x, nose.y, linePaint)
            canvas.drawLine(nose.x, nose.y, lm.x, lm.y, linePaint)
            canvas.drawLine(nose.x, nose.y, rm.x, rm.y, linePaint)
            canvas.drawLine(lm.x, lm.y, chin.x, chin.y, linePaint)
            canvas.drawLine(rm.x, rm.y, chin.x, chin.y, linePaint)
            canvas.drawLine(forehead.x, forehead.y, leftCheek.x, leftCheek.y, linePaint)
            canvas.drawLine(forehead.x, forehead.y, rightCheek.x, rightCheek.y, linePaint)
            canvas.drawLine(leftCheek.x, leftCheek.y, chin.x, chin.y, linePaint)
            canvas.drawLine(rightCheek.x, rightCheek.y, chin.x, chin.y, linePaint)
        }
    }

    private fun drawContour(canvas: Canvas, points: List<PointF>) {
        if (points.size < 2) return

        val path = Path()
        val first = translatePoint(points[0])
        path.moveTo(first.x, first.y)

        for (i in 1 until points.size) {
            val p = translatePoint(points[i])
            path.lineTo(p.x, p.y)
        }

        canvas.drawPath(path, linePaint)

        points.forEach { point ->
            val p = translatePoint(point)
            canvas.drawCircle(p.x, p.y, 9f, pointGlowPaint)
            canvas.drawCircle(p.x, p.y, 4f, pointPaint)
        }
    }

    private fun translatePoint(point: PointF): PointF {
        val viewWidth = overlayView.width.toFloat()
        val viewHeight = overlayView.height.toFloat()

        if (imageWidth == 0 || imageHeight == 0 || viewWidth == 0f || viewHeight == 0f) {
            return PointF(0f, 0f)
        }

        // Determine actual image dimensions based on rotation
        val actualImageWidth: Float
        val actualImageHeight: Float

        when (rotationDegrees) {
            90, 270 -> {
                // Portrait orientation - swap dimensions
                actualImageWidth = imageHeight.toFloat()
                actualImageHeight = imageWidth.toFloat()
            }
            else -> {
                // Landscape orientation
                actualImageWidth = imageWidth.toFloat()
                actualImageHeight = imageHeight.toFloat()
            }
        }

        // Calculate scale to fit (matching PreviewView.ScaleType.FIT_CENTER)
        val widthRatio = viewWidth / actualImageWidth
        val heightRatio = viewHeight / actualImageHeight
        val scaleFactor = widthRatio.coerceAtMost(heightRatio) // Use minimum to fit inside

        // Calculate the actual displayed image size
        val scaledWidth = actualImageWidth * scaleFactor
        val scaledHeight = actualImageHeight * scaleFactor

        // Calculate offsets to center the image
        val offsetX = (viewWidth - scaledWidth) / 2f
        val offsetY = (viewHeight - scaledHeight) / 2f

        // Transform the point coordinates
        var x = point.x * scaleFactor + offsetX
        val y = point.y * scaleFactor + offsetY

        // Mirror horizontally for front camera
        if (isFrontCamera) {
            x = viewWidth - x
        }

        return PointF(x, y)
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor?.shutdown()
            cameraExecutor = null
            faceDetector.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
        }
    }
}