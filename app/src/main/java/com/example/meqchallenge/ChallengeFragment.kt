package com.example.meqchallenge

import android.Manifest
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.meqchallenge.databinding.FragmentChallengeBinding
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ChallengeFragment : Fragment(),  ObjectDetectorHelper. DetectorListener {

    //Use ViewBinding to reference layout
    private var binding: FragmentChallengeBinding? = null

    //Use Viewmodel
    private val viewModel by viewModels<ChallengeFragmentViewModel>()

    //The label used for the reference object
    private val referenceLabel = "cell phone"

    //Camera and Analysing Vars
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Configure Camera and detectors
        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding!!.viewFinder.post {
           initCamera()
        }

        //Configure Compose UI Components
        binding!!.composeView.setContent {
            MyApplicationTheme {
                ConfigureComposeUIComponents()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Request Camera Permissions
        val requestCodePermissions = 10
        val requiredPermissions = arrayOf(Manifest.permission.CAMERA)

        ActivityCompat.requestPermissions(
            requireActivity(), requiredPermissions, requestCodePermissions
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentChallengeBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //Camera threads and ViewBinding
        binding = null
        cameraExecutor.shutdown()
    }

    private fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun bindCameraUseCases() {
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        //Only support back camera for now
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview =
            Preview.Builder()
                .setTargetRotation(binding!!.viewFinder.display.rotation)
                .build()

        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetRotation(binding!!.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }
                        detectObjects(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview?.surfaceProvider = binding!!.viewFinder.surfaceProvider
        } catch (exc: Exception) {
            Log.e("TAG", "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = binding!!.viewFinder.display.rotation
    }

    // Update UI after objects have been detected.
    override fun onResults(
        results: List<DetectedObject>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
            //Extract detected objects (if any)
            val reference = results?.find { it.label == referenceLabel }
            val detected = results?.find { it.label != referenceLabel }

            viewModel.referenceDetected.postValue(reference != null)
            viewModel.objectDetected.postValue(detected != null)

            calculateAndSetDimensions(reference, detected)

            //Draw bounding boxes
            binding!!.overlay.setResults(
                results,
                imageHeight,
                imageWidth
            )

            // Force a redraw
            binding!!.overlay.invalidate()
        }
    }

    private fun calculateAndSetDimensions(reference: DetectedObject?, detected: DetectedObject?) {
        //From a distance of approx 30cm away, a phone of dimensions 7x14cms
        //Is approx 150dp width and 270dp for height

        val dpScaleWidth = 150
        val dpScaleHeight = 270
        val cmScaleWidth = 7
        val cmScaleHeight = 14

        //When calculating, the size should be within 30dp of above values
        val estimateBound = 30

        if(reference != null) {
            //Check if the detected reference object dimensions approximate this
            val referenceWidth = reference.right - reference.left
            val referenceHeight = reference.bottom - reference.top

            val widthWithinEstimates = referenceWidth > (dpScaleWidth - estimateBound) && referenceWidth < (dpScaleWidth + estimateBound)
            val heightWithinEstimates = referenceHeight > (dpScaleHeight - estimateBound) && referenceHeight < (dpScaleHeight + estimateBound)
            val referenceWithinEstimates = widthWithinEstimates && heightWithinEstimates

            viewModel.referenceSizeCorrect.postValue(referenceWithinEstimates)

            if(referenceWithinEstimates) {
                //Our reference is approx the correct size so detect size of other obj

                if(detected != null) {
                    val widthCm = ((detected.right - detected.left) / referenceWidth) * cmScaleWidth
                    val heightCm = ((detected.bottom - detected.top) / referenceHeight) * cmScaleHeight

                    val widScale = ((detected.right - detected.left) / referenceWidth)
                    val heiScale = ((detected.bottom - detected.top) / referenceHeight)

                    viewModel.objectLabel.postValue(detected.label)

                    viewModel.objectEstimatedWidthCm.postValue(widthCm)
                    viewModel.objectEstimatedHeightCm.postValue(heightCm)

                    viewModel.objectEstimatedWidthScale.postValue(widScale)
                    viewModel.objectEstimatedHeightScale.postValue(heiScale)
                }
            }
        }
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    private fun ConfigureComposeUIComponents() {
        val referenceDetected = viewModel.referenceDetected.observeAsState()
        val objectDetected = viewModel.objectDetected.observeAsState()
        val referenceSizeCorrect = viewModel.referenceSizeCorrect.observeAsState()
        val objectLabel = viewModel.objectLabel.observeAsState()
        val objectWidthCm = viewModel.objectEstimatedWidthCm.observeAsState()
        val objectHeightCm = viewModel.objectEstimatedHeightCm.observeAsState()
        val objectWidthScale = viewModel.objectEstimatedWidthScale.observeAsState()
        val objectHeightScale = viewModel.objectEstimatedHeightScale.observeAsState()

        Column {
            //Show text if reference is detected, correct size or not detected
            Text(text =
                if(referenceDetected.value == false) {
                    getString(R.string.referenceNotDetected)
                }
                else {
                    if(referenceSizeCorrect.value == true) {
                        getString(R.string.referenceDetected)
                    }
                    else {
                        getString(R.string.referenceIncorrect)
                    }
                 }
            )

            //Show text if additional object was detected
            if(referenceSizeCorrect.value == true
                && referenceDetected.value == true) {

                //If object detected, show appropriate ui
                if(objectDetected.value == true) {
                    Text(text = getString(R.string.detectedLabelName, objectLabel.value))
                    Text(text = getString(R.string.detectedSizeCm, objectWidthCm.value, objectHeightCm.value))
                    Text(text = getString(R.string.detectedSizeScale, objectWidthScale.value, objectHeightScale.value))
                }
                else {
                    Text(text = getString(R.string.detectedMissing))
                }
            }
        }
    }
}