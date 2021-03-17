package jp.tinyport.photogallery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import dagger.hilt.android.AndroidEntryPoint
import jp.tinyport.photogallery.data.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var repo: ImageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        log.info("[MainActivity] onCreate")

        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            val images = when (val data = repo.retrieveImage(10, null)) {
                is Ok -> {
                    data.value
                }
                is Err -> {
                    log.warn("failed to retrieve image: %s", data.error)
                    return@launch
                }
            }
            log.info("succeeded: %s", images)
        }
    }

    override fun onDestroy() {
        log.info("[MainActivity] onDestroy")

        super.onDestroy()
    }
}
