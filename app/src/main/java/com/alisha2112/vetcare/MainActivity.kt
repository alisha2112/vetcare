package com.alisha2112.vetcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alisha2112.vetcare.ui.VetCareApp
import com.alisha2112.vetcare.ui.theme.VetCareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            VetCareTheme {
                VetCareApp()
            }
        }
    }
}
