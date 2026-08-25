package com.binkes.camerakfacedetectorkizito.data.local

import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.camera.CAMERA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext




class AppPermissionsControlImpl : AppPermissionsControlsRepository {

    override suspend fun checkCameraPermissions(
        controller: PermissionsController,
        onRequestDenied: () -> Unit,
        onRequestDeniedForever: () -> Unit,
        onRequestGranted: () -> Unit
    ) {
        withContext(Dispatchers.IO) {

            val permission = Permission.CAMERA
            val isGranted = controller.isPermissionGranted(permission)

            if (!isGranted) {
                try {
                    controller.providePermission(permission)
                    withContext(Dispatchers.Main) { onRequestGranted() }
                } catch (e: DeniedException) {
                    withContext(Dispatchers.Main) { onRequestDenied() }
                } catch (e: DeniedAlwaysException) {
                    withContext(Dispatchers.Main) { onRequestDeniedForever() }
                } catch (e: RequestCanceledException) {
                    withContext(Dispatchers.Main) { onRequestDenied() }
                }
            } else {
                withContext(Dispatchers.Main) {
                    onRequestGranted()
                }
            }
        }
    }



}



