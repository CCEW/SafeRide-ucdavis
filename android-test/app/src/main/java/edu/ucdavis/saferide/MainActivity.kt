package edu.ucdavis.saferide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.ucdavis.saferide.ui.SafeRideApp
import edu.ucdavis.saferide.ui.theme.SafeRideTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Each screen sets its own status bar icon color (see SystemBarIcons).
        enableEdgeToEdge()
        setContent {
            SafeRideTheme {
                SafeRideApp(viewModel())
            }
        }
    }
}
