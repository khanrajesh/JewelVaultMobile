package com.velox.jewelvault.ui.screen.webview

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class WebViewViewModel @Inject constructor(
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>
) : ViewModel() {

    val currentScreenHeadingState = _currentScreenHeadingState
    val isLoading = mutableStateOf(false) // Start with false to prevent initial flicker
    val errorMessage = mutableStateOf("")

    fun setScreenTitle(title: String) {
        currentScreenHeadingState.value = title
    }

    fun setLoading(loading: Boolean) {
        isLoading.value = loading
    }

    fun setError(error: String) {
        errorMessage.value = error
        isLoading.value = false
    }

    fun clearError() {
        errorMessage.value = ""
    }
}
