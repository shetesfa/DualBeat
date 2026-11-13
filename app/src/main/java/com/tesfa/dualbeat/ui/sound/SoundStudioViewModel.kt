package com.tesfa.dualbeat.ui.sound

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesfa.dualbeat.DualBeatApp
import com.tesfa.dualbeat.domain.model.AudioEffectState
import com.tesfa.dualbeat.domain.model.EqualizerPresetName
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SoundStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DualBeatApp
    val soundEngine = app.soundStudioEngine

    val effectState: StateFlow<AudioEffectState> = soundEngine.state

    fun toggleEnable(enabled: Boolean) {
        soundEngine.setEnabled(enabled)
    }

    fun setBandLevel(bandIndex: Short, levelMilliBel: Short) {
        soundEngine.setBandLevel(bandIndex, levelMilliBel)
    }

    fun selectPreset(preset: EqualizerPresetName) {
        soundEngine.applyPreset(preset)
    }

    fun setBassBoost(strength: Short) {
        soundEngine.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Short) {
        soundEngine.setVirtualizer(strength)
    }

    fun reset() {
        soundEngine.reset()
    }
}
