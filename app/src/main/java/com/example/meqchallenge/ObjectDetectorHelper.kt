package com.example.meqchallenge

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectorHelper(
    val context: Context,
    val objectDetectorListener: DetectorListener?) {

    private var objectDetector: ObjectDetector? = null

    init {
        setupDetector()
    }

     private fun setupDetector() {
        // Create the base options for the detector using specifies max results and score threshold
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(0.5f)
                .setMaxResults(2)

        optionsBuilder.setBaseOptions(BaseOptions.builder().setNumThreads(2).build())

        val modelName = "ssd-mobilenet-v1.tflite"

        try {
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e("Test", "TFLite failed to load model with error: " + e.message)
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        if (objectDetector == null) {
            setupDetector()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()

        // Preprocess the image and convert it into a TensorImage for detection.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val results = objectDetector?.detect(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        //Convert Detected List to UI object type
        val detectedObjs = results?.map { DetectedObject(
            it.categories.first().label,
            it.boundingBox.left,
            it.boundingBox.right,
            it.boundingBox.top,
            it.boundingBox.bottom,
        ) }

        objectDetectorListener?.onResults(
            detectedObjs,
            inferenceTime,
            tensorImage.height,
            tensorImage.width)
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: List<DetectedObject>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }
}
