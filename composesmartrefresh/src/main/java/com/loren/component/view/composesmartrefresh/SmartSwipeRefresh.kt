package com.loren.component.view.composesmartrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Created by Loren on 2022/6/13
 * Description -> 支持下拉刷新&加载更多的通用组件
 * [state] 刷新以及加载的状态容器，仅用于配置和读取状态
 * [onRefresh] 刷新回调，返回 [SmartSwipeResult] 表示请求结果
 * [onLoadMore] 加载更多回调，返回 [SmartSwipeResult.NoMore] 可停止后续加载
 * [initialRefresh] 是否在首次组合时自动触发刷新
 * [headerIndicator] 自定义头部指示器
 * [footerIndicator] 自定义尾部指示器
 * [contentScrollState] 内容滚动状态；传入后组件才能检测列表是否到达底部
 * [content] 内容布局
 */
@Composable
fun SmartSwipeRefresh(
    modifier: Modifier = Modifier,
    state: SmartSwipeRefreshState = rememberSmartSwipeRefreshState(),
    onRefresh: (suspend () -> SmartSwipeResult)? = null,
    onLoadMore: (suspend () -> SmartSwipeResult)? = null,
    initialRefresh: Boolean = false,
    headerIndicator: @Composable (() -> Unit)? = { MyRefreshHeader(flag = state.refreshFlag) },
    footerIndicator: @Composable (() -> Unit)? = { MyRefreshFooter(flag = state.loadMoreFlag) },
    contentScrollState: ScrollableState? = null,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val connection = remember(coroutineScope, contentScrollState != null, onLoadMore != null) {
        SmartSwipeRefreshNestedScrollConnection(
            state = state,
            coroutineScope = coroutineScope,
            hasScrollableContent = contentScrollState != null,
            hasLoadMoreCallback = onLoadMore != null
        )
    }

    LaunchedEffect(state.refreshFlag) {
        when (state.refreshFlag) {
            SmartSwipeStateFlag.REFRESHING -> {
                state.animateIsOver = false
                state.completeRefresh(executeSwipeAction(onRefresh))
            }

            SmartSwipeStateFlag.ERROR, SmartSwipeStateFlag.SUCCESS -> {
                delay(500)
                state.animateOffsetTo(0f)
            }

            else -> {}
        }
    }

    LaunchedEffect(state.loadMoreFlag) {
        when (state.loadMoreFlag) {
            SmartSwipeStateFlag.REFRESHING -> {
                state.animateIsOver = false
                state.completeLoadMore(executeSwipeAction(onLoadMore))
            }

            SmartSwipeStateFlag.ERROR, SmartSwipeStateFlag.SUCCESS -> {
                delay(500)
                state.animateOffsetTo(0f)
            }

            SmartSwipeStateFlag.NO_MORE -> {
                if (state.autoBackAfterNoMoreData) {
                    delay(500)
                    state.animateOffsetTo(0f)
                }
            }

            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        if (initialRefresh) {
            state.initRefresh()
        }
    }

    LaunchedEffect(state.indicatorOffset) {
        if (state.indicatorOffset < 0 && state.loadMoreFlag != SmartSwipeStateFlag.SUCCESS) {
            contentScrollState?.dispatchRawDelta(-state.indicatorOffset)
        }
    }

    Box(
        modifier = modifier.clipToBounds()
    ) {
        SubComposeSmartSwipeRefresh(
            headerIndicator = headerIndicator, footerIndicator = footerIndicator
        ) { header, footer ->
            state.headerHeight = header.toFloat()
            state.footerHeight = footer.toFloat()

            Box(modifier = Modifier.nestedScroll(connection)) {
                val p = with(LocalDensity.current) { state.indicatorOffset.toDp() }
                val contentModifier = when {
                    p > 0.dp -> Modifier.padding(top = p)
                    p < 0.dp && contentScrollState != null -> Modifier.padding(bottom = -p)
                    p < 0.dp -> Modifier.graphicsLayer { translationY = state.indicatorOffset }
                    else -> Modifier
                }
                Box(modifier = contentModifier) {
                    content()
                }
                headerIndicator?.let {
                    Box(modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer { translationY = -header.toFloat() + state.indicatorOffset }) {
                        headerIndicator()
                    }
                }
                footerIndicator?.let {
                    Box(modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer { translationY = footer.toFloat() + state.indicatorOffset }) {
                        footerIndicator()
                    }
                }
            }
        }
    }
}

/** 执行业务回调，并将普通异常转换为可展示的失败状态。 */
private suspend fun executeSwipeAction(action: (suspend () -> SmartSwipeResult)?): SmartSwipeResult {
    return try {
        action?.invoke() ?: SmartSwipeResult.Success
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        SmartSwipeResult.Error
    }
}

/** 供自定义头部和尾部指示器展示的只读状态。 */
enum class SmartSwipeStateFlag {
    IDLE, REFRESHING, SUCCESS, ERROR, NO_MORE, TIPS_DOWN, TIPS_RELEASE
}

/**
 * 刷新或加载更多回调的执行结果。
 * [NoMore] 仅对加载更多有意义，表示当前页面没有下一页数据。
 */
sealed interface SmartSwipeResult {
    /** 请求成功，允许后续继续加载。 */
    data object Success : SmartSwipeResult

    /** 请求失败，尾部或头部会显示错误状态。 */
    data object Error : SmartSwipeResult

    /** 没有更多数据，后续自动加载和手动加载都会被忽略。 */
    data object NoMore : SmartSwipeResult
}

/**
 * 边界阈值策略
 * [ThresholdScrollStrategy.None] 阈值为0
 * [ThresholdScrollStrategy.UnLimited] 阈值为任意
 * [ThresholdScrollStrategy.Fixed] 阈值为固定数值
 */
sealed interface ThresholdScrollStrategy {
    /** 不额外展开指示器。 */
    data object None : ThresholdScrollStrategy

    /** 不限制指示器展开高度。 */
    data object UnLimited : ThresholdScrollStrategy

    /** 将指示器展开高度限制为指定 px 值。 */
    data class Fixed(val height: Float) : ThresholdScrollStrategy
}

/**
 * 创建并记住 [SmartSwipeRefreshState]。
 * 仅在需要修改默认交互配置或自定义指示器时显式创建；一般场景可省略 `state` 参数。
 */
@Composable
fun rememberSmartSwipeRefreshState(): SmartSwipeRefreshState {
    return remember {
        SmartSwipeRefreshState()
    }
}

/**
 * SmartSwipeRefresh 的可配置状态。
 * 刷新和加载的运行状态由组件维护，业务只需通过回调返回 [SmartSwipeResult]。
 */
class SmartSwipeRefreshState {
    /**
     * 拖动距离的阻尼系数，范围通常为 0 到 1；值越小，指示器移动越慢。
     */
    var stickinessLevel = 0.5f

    /**
     * 拖动时头部指示器允许展开的最大高度策略。
     */
    var dragHeaderIndicatorStrategy: ThresholdScrollStrategy = ThresholdScrollStrategy.UnLimited

    /**
     * 拖动时尾部指示器允许展开的最大高度策略。
     */
    var dragFooterIndicatorStrategy: ThresholdScrollStrategy = ThresholdScrollStrategy.UnLimited

    /**
     * 快速滑动时头部指示器允许展开的最大高度策略。
     */
    var flingHeaderIndicatorStrategy: ThresholdScrollStrategy = ThresholdScrollStrategy.None

    /**
     * 快速滑动时尾部指示器允许展开的最大高度策略。
     */
    var flingFooterIndicatorStrategy: ThresholdScrollStrategy = ThresholdScrollStrategy.None

    /**
     * 头部指示器的实际高度，由组件内部测量。
     */
    var headerHeight = 0f
        internal set

    /**
     * 尾部指示器的实际高度，由组件内部测量。
     */
    var footerHeight = 0f
        internal set

    /**
     * 是否允许下拉刷新。
     */
    var enableRefresh = true

    /**
     * 是否允许手动上拉加载更多。
     */
    var enableLoadMore = true

    /**
     * 内容滑动到底部时是否自动加载更多，默认开启。
     */
    var enableAutoLoadMore = true

    /**
     * 已加载全部数据后，后续上拉或滑到底部时是否继续展示“没有更多数据”提示，默认开启。
     * 该配置只控制提示展示，不会再次触发加载更多回调。
     */
    var showNoMoreData = true

    /**
     * “没有更多数据”提示展示后是否自动回到原位，默认开启。
     * 关闭后尾部提示会停留在展开位置，直到用户向下滚动收起或刷新重置。
     */
    var autoBackAfterNoMoreData = true

    /** fling 释放时是否已经拉出头部或尾部指示器，由组件内部维护。 */
    var releaseIsEdge = false
        internal set

    /** 当前刷新指示器状态，只读；状态由组件根据回调结果自动更新。 */
    var refreshFlag by mutableStateOf(SmartSwipeStateFlag.IDLE)
        internal set

    /** 当前加载更多指示器状态，只读；状态由组件根据回调结果自动更新。 */
    var loadMoreFlag by mutableStateOf(SmartSwipeStateFlag.IDLE)
        internal set

    /** 是否已加载全部数据；刷新开始时会自动重置。 */
    var hasNoMoreData by mutableStateOf(false)
        private set

    /** 指示器动画是否结束，由组件内部维护。 */
    var animateIsOver by mutableStateOf(true)
        internal set
    private val _indicatorOffset = Animatable(0f)
    private val mutatorMutex = MutatorMutex()

    /** 当前指示器相对于静止位置的偏移量，单位为 px；正数为头部，负数为尾部。 */
    val indicatorOffset: Float
        get() = _indicatorOffset.value

    /** 当前是否正在执行刷新、加载更多或指示器动画。 */
    fun isLoading() = !animateIsOver || refreshFlag == SmartSwipeStateFlag.REFRESHING || loadMoreFlag == SmartSwipeStateFlag.REFRESHING

    /** 当前是否允许开始加载更多。 */
    internal fun canLoadMore(): Boolean {
        return enableLoadMore && !hasNoMoreData && !isLoading()
    }

    /** 当前触底时是否应展示已加载全部数据的尾部提示。 */
    internal fun shouldShowNoMoreData(): Boolean {
        return hasNoMoreData && showNoMoreData && footerHeight != 0f
    }

    /** 尝试开始一次加载更多，返回 false 表示当前不可加载。 */
    internal fun startLoadMore(): Boolean {
        if (!canLoadMore()) {
            return false
        }
        loadMoreFlag = SmartSwipeStateFlag.REFRESHING
        return true
    }

    /** 开始一次刷新，并恢复加载更多能力。 */
    internal fun startRefresh() {
        hasNoMoreData = false
        if (loadMoreFlag == SmartSwipeStateFlag.NO_MORE) {
            loadMoreFlag = SmartSwipeStateFlag.IDLE
        }
        refreshFlag = SmartSwipeStateFlag.REFRESHING
    }

    /** 将刷新回调结果转换为头部指示器状态。 */
    internal fun completeRefresh(result: SmartSwipeResult) {
        refreshFlag = if (result == SmartSwipeResult.Error) SmartSwipeStateFlag.ERROR else SmartSwipeStateFlag.SUCCESS
    }

    /** 将加载更多回调结果转换为尾部指示器状态。 */
    internal fun completeLoadMore(result: SmartSwipeResult) {
        loadMoreFlag = when (result) {
            SmartSwipeResult.Success -> SmartSwipeStateFlag.SUCCESS
            SmartSwipeResult.Error -> SmartSwipeStateFlag.ERROR
            SmartSwipeResult.NoMore -> {
                hasNoMoreData = true
                animateIsOver = true
                SmartSwipeStateFlag.NO_MORE
            }
        }
    }

    /**
     * 展开“没有更多数据”尾部提示，且始终保留 [SmartSwipeStateFlag.NO_MORE] 状态。
     * 此方法只提供视觉反馈，不会恢复加载能力或执行加载更多回调。
     */
    internal suspend fun showNoMoreData() {
        if (!shouldShowNoMoreData()) {
            return
        }

        loadMoreFlag = SmartSwipeStateFlag.NO_MORE
        animateIsOver = false
        try {
            mutatorMutex.mutate {
                _indicatorOffset.animateTo(-footerHeight)
            }
            if (autoBackAfterNoMoreData) {
                delay(500)
                animateOffsetTo(0f)
            }
        } finally {
            animateIsOver = true
        }
    }

    /** 将指示器平滑移动到指定的 px 偏移量。 */
    internal suspend fun animateOffsetTo(offset: Float) {
        mutatorMutex.mutate {
            _indicatorOffset.animateTo(offset) {
                if (this.value == 0f) {
                    animateIsOver = true
                }
            }
        }
    }

    /** 将指示器立即移动到指定的 px 偏移量，并更新提示状态。 */
    internal suspend fun snapOffsetTo(offset: Float) {
        mutatorMutex.mutate(MutatePriority.UserInput) {
            _indicatorOffset.snapTo(offset)

            if (indicatorOffset >= headerHeight) {
                refreshFlag = SmartSwipeStateFlag.TIPS_RELEASE
            } else if (indicatorOffset <= -footerHeight && !hasNoMoreData) {
                loadMoreFlag = SmartSwipeStateFlag.TIPS_RELEASE
            } else {
                if (indicatorOffset > 0) {
                    refreshFlag = SmartSwipeStateFlag.TIPS_DOWN
                }
                if (indicatorOffset < 0 && !hasNoMoreData) {
                    loadMoreFlag = SmartSwipeStateFlag.TIPS_DOWN
                }
            }
        }
    }

    /** 开始一次首次刷新，并清除上一次加载更多的结束状态。 */
    internal suspend fun initRefresh() {
        snapOffsetTo(headerHeight)
        startRefresh()
    }

    /** 根据阈值策略计算指示器允许展开的高度。 */
    internal fun strategyIndicatorHeight(strategy: ThresholdScrollStrategy): Float = when (strategy) {
        ThresholdScrollStrategy.None -> 0f
        is ThresholdScrollStrategy.Fixed -> strategy.height
        else -> Float.MAX_VALUE
    }
}

private class SmartSwipeRefreshNestedScrollConnection(
    val state: SmartSwipeRefreshState,
    private val coroutineScope: CoroutineScope,
    private val hasScrollableContent: Boolean,
    private val hasLoadMoreCallback: Boolean
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return when {
            state.isLoading() -> Offset.Zero
            available.y < 0 && state.indicatorOffset > 0 -> {
                // header can drag [state.indicatorOffset, 0]
                val canConsumed = (available.y * state.stickinessLevel).coerceAtLeast(0 - state.indicatorOffset)
                scroll(canConsumed)
            }

            available.y > 0 && state.indicatorOffset < 0 -> {
                // footer can drag [state.indicatorOffset, 0]
                val canConsumed = (available.y * state.stickinessLevel).coerceAtMost(0 - state.indicatorOffset)
                scroll(canConsumed)
            }

            else -> Offset.Zero
        }
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return when {
            state.isLoading() -> Offset.Zero
            available.y < 0 && state.shouldShowNoMoreData() -> {
                coroutineScope.launch {
                    state.showNoMoreData()
                }
                available
            }
            available.y < 0 &&
                hasScrollableContent &&
                hasLoadMoreCallback &&
                state.enableAutoLoadMore &&
                (source == NestedScrollSource.Drag || source == NestedScrollSource.Fling) &&
                state.startLoadMore() -> {
                coroutineScope.launch {
                    state.animateOffsetTo(-state.footerHeight)
                }
                available
            }
            available.y > 0 && state.enableRefresh && state.headerHeight != 0f -> {
                val canConsumed = if (source == NestedScrollSource.Fling) {
                    (available.y * state.stickinessLevel).coerceAtMost(state.strategyIndicatorHeight(state.flingHeaderIndicatorStrategy) - state.indicatorOffset)
                } else {
                    (available.y * state.stickinessLevel).coerceAtMost(state.strategyIndicatorHeight(state.dragHeaderIndicatorStrategy) - state.indicatorOffset)
                }
                scroll(canConsumed)
            }

            available.y < 0 && state.canLoadMore() && state.footerHeight != 0f -> {
                val canConsumed = if (source == NestedScrollSource.Fling) {
                    (available.y * state.stickinessLevel).coerceAtLeast(-state.strategyIndicatorHeight(state.flingFooterIndicatorStrategy) - state.indicatorOffset)
                } else {
                    (available.y * state.stickinessLevel).coerceAtLeast(-state.strategyIndicatorHeight(state.dragFooterIndicatorStrategy) - state.indicatorOffset)
                }
                scroll(canConsumed)
            }

            else -> Offset.Zero
        }
    }

    private fun scroll(canConsumed: Float): Offset {
        return if (canConsumed.absoluteValue > 0.5f) {
            coroutineScope.launch {
                state.snapOffsetTo(state.indicatorOffset + canConsumed)
            }
            Offset(0f, canConsumed / state.stickinessLevel)
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.isLoading()) {
            return Velocity.Zero
        }

        state.releaseIsEdge = state.indicatorOffset != 0f

        if (state.indicatorOffset >= state.headerHeight && state.releaseIsEdge) {
            if (state.refreshFlag != SmartSwipeStateFlag.REFRESHING) {
                state.startRefresh()
                state.animateOffsetTo(state.headerHeight)
                return available
            }
        }

        if (state.indicatorOffset <= -state.footerHeight && state.releaseIsEdge) {
            if (state.startLoadMore()) {
                state.animateOffsetTo(-state.footerHeight)
                return available
            }
        }

        return super.onPreFling(available)
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (state.isLoading()) {
            return Velocity.Zero
        }
        if (available.y < 0 && state.shouldShowNoMoreData()) {
            state.showNoMoreData()
            return available
        }
        if (hasScrollableContent &&
            hasLoadMoreCallback &&
            available.y < 0 &&
            state.enableAutoLoadMore &&
            state.startLoadMore()
        ) {
            state.animateOffsetTo(-state.footerHeight)
            return available
        }
        if (state.refreshFlag != SmartSwipeStateFlag.REFRESHING && state.indicatorOffset > 0) {
            state.refreshFlag = SmartSwipeStateFlag.IDLE
            state.animateOffsetTo(0f)
        }
        if (state.loadMoreFlag != SmartSwipeStateFlag.REFRESHING &&
            state.loadMoreFlag != SmartSwipeStateFlag.NO_MORE &&
            state.indicatorOffset < 0
        ) {
            state.loadMoreFlag = SmartSwipeStateFlag.IDLE
            state.animateOffsetTo(0f)
        }
        return super.onPostFling(consumed, available)
    }
}

