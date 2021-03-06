package com.shimnssso.headonenglish.ui.lecture

import android.app.Application
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.shimnssso.headonenglish.ui.MainActivity
import timber.log.Timber

@Composable
fun ExoPlayerView(
    url: String
) {
    val app = LocalContext.current.applicationContext as Application
    val activity = LocalContext.current as MainActivity
    val viewModel = ViewModelProvider(activity, MediaViewModel.Factory(app)).get(MediaViewModel::class.java)
    val isPlaying by viewModel.isPlaying.observeAsState(false)

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(url) {
        Timber.d("LaunchedEffect. url: %s", url)
        viewModel.prepare(url)

        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStart() {
                Timber.i("onStart()")
                viewModel.resume()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStop() {
                Timber.i("onStop()")
                viewModel.pause()
            }
        })
    }

    // https://developer.android.com/training/scheduling/wakelock#screen
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Timber.d("Add Keep screen on flag")
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Timber.d("Clear keep screen on flag")
        }
    }

    // TODO: Find proper way
    val isVideo = url.endsWith(".mp4")

    AndroidView(
        factory = { context ->
            if (isVideo) {
                PlayerView(context).apply {
                    player = viewModel.exoPlayer
                    setShowNextButton(false)
                }
            } else {
                PlayerControlView(context).apply {
                    player = viewModel.exoPlayer
                    showTimeoutMs = 0  // don't hide
                    setShowNextButton(false)
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
