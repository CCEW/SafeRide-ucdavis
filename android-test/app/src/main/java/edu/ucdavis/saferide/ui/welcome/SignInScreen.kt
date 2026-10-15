package edu.ucdavis.saferide.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucdavis.saferide.R
import edu.ucdavis.saferide.data.CampusLocations
import edu.ucdavis.saferide.ui.components.PrimaryButton
import edu.ucdavis.saferide.ui.components.SystemBarIcons
import edu.ucdavis.saferide.ui.theme.AggieBlue
import edu.ucdavis.saferide.ui.theme.AggieGold

private const val AUTH_DOMAIN = "fusionauth.downtownerapp.com"

/**
 * Stand-in for the UC Davis sign-in page the real app opens in a browser sheet.
 * The prototype scope excludes authentication (and WebViews), so this is a native screen that
 * signs in the hardcoded rider without a password.
 */
@Composable
fun SignInScreen(signingIn: Boolean, onSignIn: () -> Unit, onClose: () -> Unit) {
    SystemBarIcons(darkIcons = !androidx.compose.foundation.isSystemInDarkTheme())

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Browser-sheet style address bar.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_close), "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_lock), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(AUTH_DOMAIN, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Box(
                Modifier.size(72.dp).background(AggieBlue, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("UC\nDAVIS", color = AggieGold, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 17.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text("Sign in with your UC Davis account", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(28.dp))
            ReadOnlyField("UC Davis email", "alex@ucdavis.edu")
            Spacer(Modifier.height(14.dp))
            ReadOnlyField("Signed in as", CampusLocations.RIDER_NAME)
            Spacer(Modifier.height(28.dp))
            PrimaryButton("Sign In", onClick = onSignIn, loading = signingIn)
            Spacer(Modifier.height(16.dp))
            Text(
                "Simulated sign-in for this prototype. No password is needed and nothing is sent anywhere.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ReadOnlyField(label: String, value: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
