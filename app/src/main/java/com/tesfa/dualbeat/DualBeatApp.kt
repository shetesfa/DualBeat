package com.tesfa.dualbeat

import android.app.Application
import com.tesfa.dualbeat.audio.effects.SoundStudioEngine
import com.tesfa.dualbeat.bluetooth.BluetoothDeviceManager
import com.tesfa.dualbeat.bluetooth.MultiBluetoothEngine
import com.tesfa.dualbeat.data.local.DualBeatDatabase
import com.tesfa.dualbeat.data.preferences.UserPreferencesRepository
import com.tesfa.dualbeat.data.repository.MediaRepository
import com.tesfa.dualbeat.data.repository.PlaylistRepository
import com.tesfa.dualbeat.playback.player.MusicPlaybackController
import com.tesfa.dualbeat.playback.player.VideoPlaybackController
import com.tesfa.dualbeat.receiver.DualBeatCastClient
import com.tesfa.dualbeat.receiver.DualBeatCastServer

class DualBeatApp : Application() {

    // Database & Local Repositories
    lateinit var database: DualBeatDatabase
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set
    lateinit var mediaRepository: MediaRepository
        private set
    lateinit var playlistRepository: PlaylistRepository
        private set

    // Playback Engines
    lateinit var musicController: MusicPlaybackController
        private set
    lateinit var videoController: VideoPlaybackController
        private set

    // Audio Effects & Studio
    lateinit var soundStudioEngine: SoundStudioEngine
        private set

    // Bluetooth Engines
    lateinit var bluetoothDeviceManager: BluetoothDeviceManager
        private set
    lateinit var multiBluetoothEngine: MultiBluetoothEngine
        private set

    // Wi-Fi Cast System
    lateinit var castServer: DualBeatCastServer
        private set
    lateinit var castClient: DualBeatCastClient
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = DualBeatDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        mediaRepository = MediaRepository(
            context = this,
            favoriteDao = database.favoriteDao(),
            historyDao = database.playbackHistoryDao()
        )
        playlistRepository = PlaylistRepository(database.playlistDao())

        musicController = MusicPlaybackController(this)
        videoController = VideoPlaybackController(this)
        soundStudioEngine = SoundStudioEngine()

        // Bind audio session ID of music player to SoundStudioEngine
        musicController.getPlayer()?.let { player ->
            soundStudioEngine.attachSession(player.audioSessionId)
        }

        bluetoothDeviceManager = BluetoothDeviceManager(this)
        multiBluetoothEngine = MultiBluetoothEngine(this, bluetoothDeviceManager)

        castServer = DualBeatCastServer(this)
        castClient = DualBeatCastClient(this)
    }

    override fun onTerminate() {
        musicController.release()
        videoController.release()
        soundStudioEngine.releaseEffects()
        bluetoothDeviceManager.release()
        castServer.release()
        castClient.stopReceiverMode()
        super.onTerminate()
    }

    companion object {
        lateinit var instance: DualBeatApp
            private set
    }
}
