package com.loren.component.view.composesmartrefresh

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.SubcomposeLayout

@Composable
internal fun SubComposeSmartSwipeRefresh(
    headerIndicator: (@Composable () -> Unit)?,
    footerIndicator: (@Composable () -> Unit)?,
    content: @Composable (header: Int, footer: Int) -> Unit
) {
    SubcomposeLayout { constraints ->
        val header = subcompose("header", headerIndicator ?: {}).firstOrNull()?.measure(constraints)
        val footer = subcompose("footer", footerIndicator ?: {}).firstOrNull()?.measure(constraints)
        val body = subcompose("content") { content(header?.height ?: 0, footer?.height ?: 0) }.first().measure(constraints)
        layout(body.width, body.height) { body.placeRelative(0, 0) }
    }
}
