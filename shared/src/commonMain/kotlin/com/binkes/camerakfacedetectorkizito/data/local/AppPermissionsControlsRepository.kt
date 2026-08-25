package com.binkes.camerakfacedetectorkizito.data.local

import dev.icerock.moko.permissions.PermissionsController


interface AppPermissionsControlsRepository {

    suspend fun checkCameraPermissions(
        controller: PermissionsController,
        onRequestDenied: () -> Unit,
        onRequestDeniedForever: () -> Unit,
        onRequestGranted: () -> Unit
    )

}