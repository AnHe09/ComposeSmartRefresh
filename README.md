## ComposeSmartRefresh
UI参照SmartRefreshLayout仿写，基于compose实现。有下拉刷新&上拉加载功能(无需Paging3)，并且可设置拖动阈值以及自定义头尾布局。

初始来源是
https://github.com/Loren-Moon/ComposeSmartRefresh
在这个基础上我简化了api，添加了自己开发中需要的状态
```

```kotlin
val listState = rememberLazyListState()
SmartSwipeRefresh(
    initialRefresh = true,
    onRefresh = {
        repository.refresh()
        SmartSwipeResult.Success
    },
    onLoadMore = {
        val hasMore = repository.loadNextPage()
        if (hasMore) SmartSwipeResult.Success else SmartSwipeResult.NoMore
    },
    contentScrollState = listState
) {
    LazyColumn(state = listState) {
        // items(...)
    }
}
```

回调正常返回后，组件自动显示成功状态；回调抛出异常时自动显示失败状态。加载更多返回`SmartSwipeResult.NoMore`后会显示“没有更多数据”，并停止自动和手动加载；下一次刷新会重新允许加载。默认会在后续上拉或滑到底部时再次展示“没有更多数据”提示，但不会再次调用加载回调。

默认配置已经开启下拉刷新、上拉加载和滑动到底部自动加载。只有需要改变交互策略或自定义指示器时，才创建状态：

```kotlin
val refreshState = rememberSmartSwipeRefreshState().apply {
    enableRefresh = true
    enableLoadMore = true
    enableAutoLoadMore = false
    showNoMoreData = true
    autoBackAfterNoMoreData = true
}
```

`enableAutoLoadMore`默认开启，自动加载需要同时传入`contentScrollState`和`onLoadMore`。关闭它后，仍可通过上拉手势触发加载更多。`showNoMoreData`默认开启；关闭后，已经没有更多数据时的后续触底不会展示尾部提示，也不会执行加载回调。`autoBackAfterNoMoreData`默认开启；关闭后提示展示后会停留在展开位置，直到用户向下滚动收起或刷新重置。

可用`headerIndicator`和`footerIndicator`自定义指示器，它们分别接收`refreshState.refreshFlag`和`refreshState.loadMoreFlag`。

使用`initialRefresh = true`可在首次展示时自动刷新。

## :camera_flash: Screenshots

| <img src="/snapshot/refresh_success.gif" width="260"> | <img src="/snapshot/refresh_error.gif" width="260"> |
|-------------------------------------------------------|-----------------------------------------------------|
| <img src="/snapshot/load_success.gif" width="260">    | <img src="/snapshot/load_error.gif" width="260">    |
