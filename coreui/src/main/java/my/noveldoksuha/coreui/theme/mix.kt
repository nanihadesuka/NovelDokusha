package my.noveldoksuha.coreui.theme

fun androidx.compose.ui.graphics.Color.mix(
    color: androidx.compose.ui.graphics.Color,
    fraction: Float,
) = androidx.compose.ui.graphics.Color(
    red = red * fraction + color.red * (1f - fraction),
    green = green * fraction + color.green * (1f - fraction),
    blue = blue * fraction + color.blue * (1f - fraction),
    alpha = alpha * fraction + color.alpha * (1f - fraction),
)


