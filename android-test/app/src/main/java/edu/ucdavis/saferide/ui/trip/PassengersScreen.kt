package edu.ucdavis.saferide.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucdavis.saferide.R
import edu.ucdavis.saferide.domain.TripRules
import edu.ucdavis.saferide.ui.components.SystemBarIcons
import edu.ucdavis.saferide.ui.theme.AggieBlue

/** "How Many Passengers?" with the number pad, wheelchair toggle, and arrow button from the reference. */
@Composable
fun PassengersScreen(
    initialCount: Int?,
    initialWheelchair: Boolean,
    onBack: () -> Unit,
    onNext: (count: Int, wheelchair: Boolean) -> Unit,
) {
    SystemBarIcons(darkIcons = !isSystemInDarkTheme())
    var text by rememberSaveable { mutableStateOf(initialCount?.toString().orEmpty()) }
    var wheelchair by rememberSaveable { mutableStateOf(initialWheelchair) }
    val count = text.toIntOrNull()
    val valid = TripRules.isValidPassengerCount(count)
    val tooMany = count != null && count > TripRules.MAX_PASSENGERS
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "How Many Passengers?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
            BasicTextField(
                value = text,
                onValueChange = { new -> text = new.filter(Char::isDigit).take(2) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { if (valid) onNext(count!!, wheelchair) }),
                textStyle = TextStyle(fontSize = 34.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(AggieBlue),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) { inner() }
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                buildAnnotatedString {
                    append("Max of ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${TripRules.MAX_PASSENGERS}") }
                    append(" passengers")
                },
                fontSize = 17.sp,
                color = if (tooMany) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            WheelchairToggle(selected = wheelchair, onToggle = { wheelchair = !wheelchair }, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(28.dp))
            Row {
                Icon(painterResource(R.drawable.ic_info), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp).size(26.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Rides may be shared.", fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    Text("VIP Mobility ride requests must be 2 passengers or less.", fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Surface(
            onClick = { if (valid) onNext(count!!, wheelchair) },
            enabled = valid,
            shape = CircleShape,
            color = if (valid) AggieBlue else AggieBlue.copy(alpha = 0.3f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).size(68.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_arrow_forward), "Next", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun WheelchairToggle(selected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val content = if (selected) Color.White else MaterialTheme.colorScheme.primary
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(50),
        color = if (selected) AggieBlue else MaterialTheme.colorScheme.surface,
        modifier = modifier.border(1.5.dp, if (selected) AggieBlue else MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 26.dp, vertical = 14.dp)) {
            Icon(painterResource(R.drawable.ic_wheelchair), null, tint = content, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
            Text("Wheelchair", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = content)
        }
    }
}
