package com.example.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Sharp beveled gothic corners for signature Succubus elegance
val KafkaShapes = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = CutCornerShape(topStart = 10.dp, topEnd = 3.dp, bottomEnd = 10.dp, bottomStart = 3.dp),
    medium = CutCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomEnd = 14.dp, bottomStart = 4.dp),
    large = CutCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomEnd = 18.dp, bottomStart = 6.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun KafkaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KafkaDarkColorScheme else KafkaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KafkaTypography,
        shapes = KafkaShapes,
        content = content
    )
}
