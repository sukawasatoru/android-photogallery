package jp.tinyport.photogallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingDataAdapter
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.tinyport.photogallery.data.repository.ImageRepository
import jp.tinyport.photogallery.databinding.ListItemBinding
import jp.tinyport.photogallery.databinding.MainActivityBinding
import jp.tinyport.photogallery.model.MyImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var repo: ImageRepository

    private val vm: MyVm by viewModels()
    private val hugeVm: HugeVm by viewModels()

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        log.info("[MainActivity] onCreate")

        super.onCreate(savedInstanceState)

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usePagingV3()
    }

    override fun onDestroy() {
        log.info("[MainActivity] onDestroy")

        super.onDestroy()
    }

    override fun onResume() {
        log.info("[MainActivity] onResume")

        super.onResume()
    }

    override fun onPause() {
        log.info("[MainActivity] onPause")

        super.onPause()
    }

    override fun onRestart() {
        log.info("[MainActivity] onRestart")

        super.onRestart()
    }

    /**
     * ver. 1d0c559.
     */
    private fun useMyAdapter() {
        val adapter = MyAdapter()
        binding.list.adapter = adapter

        vm.images.asLiveData().observe(this) {
            log.info("[MainActivity] onImages len: %s", it.size)
            adapter.update(lifecycleScope, it)
        }

        if (vm.init) {
            return
        }

        vm.init = true

        vm.viewModelScope.launch {
            repo.retrieveImageAndUpdate()
                    .onCompletion {
                        log.info("onCompletion")
                    }
                    .onStart {
                        log.info("onStart")
                    }
                    .collect {
                        log.info(
                                "collect size: %s",
                                it.map { it.size.toString() }.getOrElse { "(null)" },
                        )
                        when (it) {
                            is Ok -> {
                                vm.images.emit(it.value)
                            }
                            is Err -> {
                                log.warn(it.error)
                            }
                        }
                    }
        }
    }

    /**
     * Paging ver.
     */
    private fun usePagingV3() {
        val diffCallback = object : DiffUtil.ItemCallback<MyImage>() {
            override fun areItemsTheSame(oldItem: MyImage, newItem: MyImage): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MyImage, newItem: MyImage): Boolean {
                return oldItem == newItem
            }
        }

        val adapter = object : PagingDataAdapter<MyImage, MyViewHolder>(diffCallback) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
                return MyViewHolder(ListItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false))
            }

            override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
                val item = getItem(position)
                log.info("[MainActivity] onBindViewHolder pos: %s, item: %s",
                        position, item?.url)
                item?.let {
                    Glide.with(holder.binding.image)
                            .load(it.url)
                            .into(holder.binding.image)
                }
            }

            override fun onViewRecycled(holder: MyViewHolder) {
                super.onViewRecycled(holder)

                Glide.with(holder.binding.image).clear(holder.binding.image)
            }
        }

        binding.list.adapter = adapter

        lifecycleScope.launch {
            hugeVm.pagerFlow.collectLatest { adapter.submitData(it) }
        }

        lifecycleScope.launch {
            adapter.loadStateFlow
                    .collectLatest {
                        log.info("[MainActivity] loadStateFlow: %s", it)
                    }
        }

        // TODO: https://developer.android.com/topic/libraries/architecture/paging/v3-paged-data?hl=ja#load-state-adapter
        // adapter.withLoadStateHeader()
    }

    override fun onTrimMemory(level: Int) {
        val levelString = when (level) {
            TRIM_MEMORY_COMPLETE -> "TRIM_MEMORY_COMPLETE"
            TRIM_MEMORY_MODERATE -> "TRIM_MEMORY_MODERATE"
            TRIM_MEMORY_BACKGROUND -> "TRIM_MEMORY_BACKGROUND"
            TRIM_MEMORY_UI_HIDDEN -> "TRIM_MEMORY_UI_HIDDEN"
            TRIM_MEMORY_RUNNING_CRITICAL -> "TRIM_MEMORY_RUNNING_CRITICAL"
            TRIM_MEMORY_RUNNING_LOW -> "TRIM_MEMORY_RUNNING_LOW"
            TRIM_MEMORY_RUNNING_MODERATE -> "TRIM_MEMORY_RUNNING_MODERATE"
            else -> throw RuntimeException("unreachable")
        }

        log.info("[MainActivity] onTrimMemory: %s", levelString)

        super.onTrimMemory(level)
    }
}

