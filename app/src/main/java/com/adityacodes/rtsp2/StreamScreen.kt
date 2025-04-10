package com.adityacodes.rtsp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.adityacodes.rtsp2.StreamViewModel


@OptIn(UnstableApi::class)
@Composable
fun StreamScreen(modifier: Modifier = Modifier,viewModel: StreamViewModel = StreamViewModel(LocalContext.current.applicationContext as Application)) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isInPipMode by remember { mutableStateOf(activity?.isInPictureInPictureMode ?: false) }


    DisposableEffect(Unit) {
        val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(p0: Activity) {
                isInPipMode = p0.isInPictureInPictureMode
            }

            override fun onActivityPaused(p0: Activity) {
                isInPipMode = p0.isInPictureInPictureMode
            }

            override fun onActivityStopped(p0: Activity) {}
            override fun onActivityStarted(p0: Activity) {}
            override fun onActivityDestroyed(p0: Activity) {}
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
        }

        val app = context.applicationContext as Application
        app.registerActivityLifecycleCallbacks(lifecycleCallbacks)

        onDispose {
            app.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        }
    }
    var rtspUrl by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    val player = remember(context) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(DefaultHttpDataSource.Factory())
                    .setLiveTargetOffsetMs(5000)

            )
            .build()
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        OutlinedTextField(
            value = rtspUrl,
            onValueChange = { rtspUrl = it },
            label = { Text("Enter RTSP URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    if (rtspUrl.isNotBlank()) {
                        val mediaItem = MediaItem.fromUri(rtspUrl)

                        val mediaSource = if (rtspUrl.startsWith("rtsp")) {
                            RtspMediaSource.Factory().createMediaSource(mediaItem)
                        } else {
                            DefaultMediaSourceFactory(context).createMediaSource(mediaItem)
                        }

                        player.setMediaSource(mediaSource)
                        player.prepare()
                        player.playWhenReady = true
                        isStreaming = true
                    }
                },
                enabled = !isStreaming
            ) {
                Text("Play")
            }

            Button(
                onClick = {
                    if (rtspUrl.isNotBlank() && !isRecording) {
                        viewModel.startRecording(rtspUrl) { success, message ->
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        isRecording = true
                    }
                },
                enabled = isStreaming && !isRecording
            ) {
                Text(if (isRecording) "Recording..." else "Start Recording")
            }

            Button( onClick = {
                if (isRecording) {
                    viewModel.stopRecording()
                    isRecording = false
                    Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show()
                }
            }){
                Text("Stop Recording")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isStreaming) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        this.player = player
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun spreview(){
    StreamScreen()
}
