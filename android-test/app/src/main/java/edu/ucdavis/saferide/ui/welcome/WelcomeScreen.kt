package edu.ucdavis.saferide.ui.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucdavis.saferide.ui.components.PrimaryButton
import edu.ucdavis.saferide.ui.components.SystemBarIcons
import edu.ucdavis.saferide.ui.theme.AggieBlue
import edu.ucdavis.saferide.ui.theme.AggieGold

/** First screen: van illustration, short description, and "Get Started". */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    SystemBarIcons(darkIcons = false)
    var dialog by remember { mutableStateOf<String?>(null) }
    val dark = isSystemInDarkTheme()

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Aggie Blue status bar strip, like the reference.
        Box(Modifier.fillMaxWidth().background(AggieBlue).statusBarsPadding())

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.surface,
                        0.62f to MaterialTheme.colorScheme.surface,
                        1f to if (dark) Color(0xFF2C3A52) else Color(0xFF8A9AB2),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            SafeRideVan(Modifier.fillMaxWidth(0.72f).height(170.dp))
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Text("UC Davis Safe Ride", fontSize = 30.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            Text(
                "Safe Ride provides transportation services to students and faculty of UC Davis.",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))
            PrimaryButton("Get Started", onClick = onGetStarted)
            Spacer(Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.height(24.dp),
            ) {
                FooterLink("Terms of Use") { dialog = "Terms of Use" }
                VerticalDivider(Modifier.padding(horizontal = 16.dp).height(20.dp))
                FooterLink("Privacy Policy") { dialog = "Privacy Policy" }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    dialog?.let { title ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("OK") } },
            title = { Text(title) },
            text = { Text("This is an ECS 191 class prototype of UC Davis Safe Ride. No account data is collected or sent anywhere.") },
        )
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** A line-art SafeRide van in Aggie Blue and Gold, drawn in code. */
@Composable
private fun SafeRideVan(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        // Drawn in a 300 x 150 design space, scaled to fit.
        val s = minOf(size.width / 300f, size.height / 150f)
        val ox = (size.width - 300f * s) / 2
        val oy = (size.height - 150f * s) / 2
        fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
        val line = Stroke(width = 3f * s)

        val body = Path().apply {
            moveTo(p(18f, 112f).x, p(18f, 112f).y)
            lineTo(p(18f, 88f).x, p(18f, 88f).y)
            quadraticTo(p(20f, 74f).x, p(20f, 74f).y, p(42f, 68f).x, p(42f, 68f).y)
            lineTo(p(66f, 40f).x, p(66f, 40f).y)
            quadraticTo(p(76f, 28f).x, p(76f, 28f).y, p(96f, 28f).x, p(96f, 28f).y)
            lineTo(p(280f, 28f).x, p(280f, 28f).y)
            quadraticTo(p(290f, 28f).x, p(290f, 28f).y, p(290f, 40f).x, p(290f, 40f).y)
            lineTo(p(290f, 112f).x, p(290f, 112f).y)
            close()
        }
        drawPath(body, Color.White)
        drawPath(body, AggieBlue, style = line)

        // Windshield / side window and door line.
        val window = Path().apply {
            moveTo(p(52f, 68f).x, p(52f, 68f).y)
            lineTo(p(72f, 44f).x, p(72f, 44f).y)
            quadraticTo(p(78f, 37f).x, p(78f, 37f).y, p(88f, 37f).x, p(88f, 37f).y)
            lineTo(p(118f, 37f).x, p(118f, 37f).y)
            lineTo(p(118f, 68f).x, p(118f, 68f).y)
            close()
        }
        drawPath(window, AggieBlue, style = Stroke(width = 2.5f * s))
        drawLine(AggieBlue, p(124f, 32f), p(124f, 108f), strokeWidth = 2f * s)
        drawLine(AggieBlue, p(18f, 100f), p(290f, 100f), strokeWidth = 1.5f * s)
        drawRoundRect(AggieBlue, p(22f, 82f), Size(8f * s, 6f * s), CornerRadius(2f * s), style = Stroke(1.5f * s))

        // Markings.
        drawText(measurer, "AGGIE HOST SECURITY", p(138f, 44f), TextStyle(color = AggieGold, fontSize = (9f * s).toSp(), fontWeight = FontWeight.Bold, letterSpacing = (1.2f * s).toSp()))
        drawText(measurer, "SAFE RIDE", p(136f, 56f), TextStyle(color = AggieBlue, fontSize = (25f * s).toSp(), fontWeight = FontWeight.ExtraBold))
        drawCircle(AggieGold, radius = 11f * s, center = p(100f, 84f))
        drawCircle(AggieBlue, radius = 11f * s, center = p(100f, 84f), style = Stroke(2f * s))

        wheel(p(76f, 114f), s)
        wheel(p(240f, 114f), s)
    }
}

private fun DrawScope.wheel(center: Offset, s: Float) {
    drawCircle(Color.White, radius = 24f * s, center = center)
    drawCircle(AggieBlue, radius = 20f * s, center = center, style = Stroke(5f * s))
    drawCircle(AggieBlue, radius = 10f * s, center = center, style = Stroke(2f * s))
    drawCircle(AggieBlue, radius = 3f * s, center = center)
}
