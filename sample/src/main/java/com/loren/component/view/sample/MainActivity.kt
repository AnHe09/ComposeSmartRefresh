package com.loren.component.view.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.loren.component.view.composesmartrefresh.SmartSwipeRefresh
import com.loren.component.view.composesmartrefresh.SmartSwipeResult
import kotlinx.coroutines.delay

@ExperimentalFoundationApi
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scrollState = rememberLazyListState()
            val viewModel by viewModels<MainViewModel>()
            val mainUiState = viewModel.mainUiState.observeAsState()
            SmartSwipeRefresh(
                modifier = Modifier.fillMaxSize(),
                initialRefresh = true,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                contentScrollState = scrollState
            ) {
                CompositionLocalProvider(LocalOverscrollConfiguration.provides(null)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = scrollState
                    ) {
                        mainUiState.value?.data?.let {
                            items(it) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .background(Color.LightGray)
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        modifier = Modifier
                                            .width(32.dp)
                                            .height(32.dp),
                                        painter = painterResource(id = item.icon),
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(text = item.title)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class MainViewModel : ViewModel() {

    private val _mainUiState: MutableLiveData<MainUiState> = MutableLiveData()
    val mainUiState: LiveData<MainUiState>
        get() = _mainUiState

    private val topics = listOf(
        TopicModel("Arts & Crafts", RandomIcon.icon()),
        TopicModel("Beauty", RandomIcon.icon()),
        TopicModel("Books", RandomIcon.icon()),
        TopicModel("Business", RandomIcon.icon()),
        TopicModel("Comics", RandomIcon.icon()),
        TopicModel("Culinary", RandomIcon.icon()),
        TopicModel("Design", RandomIcon.icon()),
        TopicModel("Writing", RandomIcon.icon()),
        TopicModel("Religion", RandomIcon.icon()),
        TopicModel("Technology", RandomIcon.icon()),
        TopicModel("Social sciences", RandomIcon.icon()),
        TopicModel("Arts & Crafts", RandomIcon.icon()),
        TopicModel("Beauty", RandomIcon.icon()),
        TopicModel("Books", RandomIcon.icon()),
        TopicModel("Business", RandomIcon.icon()),
        TopicModel("Comics", RandomIcon.icon()),
        TopicModel("Culinary", RandomIcon.icon()),
        TopicModel("Design", RandomIcon.icon()),
        TopicModel("Writing", RandomIcon.icon()),
        TopicModel("Religion", RandomIcon.icon()),
        TopicModel("Technology", RandomIcon.icon()),
        TopicModel("Social sciences", RandomIcon.icon())
    )

    private var requestShouldFail = false
    private var loadedPageCount = 0

    /** 模拟刷新接口，结果由 SmartSwipeRefresh 自动显示。 */
    suspend fun refresh(): SmartSwipeResult {
        return runCatching {
            delay(2000)
            check(!requestShouldFail)
            loadedPageCount = 1
            _mainUiState.value = MainUiState(
                data = topics.toMutableList().apply {
                    this[0] = this[0].copy(title = System.currentTimeMillis().toString())
                }
            )
            SmartSwipeResult.Success
        }.getOrElse {
            SmartSwipeResult.Error
        }.also {
            requestShouldFail = !requestShouldFail
        }
    }

    /** 模拟分页接口，第三页之后返回 NoMore 以停止继续加载。 */
    suspend fun loadMore(): SmartSwipeResult {
        if (loadedPageCount >= 3) {
            return SmartSwipeResult.NoMore
        }
        return runCatching {
            delay(2000)
            check(!requestShouldFail)
            loadedPageCount += 1
            _mainUiState.value = MainUiState(
                data = (_mainUiState.value?.data.orEmpty() + topics).toMutableList()
            )
            SmartSwipeResult.Success
        }.getOrElse {
            SmartSwipeResult.Error
        }.also {
            requestShouldFail = !requestShouldFail
        }
    }
}

data class TopicModel(
    val title: String,
    @DrawableRes
    val icon: Int
)

data class MainUiState(
    val data: List<TopicModel> = emptyList()
)
