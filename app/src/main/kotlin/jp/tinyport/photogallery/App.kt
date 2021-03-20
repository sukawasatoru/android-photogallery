package jp.tinyport.photogallery

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jp.tinyport.logger.ConsoleLogDestination
import jp.tinyport.logger.Logger
import java.io.OutputStream
import java.io.PrintStream
import javax.inject.Inject
import kotlin.properties.Delegates

var log by Delegates.notNull<Logger>()

@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var logger: Logger

    override fun onCreate() {
        super.onCreate()

        val isReleaseBuild = !BuildConfig.DEBUG

        log = logger
        log.addDestination(ConsoleLogDestination("Photo").apply {
            if (isReleaseBuild) {
                minLevel = Logger.Level.INFO
            }
        })

        if (isReleaseBuild) {
            val emptyStream = PrintStream(object : OutputStream() {
                override fun write(b: Int) {
                    // do nothing.
                }
            })

            System.setOut(emptyStream)
            System.setErr(emptyStream)
        }

        log.info("Hello")
    }
}
