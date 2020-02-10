package io.getstream.chat.android.client

import io.getstream.chat.android.client.api.ChatApi
import io.getstream.chat.android.client.api.ChatApiImpl
import io.getstream.chat.android.client.api.ChatConfig
import io.getstream.chat.android.client.events.ConnectedEvent
import io.getstream.chat.android.client.parser.JsonParserImpl
import io.getstream.chat.android.client.logger.StreamLogger
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.api.models.RetrofitApi
import io.getstream.chat.android.client.api.models.RetrofitCdnApi
import io.getstream.chat.android.client.socket.ChatSocket
import io.getstream.chat.android.client.utils.observable.JustObservable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ClientConnectionTests {

    val userId = "test-id"
    val connectionId = "connection-id"
    val user = User(userId)
    val token = "token"
    val config = ChatConfig.Builder().token(token).build()

    val connectedEvent = ConnectedEvent().apply {
        me = this@ClientConnectionTests.user
        connectionId = this@ClientConnectionTests.connectionId
    }

    lateinit var api: ChatApi
    lateinit var socket: ChatSocket
    lateinit var retrofitApi: RetrofitApi
    lateinit var retrofitCdnApi: RetrofitCdnApi
    lateinit var client: ChatClient
    lateinit var logger: StreamLogger

    @Before
    fun before() {
        socket = mock(ChatSocket::class.java)
        retrofitApi = mock(RetrofitApi::class.java)
        retrofitCdnApi = mock(RetrofitCdnApi::class.java)
        logger = mock(StreamLogger::class.java)
        api = ChatApiImpl(
            retrofitApi,
            retrofitCdnApi,
            config,
            JsonParserImpl(),
            logger
        )
    }

    @Test
    fun successConnection() {

        `when`(socket.events()).thenReturn(JustObservable(connectedEvent))

        client = ChatClientImpl(api, socket, config, logger)
        client.setUser(user, token)

        verify(socket, times(1)).connect(user)
    }

    @Test
    fun connectAndDisconnect() {
        `when`(socket.events()).thenReturn(JustObservable(connectedEvent))

        client = ChatClientImpl(api, socket, config, logger)
        client.setUser(user, token)

        client.disconnect()

        verify(socket, times(1)).disconnect()
    }


}