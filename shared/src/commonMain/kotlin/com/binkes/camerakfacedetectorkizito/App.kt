package com.binkes.camerakfacedetectorkizito

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.binkes.camerakfacedetectorkizito.navigation.RootNavGraph

import com.binkes.camerakfacedetectorkizito.ui.FaceScannerInfoScreen

@Composable
@Preview
fun App() {
    MaterialTheme {

        val navController = rememberNavController()


        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            RootNavGraph(
                navController = navController
            )

        }


    }
}