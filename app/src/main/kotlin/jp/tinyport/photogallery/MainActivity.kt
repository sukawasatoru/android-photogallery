package jp.tinyport.photogallery

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import dagger.hilt.android.AndroidEntryPoint
import jp.tinyport.photogallery.data.repository.ImageRepository
import jp.tinyport.photogallery.model.MyImage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var repo: ImageRepository

    private val vm: MyVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        log.info("[MainActivity] onCreate")

        super.onCreate(savedInstanceState)

        vm.images.asLiveData().observe(this) {
            log.info("[MainActivity] onImages len: %s", it.size)
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
                        log.info("collect size: %s",
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

