package com.carstenkeller.videocutter.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

/**
 * Icon-Button mit Tooltip (langes Antippen/Hover zeigt [label]), damit
 * Funktionsbuttons ohne ausgeschriebenen Text auskommen.
 *
 * Nimmt bewusst eine Drawable-Resource-ID statt eines Compose [ImageVector]
 * aus `material-icons-extended` entgegen: diese Bibliothek allein hat die
 * APK um ~65MB aufgebläht (alle ~12.000 Material-Icons in 5 Stilvarianten,
 * in Debug-Builds ohne Minify komplett ungeschrumpft). Eigene, schlanke
 * Vektor-Icons unter res/drawable/ic_*.xml kosten dagegen nur wenige hundert
 * Byte pro Icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconActionButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(painter = painterResource(id = icon), contentDescription = label, tint = tint)
        }
    }
}