@Composable
private fun SubComposeSmartSwipeRefresh(
    headerIndicator: (@Composable () -> Unit)?, footerIndicator: (@Composable () -> Unit)?, content: @Composable (header: Int, footer: Int) -> Unit
) {
    SubcomposeLayout { constraints ->
        val headerPlaceable = subcompose("header", headerIndicator ?: {}).firstOrNull()?.measure(constraints)
        val footerPlaceable = subcompose("footer", footerIndicator ?: {}).firstOrNull()?.measure(constraints)
        val contentPlaceable =
            subcompose("content") { content(headerPlaceable?.height ?: 0, footerPlaceable?.height ?: 0) }.first().measure(constraints)
        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.placeRelative(0, 0)
        }
    }
}

/**
 * 默认的下拉刷新指示器。
 * [flag] 由 [SmartSwipeRefreshState.refreshFlag] 提供；[isNeedTimestamp] 控制是否展示最近刷新时间。
 */
@Composable
fun MyRefreshHeader(flag: SmartSwipeStateFlag, isNeedTimestamp: Boolean = true) {
    var lastRecordTime by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color.White)
    ) {
        val refreshAnimate by rememberInfiniteTransition(label = "MyRefreshHeader").animateFloat(
            initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing)), label = "MyRefreshHeader"
        )
        val transitionState = remember { MutableTransitionState(0) }
        val transition = updateTransition(transitionState, label = "arrowTransition")
        val arrowDegrees by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 500) }, label = "arrowDegrees"
        ) {
            if (it == 0) 0f else 180f
        }
        transitionState.targetState = if (flag == SmartSwipeStateFlag.TIPS_RELEASE) 1 else 0
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(
                modifier = Modifier.rotate(if (flag == SmartSwipeStateFlag.REFRESHING) refreshAnimate else arrowDegrees), imageVector = when (flag) {
                    SmartSwipeStateFlag.IDLE -> Icons.Default.KeyboardArrowDown
                    SmartSwipeStateFlag.REFRESHING -> Icons.Default.Refresh
                    SmartSwipeStateFlag.SUCCESS -> {
                        lastRecordTime = System.currentTimeMillis()
                        Icons.Default.Done
                    }

                    SmartSwipeStateFlag.ERROR -> {
                        lastRecordTime = System.currentTimeMillis()
                        Icons.Default.Warning
                    }

                    SmartSwipeStateFlag.NO_MORE -> Icons.Default.Done
                    SmartSwipeStateFlag.TIPS_DOWN -> Icons.Default.KeyboardArrowDown
                    SmartSwipeStateFlag.TIPS_RELEASE -> Icons.Default.KeyboardArrowDown
                }, contentDescription = null
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = when (flag) {
                        SmartSwipeStateFlag.REFRESHING -> "刷新中..."
                        SmartSwipeStateFlag.SUCCESS -> "刷新成功"
                        SmartSwipeStateFlag.ERROR -> "刷新失败"
                        SmartSwipeStateFlag.NO_MORE -> "刷新完成"
                        SmartSwipeStateFlag.IDLE, SmartSwipeStateFlag.TIPS_DOWN -> "下拉可以刷新"
                        SmartSwipeStateFlag.TIPS_RELEASE -> "释放立即刷新"
                    }, fontSize = 18.sp
                )
                if (isNeedTimestamp) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "上次刷新：${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(lastRecordTime)}", fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * 默认的上拉加载指示器。
 * [flag] 由 [SmartSwipeRefreshState.loadMoreFlag] 提供；[isNeedTimestamp] 控制是否展示最近加载时间。
 */
