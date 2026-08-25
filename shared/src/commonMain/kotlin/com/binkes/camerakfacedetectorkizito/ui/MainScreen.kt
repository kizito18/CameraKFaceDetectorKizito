package com.binkes.camerakfacedetectorkizito.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.binkes.camerakfacedetectorkizito.data.local.AppPermissionsControlImpl
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun MainScreen(
    onGoToFaceInfo: () -> Unit,
    onGoToTakePicture: () -> Unit,
    onGoToLivenessVerification: () -> Job
) {
    val scope = rememberCoroutineScope()

    val permissionFactory = rememberPermissionsControllerFactory()
    val permissionController = remember(permissionFactory) {
        permissionFactory.createPermissionsController()
    }
    val appPermissionsControls = remember { AppPermissionsControlImpl() }

    BindEffect(permissionController) // required for moko-permissions

    var hasCameraPermission by remember { mutableStateOf(false) }

    fun requestCameraPermissionThen(onGranted: () -> Unit) {
        scope.launch(Dispatchers.IO) {
            if (permissionController.isPermissionGranted(Permission.CAMERA)) {
                hasCameraPermission = true
                withContext(Dispatchers.Main) { onGranted() }
                return@launch
            }

            appPermissionsControls.checkCameraPermissions(
                controller = permissionController,
                onRequestDenied = {
                    scope.launch(Dispatchers.Main) {
                        hasCameraPermission = false
                    }
                },
                onRequestDeniedForever = {
                    scope.launch(Dispatchers.Main) {
                        hasCameraPermission = false
                    }
                    scope.launch(Dispatchers.IO) {
                        permissionController.openAppSettings()
                    }
                },
                onRequestGranted = {
                    scope.launch(Dispatchers.Main) {
                        hasCameraPermission = true
                        onGranted()
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        Button(
            onClick = {
                requestCameraPermissionThen(
                   onGranted = {
                       onGoToTakePicture()
                   }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Take Picture")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                requestCameraPermissionThen(
                    onGranted = {
                        onGoToLivenessVerification()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Face Recognition")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                requestCameraPermissionThen(onGranted = {
                    onGoToFaceInfo()
                }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Face Info")
        }

    }
}
