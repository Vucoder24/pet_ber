package com.nvv.petber.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpSendPipeline
import io.ktor.util.AttributeKey
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorPlugin private constructor(
    private val context: Context
) {

    class Config {
        lateinit var context: Context
    }

    companion object : HttpClientPlugin<Config, NetworkErrorPlugin> {

        override val key: AttributeKey<NetworkErrorPlugin> =
            AttributeKey("NetworkErrorPlugin")

        override fun prepare(block: Config.() -> Unit): NetworkErrorPlugin {
            val config = Config().apply(block)
            return NetworkErrorPlugin(config.context)
        }

        override fun install(plugin: NetworkErrorPlugin, scope: HttpClient) {

            scope.sendPipeline.intercept(HttpSendPipeline.Monitoring) {
                try {
                    proceed()
                } catch (e: Exception) {

                    if (e is IOException ||
                        e is SocketTimeoutException ||
                        e is UnknownHostException
                    ) {
                        Handler(Looper.getMainLooper()).post {
                            NetworkToastManager.show(plugin.context)
                        }
                    }

                    throw e
                }
            }
        }
    }
}