@Composable
fun MyRefreshFooter(flag: SmartSwipeStateFlag, isNeedTimestamp: Boolean = true) {
    var lastRecordTime by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color.White)
    ) {
        val refreshAnimate by rememberInfiniteTransition(label = "MyRefreshFooter").animateFloat(
            initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing)), label = "MyRefreshFooter"
        )
        val transitionState = remember { MutableTransitionState(0) }
        val transition = updateTransition(transitionState, label = "arrowTransition")
        val arrowDegrees by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 500) }, label = "arrowDegrees"
        ) {
            if (it == 0) 0f else 180f
        }
        transitionState.targetState = if (flag == SmartSwipeStateFlag.TIPS_RELEASE) 1 else 0
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(
                modifier = Modifier.rotate(if (flag == SmartSwipeStateFlag.REFRESHING) refreshAnimate else arrowDegrees), imageVector = when (flag) {
                    SmartSwipeStateFlag.IDLE -> Icons.Default.KeyboardArrowUp
                    SmartSwipeStateFlag.REFRESHING -> Icons.Default.Refresh
                    SmartSwipeStateFlag.SUCCESS -> {
                        lastRecordTime = System.currentTimeMillis()
                        Icons.Default.Done
                    }

                    SmartSwipeStateFlag.ERROR -> {
                        lastRecordTime = System.currentTimeMillis()
                        Icons.Default.Warning
                    }

                    SmartSwipeStateFlag.NO_MORE -> Icons.Default.Done
                    SmartSwipeStateFlag.TIPS_DOWN -> Icons.Default.KeyboardArrowUp
                    SmartSwipeStateFlag.TIPS_RELEASE -> Icons.Default.KeyboardArrowUp
                }, contentDescription = null
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = when (flag) {
                        SmartSwipeStateFlag.REFRESHING -> "正在加载..."
                        SmartSwipeStateFlag.SUCCESS -> "加载成功"
                        SmartSwipeStateFlag.ERROR -> "加载失败"
                        SmartSwipeStateFlag.NO_MORE -> "没有更多数据"
                        SmartSwipeStateFlag.IDLE, SmartSwipeStateFlag.TIPS_DOWN -> "上拉加载更多"
                        SmartSwipeStateFlag.TIPS_RELEASE -> "释放立即加载"
                    }, fontSize = 18.sp
                )
                if (isNeedTimestamp) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "上次加载：${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(lastRecordTime)}", fontSize = 14.sp)
                }
            }
        }
    }
}
