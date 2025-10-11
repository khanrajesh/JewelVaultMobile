package com.velox.jewelvault.ui.screen.bluetooth

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ManagePrintersViewModel @Inject constructor(
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>
) : ViewModel() {
    
    val currentScreenHeadingState = _currentScreenHeadingState
    
    init {
        // Set screen heading when ViewModel is created
        currentScreenHeadingState.value = "Manage Printers"
    }
}
