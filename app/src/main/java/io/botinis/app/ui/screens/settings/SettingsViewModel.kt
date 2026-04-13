package io.botinis.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.botinis.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.groqApiKey.collectLatest { key ->
                _apiKey.value = key
                _isConfigured.value = key.isNotBlank()
            }
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.saveGroqApiKey(key)
            _isSaved.value = true
            _isConfigured.value = key.isNotBlank()
        }
    }

    fun clearSavedState() {
        _isSaved.value = false
    }
}
