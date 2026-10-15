package edu.ucdavis.saferide.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucdavis.saferide.R
import edu.ucdavis.saferide.ui.components.PrimaryButton
import edu.ucdavis.saferide.ui.components.SystemBarIcons

/** "Ride Not Available", matching the reference. Go Back returns to the review screen. */
@Composable
fun RideUnavailableScreen(message: String, onGoBack: () -> Unit) {
    MessageScreen(
        icon = R.drawable.ic_warning,
        title = "Ride Not Available",
        message = message,
        button = "Go Back",
        onButton = onGoBack,
    )
}

@Composable
private fun MessageScreen(icon: Int, title: String, message: String, button: String, onButton: () -> Unit) {
    SystemBarIcons(darkIcons = !isSystemInDarkTheme())
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(100.dp))
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(84.dp),
        )
        Spacer(Modifier.height(40.dp))
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(18.dp))
        Text(message, fontSize = 18.sp, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        PrimaryButton(button, onClick = onButton, modifier = Modifier.padding(bottom = 20.dp))
    }
}
