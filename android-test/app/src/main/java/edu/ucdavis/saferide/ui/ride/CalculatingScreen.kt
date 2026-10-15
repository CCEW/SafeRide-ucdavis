package edu.ucdavis.saferide.ui.ride

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucdavis.saferide.R
import edu.ucdavis.saferide.data.CampusStatus
import edu.ucdavis.saferide.data.RideRequest
import edu.ucdavis.saferide.domain.StatusText
import edu.ucdavis.saferide.ui.components.SystemBarIcons
import edu.ucdavis.saferide.ui.theme.AggieBlue
import edu.ucdavis.saferide.ui.theme.AggieGold

/**
 * Shown right after Continue: first "Calculating…", then the rider's place in line and pickup estimate.
 * The ViewModel moves on to live tracking after a short pause.
 */
@Composable
fun CalculatingScreen(ride: RideRequest?, status: CampusStatus?) {
    SystemBarIcons(darkIcons = false)
    // The request is in flight; there's nothing to go back to mid-calculation.
    BackHandler {}

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(Modifier.fillMaxSize().background(AggieBlue), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        ) {
            Box(
                Modifier.size(120.dp).scale(if (ride == null) pulse else 1f).background(AggieGold, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (ride == null) {
                    Icon(painterResource(R.drawable.ic_van), null, tint = AggieBlue, modifier = Modifier.size(60.dp))
                } else {
                    Text("#${ride.queuePosition}", color = AggieBlue, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(36.dp))
            AnimatedContent(
                targetState = ride,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "calculating",
            ) { result ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (result == null) {
                        Text("Calculating your place in line…", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Text("Checking active vans and the current queue", color = Color.White.copy(alpha = 0.75f), fontSize = 16.sp, textAlign = TextAlign.Center)
                    } else {
                        Text("You're #${result.queuePosition} in line", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            StatusText.queueEstimate(result.queuePosition, status?.estimatedWaitMinutes ?: 12),
                            color = AggieGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Pickup at ${result.pickup.stopName}", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
            LinearProgressIndicator(
                color = AggieGold,
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth(0.6f),
            )
        }
    }
}
