package com.binkes.camerakfacedetectorkizito.navigation


sealed class NavScreens(val route: String) {

    object Main : NavScreens("main_screen")
    object LivenessVerification : NavScreens("face_recognition_screens")
    object FaceScannerInfo : NavScreens("face_scanner_info_screens")
    object TakePicture : NavScreens("take_picture_screen")

}

