package com.loren.component.view.composesmartrefresh

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MyRefreshHeader(flag: SmartSwipeStateFlag, isNeedTimestamp: Boolean = true) {
    SwipeIndicator(flag, isNeedTimestamp, isFooter = false)
}

@Composable
fun MyRefreshFooter(flag: SmartSwipeStateFlag, isNeedTimestamp: Boolean = true) {
    SwipeIndicator(flag, isNeedTimestamp, isFooter = true)
}

@Composable
private fun SwipeIndicator(flag: SmartSwipeStateFlag, isNeedTimestamp: Boolean, isFooter: Boolean) {
    var lastRecordTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val title = when (flag) {
        SmartSwipeStateFlag.REFRESHING -> if (isFooter) "正在加载..." else "刷新中..."
        SmartSwipeStateFlag.SUCCESS -> if (isFooter) "加载成功" else "刷新成功"
        SmartSwipeStateFlag.ERROR -> if (isFooter) "加载失败" else "刷新失败"
        SmartSwipeStateFlag.NO_MORE -> if (isFooter) "没有更多数据" else "刷新完成"
        SmartSwipeStateFlag.IDLE, SmartSwipeStateFlag.TIPS_DOWN -> if (isFooter) "上拉加载更多" else "下拉可以刷新"
        SmartSwipeStateFlag.TIPS_RELEASE -> if (isFooter) "释放立即加载" else "释放立即刷新"
    }
    val icon = when (flag) {
        SmartSwipeStateFlag.IDLE, SmartSwipeStateFlag.TIPS_DOWN -> if (isFooter) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
        SmartSwipeStateFlag.TIPS_RELEASE -> if (isFooter) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
        SmartSwipeStateFlag.REFRESHING -> Icons.Default.Refresh
        SmartSwipeStateFlag.NO_MORE, SmartSwipeStateFlag.SUCCESS -> { if (flag == SmartSwipeStateFlag.SUCCESS) lastRecordTime = System.currentTimeMillis(); Icons.Default.Done }
        SmartSwipeStateFlag.ERROR -> { lastRecordTime = System.currentTimeMillis(); Icons.Default.Warning }
    }
    val spinning by rememberInfiniteTransition(label = if (isFooter) "MyRefreshFooter" else "MyRefreshHeader").animateFloat(0f, 360f, infiniteRepeatable(tween(500, easing = LinearEasing)), label = "spin")
    val transitionState = remember { MutableTransitionState(0) }
    val transition = updateTransition(transitionState, label = "arrowTransition")
    val arrowDegrees by transition.animateFloat({ tween(500) }, label = "arrowDegrees") { if (it == 0) 0f else 180f }
    transitionState.targetState = if (flag == SmartSwipeStateFlag.TIPS_RELEASE) 1 else 0
    Box(Modifier.fillMaxWidth().height(80.dp).background(Color.White)) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.rotate(if (flag == SmartSwipeStateFlag.REFRESHING) spinning else arrowDegrees)
            )
            Column(Modifier.padding(start = 8.dp)) {
                Text(title, fontSize = 18.sp)
                if (isNeedTimestamp) {
                    Spacer(Modifier.height(4.dp))
                    Text(if (isFooter) "上次加载：${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(lastRecordTime)}" else "上次刷新：${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(lastRecordTime)}", fontSize = 14.sp)
                }
            }
        }
    }
}
