package com.binkes.camerakfacedetectorkizito.ui


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.binkes.camerakfacedetectorkizito.data.PhotoData
import com.binkes.camerakfacedetectorkizito.data.local.AppPermissionsControlImpl
import com.binkes.camerakfacedetectorkizito.face.DetectorKind
import com.binkes.camerakfacedetectorkizito.face.FaceDetectorFactory
import com.binkes.camerakfacedetectorkizito.face.attachFaceAnalyzer
import com.binkes.camerakfacedetectorkizito.face.createImageCompressor
import com.binkes.camerakfacedetectorkizito.kizitoFaceRecognition.FaceGuidanceState
import com.binkes.camerakfacedetectorkizito.kizitoFaceRecognition.FaceVerificationGuidanceEngine
import com.binkes.camerakfacedetectorkizito.models.AppTypography
import com.binkes.camerakfacedetectorkizito.models.ObjectsMyColors
import com.binkes.camerakfacedetectorkizito.models.ObjectsTextColors
import com.binkes.camerakfacedetectorkizito.utill.ConstantObject
import com.kashif.cameraK.compose.CameraKScreen
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.state.CameraConfiguration
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun LivenessVerificationScreen(
    onVerificationComplete: (ByteArray) -> Unit,
    onCancel: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    val compressor = remember { createImageCompressor() }
    var compressImage by remember { mutableStateOf(false) }

    val permissionFactor = rememberPermissionsControllerFactory()
    val permissionController = remember(permissionFactor) {
        permissionFactor.createPermissionsController()
    }
    val appPermissionsControls = remember { AppPermissionsControlImpl() }
    BindEffect(permissionController)

    val engine = remember { FaceVerificationGuidanceEngine() }
    var guidanceState by remember { mutableStateOf<FaceGuidanceState>(FaceGuidanceState.NoFace) }
    var capturedPhoto by remember { mutableStateOf<PhotoData?>(null) }
    var isVerifyClicked by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    var readyController by remember { mutableStateOf<CameraController?>(null) }

    val isVerified = guidanceState is FaceGuidanceState.Verified
    val isCapturing = guidanceState is FaceGuidanceState.Capturing

    val borderColor by animateColorAsState(
        targetValue = if (capturedPhoto == null) {
            when {
                isCapturing -> Color.Black
                isVerified -> Color(0xFF4CAF50)
                else -> Color.Red
            }
        } else Color(0xFF4CAF50),
        animationSpec = tween(300),
        label = "borderColor",
    )
    val statusColor by animateColorAsState(
        targetValue = if (capturedPhoto == null) {
            when {
                isCapturing -> Color.Black
                isVerified -> Color(0xFF4CAF50)
                else -> Color.Red
            }
        } else Color(0xFF4CAF50),
        animationSpec = tween(300),
        label = "statusColor",
    )
    val cameraAlpha by animateFloatAsState(
        targetValue = if (capturedPhoto == null) 1f else 0f,
        animationSpec = tween(300), label = "cameraAlpha",
    )
    val photoAlpha by animateFloatAsState(
        targetValue = if (capturedPhoto != null) 1f else 0f,
        animationSpec = tween(300), label = "photoAlpha",
    )

    fun guidanceMessage(state: FaceGuidanceState): String {
        if (capturedPhoto != null) return "Verified!"
        return when (state) {
            is FaceGuidanceState.NoFace -> "No face detected"
            is FaceGuidanceState.MoveCloser -> "Move a little closer"
            is FaceGuidanceState.MoveBack -> "Move back a little"
            is FaceGuidanceState.CenterFace -> "Center your face in the frame"
            is FaceGuidanceState.LookStraight -> "Look straight at the camera"
            is FaceGuidanceState.HoldStill -> "Hold still..."
            is FaceGuidanceState.BlinkNow -> "Blink to verify"
            is FaceGuidanceState.Verified -> "Verified!"
            is FaceGuidanceState.Capturing -> "Capturing..."
        }
    }

    fun requestAndCheckCameraPermission(onGranted: () -> Unit) {
        scope.launch(Dispatchers.IO) {
            if (!permissionController.isPermissionGranted(Permission.CAMERA)) {
                appPermissionsControls.checkCameraPermissions(
                    controller = permissionController,
                    onRequestDenied = {
                        scope.launch(Dispatchers.Main) { hasCameraPermission = false }
                    },
                    onRequestDeniedForever = {
                        scope.launch(Dispatchers.Main) { hasCameraPermission = false }
                        scope.launch(Dispatchers.IO) { permissionController.openAppSettings() }
                    },
                    onRequestGranted = {
                        scope.launch(Dispatchers.Main) { hasCameraPermission = true }
                        onGranted()
                    },
                )
            } else {
                hasCameraPermission = true
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(1.seconds)
        requestAndCheckCameraPermission { }
    }

    LaunchedEffect(compressImage) {
        if (compressImage) {
            compressor.start(
                imageData = capturedPhoto!!,
                onProgress = { },
                onComplete = { compressed ->
                    scope.launch {
                        isVerifyClicked = false
                        onVerificationComplete(compressed.bytes)
                    }
                },
                onError = {
                    scope.launch {
                        isVerifyClicked = false
                        capturedPhoto?.let { onVerificationComplete(it.bytes) }
                    }
                },
            )
        }
    }

    val cameraState by rememberCameraKState(config = CameraConfiguration(cameraLens = CameraLens.FRONT))

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = ObjectsMyColors.PRIMARY_COLOR)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Identity Verification",
                    style = AppTypography.heading1.copy(
                        color = ObjectsTextColors.Typography_Header_Title_Color,
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Get close to the camera and position your face in the frame",
                    style = AppTypography.bodyMain.copy(
                        color = ObjectsTextColors.Typography_Sec_Hint_Color,
                        fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 19.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (capturedPhoto == null) {

                        /*
                        CameraKScreen(
                            modifier = Modifier.fillMaxSize().alpha(cameraAlpha),
                            cameraState = cameraState,
                        ) { ready ->
                            val handle = remember(ready.controller) {
                                attachFaceAnalyzer(ready.controller) { result ->
                                    guidanceState = engine.onFrame(result)
                                }
                            }
                            DisposableEffect(handle) { onDispose { handle.detach() } }

                            DisposableEffect(handle) {
                                val detector = FaceDetectorFactory.create(DetectorKind.ML_KIT)
                                handle.setDetector(detector)
                                onDispose {
                                    handle.setDetector(null)
                                    detector.close()
                                }
                            }
                        }

                         */


                        CameraKScreen(
                            modifier = Modifier.fillMaxSize().alpha(cameraAlpha),
                            cameraState = cameraState,
                        ) { ready ->
                            LaunchedEffect(ready.controller) {
                                readyController = ready.controller
                            }

                            val handle = remember(ready.controller) {
                                attachFaceAnalyzer(ready.controller) { result ->
                                    guidanceState = engine.onFrame(result)
                                }
                            }
                            DisposableEffect(handle) { onDispose { handle.detach() } }

                            DisposableEffect(handle) {
                                val detector = FaceDetectorFactory.create(DetectorKind.ML_KIT)
                                handle.setDetector(detector)
                                onDispose {
                                    handle.setDetector(null)
                                    detector.close()
                                }
                            }
                        }
                    }

                    AsyncImage(
                        model = capturedPhoto?.bytes,
                        contentDescription = "picture",
                        modifier = Modifier.fillMaxSize().alpha(photoAlpha),
                        contentScale = ContentScale.Crop,
                    )

                    // ── Face oval overlay ──────────────
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val ovalWidth = size.width * 0.65f
                        val ovalHeight = size.height * 0.75f
                        val left = (size.width - ovalWidth) / 2f
                        val top = (size.height - ovalHeight) / 2f
                        val path = Path().apply {
                            addRect(Rect(0f, 0f, size.width, size.height))
                            addOval(Rect(left, top, left + ovalWidth, top + ovalHeight))
                        }
                        drawPath(path, color = Color.Black.copy(alpha = 0.5f), style = Fill, blendMode = BlendMode.SrcOver)
                        drawOval(
                            color = borderColor,
                            topLeft = Offset(left, top),
                            size = Size(ovalWidth, ovalHeight),
                            style = Stroke(width = 4.dp.toPx()),
                        )
                    }

                    // ── Status indicator ──────────
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .background(color = statusColor, shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Canvas(modifier = Modifier.size(8.dp)) { drawCircle(color = Color.White) }
                            Text(
                                text = guidanceMessage(guidanceState),
                                style = AppTypography.caption.copy(
                                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Take / Retake photo button ─────────
                Button(
                    onClick = {
                        if (capturedPhoto == null) {
                            if (isVerified) {
                                val controller = readyController ?: return@Button
                                engine.setCapturing(true)
                                guidanceState = FaceGuidanceState.Capturing
                                scope.launch {

                                    /*
                                    // TODO: match this to your real ImageCaptureResult variants.
                                    when (val result = controller.takePictureToFile()) {
                                        is ImageCaptureResult.Success -> {
                                            capturedPhoto = PhotoData(
                                                bytes = result., // adjust field name if different
                                                fileName = "liveness_${kotlin.time.Clock.System.now().toEpochMilliseconds()}.jpg",
                                            )
                                            engine.setCapturing(false)
                                            controller.stopSession()
                                        }
                                        else -> {
                                            engine.setCapturing(false)
                                        }
                                    }

                                     */

                                    /*
                                    scope.launch {
                                        when (val result = controller.takePictureToFile()) {
                                            is ImageCaptureResult.SuccessWithFile -> {
                                                val bytes = readFileBytes(result.filePath) // you need this helper — see below
                                                capturedPhoto = PhotoData(
                                                    bytes = bytes,
                                                    fileName = "liveness_${kotlin.time.Clock.System.now().toEpochMilliseconds()}.jpg",
                                                )
                                                engine.setCapturing(false)
                                                controller.stopSession()
                                            }
                                            is ImageCaptureResult.Error -> {
                                                engine.setCapturing(false)
                                            }
                                            else -> {
                                                engine.setCapturing(false)
                                            }
                                        }
                                    }

                                     */

                                    @Suppress("DEPRECATION")
                                    scope.launch {
                                        when (val result = controller.takePicture()) {
                                            is ImageCaptureResult.Success -> {
                                                capturedPhoto = PhotoData(
                                                    bytes = result.byteArray,
                                                    fileName = "liveness_${kotlin.time.Clock.System.now().toEpochMilliseconds()}.jpg",
                                                )
                                                engine.setCapturing(false)
                                                controller.stopSession()
                                            }
                                            is ImageCaptureResult.Error -> {
                                                engine.setCapturing(false)
                                            }
                                            else -> {
                                                engine.setCapturing(false)
                                            }
                                        }
                                    }

                                }
                            }
                        } else {
                            capturedPhoto = null
                            engine.reset()
                            guidanceState = FaceGuidanceState.NoFace
                            readyController?.startSession()
                        }
                    },
                    enabled = (isVerified && hasCameraPermission) || (hasCameraPermission && capturedPhoto != null),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!hasCameraPermission || (!isVerified && capturedPhoto == null)) {
                            ObjectsTextColors.Typography_Disabled_Color.copy(alpha = 0.1f)
                        } else {
                            if (capturedPhoto != null) ObjectsMyColors.ORANGE_COLOR
                            else ObjectsMyColors.Button_Blue_Color.copy(alpha = 0.10f)
                        },
                        disabledContainerColor = ObjectsTextColors.Typography_Disabled_Color.copy(alpha = 0.1f),
                    ),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = if (capturedPhoto != null) "Retake photo" else "Take photo",
                        style = AppTypography.label.copy(
                            color = if (!hasCameraPermission || (!isVerified && capturedPhoto == null)) {
                                ObjectsTextColors.Typography_Disabled_Color
                            } else if (capturedPhoto != null) Color.White else ObjectsMyColors.Button_Blue_Color,
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }

                if (capturedPhoto != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Canvas(modifier = Modifier.size(8.dp)) { drawCircle(color = Color(0xFF4CAF50)) }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Photo captured successfully",
                            style = AppTypography.caption.copy(color = Color(0xFF4CAF50), fontSize = 12.sp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (capturedPhoto == null) {
                            requestAndCheckCameraPermission { }
                        } else {
                            isVerifyClicked = true
                            if (capturedPhoto!!.bytes.size > ConstantObject.MAX_IMAGE_SIZE_BYTES) {
                                compressImage = true
                            } else {
                                isVerifyClicked = false
                                onVerificationComplete(capturedPhoto!!.bytes)
                            }
                        }
                    },
                    enabled = capturedPhoto != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObjectsMyColors.Button_Blue_Color,
                        disabledContainerColor = ObjectsTextColors.Typography_Disabled_Color,
                    ),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        "Verify",
                        style = AppTypography.label.copy(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        readyController?.stopSession()
                        onCancel()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                ) { Text("Cancel") }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }

        if (isVerifyClicked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObjectsMyColors.Grey_Color.copy(alpha = 0.7f))
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(50.dp), trackColor = ObjectsMyColors.Button_Blue_Color)
            }
        }
    }
}