internal class MyVm : ViewModel() {
    var init = false
    val images = MutableSharedFlow<List<MyImage>>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
}

internal class MyViewHolder(val binding: ListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

internal class MyAdapter : RecyclerView.Adapter<MyViewHolder>() {
    private var list = listOf<MyImage>()
    private var updateJob: Job? = null
    private val useDiffUtil = true

    init {
        if (!useDiffUtil) {
            setHasStableIds(true)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(holder.binding.image)
                .load(list[position].url)
                .into(holder.binding.image)
    }

    override fun onViewRecycled(holder: MyViewHolder) {
        super.onViewRecycled(holder)

        Glide.with(holder.binding.image).clear(holder.binding.image)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long {
        return if (useDiffUtil) {
            RecyclerView.NO_ID
        } else {
            list[position].id.hashCode().toLong()
        }
    }

    fun update(scope: CoroutineScope, newList: List<MyImage>) {
        if (!useDiffUtil) {
            list = newList
            notifyDataSetChanged()
            return
        }

        val currentList = ArrayList(list)

        updateJob?.cancel()

        updateJob = scope.launch(Dispatchers.Default) {
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return currentList.size
                }

                override fun getNewListSize(): Int {
                    return newList.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return currentList[oldItemPosition].id == newList[newItemPosition].id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return currentList[oldItemPosition] == newList[newItemPosition]
                }
            })

            currentCoroutineContext().ensureActive()

            withContext(Dispatchers.Main) {
                updateJob = null
                currentCoroutineContext().ensureActive()

                list = newList
                diff.dispatchUpdatesTo(this@MyAdapter)
            }
        }
    }
}

@HiltViewModel
internal class HugeVm @Inject constructor(repo: ImageRepository) : ViewModel() {
    val pagerFlow = Pager(PagingConfig(
            pageSize = 1000,
            initialLoadSize = 1000,
            maxSize = 5000,
    )) {
        object : PagingSource<String, MyImage>() {
            val firstKey = UUID.randomUUID().toString()
            val keys = mutableSetOf(firstKey)

            override suspend fun load(params: LoadParams<String>): LoadResult<String, MyImage> {
                log.info("[MainActivity] load key: %s, loadSize: %s",
                        params.key, params.loadSize)
                // for load between 0 and "after" index.
                val key = if (params.key == firstKey) {
                    null
                } else {
                    params.key
                }
                return when (val data = repo.retrieveImageForPaging(params.loadSize, key)) {
                    is Ok -> {
                        data.value.second?.let {
                            keys.add(it)
                        }
                        var prevKey: String? = null
                        for ((index, entry) in keys.withIndex()) {
                            if (entry == key) {
                                prevKey = keys.elementAtOrNull(index - 1)
                                break
                            }
                        }
                        log.info("[MainActivity] load prevKey: %s, key: %s, nextKey: %s",
                                prevKey, key, data.value.second)
                        LoadResult.Page(data.value.first, prevKey, data.value.second)
                    }
                    is Err -> LoadResult.Error(Exception(data.error))
                }
            }

            override fun getRefreshKey(state: PagingState<String, MyImage>): String? {
                log.info("[MainActivity] getRefreshKey anchorPosition: %s", state.anchorPosition)

                return state.anchorPosition?.let { anchorPosition ->
                    val closest = state.closestPageToPosition(anchorPosition)
                    // TODO: nextKey +1 ?
                    // https://developer.android.com/topic/libraries/architecture/paging/v3-paged-data#pagingsource
                    log.info(
                            "[MainActivity] getRefreshKey prevKey: %s, nextKey: %s",
                            closest?.prevKey,
                            closest?.nextKey,
                    )
                    closest?.prevKey ?: closest?.nextKey
                }
            }
        }
    }
            .flow
            .cachedIn(viewModelScope)
}
