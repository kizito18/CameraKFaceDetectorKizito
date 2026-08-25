package com.binkes.camerakfacedetectorkizito.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.binkes.camerakfacedetectorkizito.ui.FaceScannerInfoScreen
import com.binkes.camerakfacedetectorkizito.ui.LivenessVerificationScreen
import com.binkes.camerakfacedetectorkizito.ui.MainScreen

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


fun NavController.popBackStackSafely() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootNavGraph(
    navController: NavHostController,
) {


    val navMutex = remember { Mutex() }

    val scope = rememberCoroutineScope()


    NavHost(
        navController = navController,
        startDestination = NavScreens.Main.route
    ) {


        composable(route = NavScreens.Main.route) {

            MainScreen(
                onGoToTakePicture = {
                    scope.launch {
                        navMutex.withLock {
                            try {
                                navController.navigate(NavScreens.TakePicture.route)
                            } catch (_: Exception) {
                                navMutex.unlock()
                            }
                        }
                    }
                },
                onGoToLivenessVerification = {
                    scope.launch {
                        navMutex.withLock {
                            try {
                                navController.navigate(NavScreens.LivenessVerification.route)
                            } catch (_: Exception) {
                                navMutex.unlock()
                            }
                        }
                    }
                },
                onGoToFaceInfo = {
                    scope.launch {
                        navMutex.withLock {
                            try {
                                navController.navigate(NavScreens.FaceScannerInfo.route)
                            } catch (_: Exception) {
                                navMutex.unlock()
                            }
                        }
                    }

                }
            )

        }
        

        composable(route = NavScreens.TakePicture.route) {

           // TakePictureScreen()

        }

        composable(route = NavScreens.LivenessVerification.route) {

            LivenessVerificationScreen(
                onVerificationComplete = { result ->
                    // result.completedInstructions, result.capturedFilePath
                    // proceed to whatever step-up action needed the liveness check
                },
                onCancel = { /* pop back */ },
            )

        }


        composable(route = NavScreens.FaceScannerInfo.route) {

            FaceScannerInfoScreen()

        }



    }


}


