package com.shimnssso.headonenglish.ui.lecture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player.Listener
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

// TODO: Save the preferred speed to db
class MediaViewModel(
    private val app: Application
) : AndroidViewModel(app) {
    val exoPlayer = SimpleExoPlayer.Builder(app).build()

    private var autoPlay: Boolean = false
    var window: Int = 0
    var position: Long = 0L

    private var _speed: MutableLiveData<Float> = MutableLiveData(1.0f)
    val speed: LiveData<Float>
        get() = _speed

    private var _isPlaying: MutableLiveData<Boolean> = MutableLiveData(false)
    val isPlaying: LiveData<Boolean>
        get() = _isPlaying

    private fun updateState() {
        autoPlay = exoPlayer.isPlaying
        window = exoPlayer.currentWindowIndex
        position = 0L.coerceAtLeast(exoPlayer.contentPosition)
        _isPlaying.value = exoPlayer.isPlaying
    }

    fun prepare(url: String) {
        viewModelScope.launch {
            val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
                app,
                Util.getUserAgent(app, app.packageName)
            )
            val mediaItem = MediaItem.fromUri(url)
            val source =
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            exoPlayer.setMediaSource(source)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = autoPlay

            exoPlayer.addListener(object: Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }
            })
        }
    }

    fun pause() {
        viewModelScope.launch {
            Timber.i("pause")
            updateState()
            exoPlayer.pause()
        }
    }

    fun resume() {
        viewModelScope.launch {
            Timber.i("resume")
            if (autoPlay) {
                exoPlayer.play()
            }
        }
    }

    fun speedUp() {
        viewModelScope.launch {
            val newSpeed = ((_speed.value!! + 0.1f) * 10).roundToInt() / 10f
            if (newSpeed > 3.0f) {
                Timber.w("Too high speed($newSpeed)!!. ignore")
                return@launch
            }

            exoPlayer.setPlaybackSpeed(newSpeed)
            _speed.value = newSpeed
            Timber.d("speedUp() to $newSpeed")
        }
    }

    fun speedDown() {
        viewModelScope.launch {
            val newSpeed = ((_speed.value!! - 0.1f) * 10).roundToInt() / 10f
            if (newSpeed < 0.2f) {
                Timber.w("Too low speed($newSpeed)!!. ignore")
                return@launch
            }

            exoPlayer.setPlaybackSpeed(newSpeed)
            _speed.value = newSpeed
            Timber.d("speedDown() to $newSpeed")
        }
    }

    override fun onCleared() {
        Timber.i("onCleared()!!")
        exoPlayer.release()
        _isPlaying.value = false
    }

    class Factory(
        private val app: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                Timber.i("Create MedialViewModel")
                return MediaViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}