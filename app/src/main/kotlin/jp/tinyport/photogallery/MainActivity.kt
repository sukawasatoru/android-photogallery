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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import dagger.hilt.android.AndroidEntryPoint
import jp.tinyport.photogallery.data.repository.ImageRepository
import jp.tinyport.photogallery.databinding.ListItemBinding
import jp.tinyport.photogallery.databinding.MainActivityBinding
import jp.tinyport.photogallery.model.MyImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var repo: ImageRepository

    private val vm: MyVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        log.info("[MainActivity] onCreate")

        super.onCreate(savedInstanceState)

        val binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
    private val list = mutableListOf<MyImage>()
    private var updateJob: Job? = null

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

    fun update(scope: CoroutineScope, newList: List<MyImage>) {
        val currentList = ArrayList(list)

        updateJob?.cancelChildren()

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

                list.clear()
                list.addAll(newList)
                diff.dispatchUpdatesTo(this@MyAdapter)
            }
        }
    }
}
