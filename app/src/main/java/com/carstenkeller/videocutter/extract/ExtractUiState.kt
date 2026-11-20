package com.carstenkeller.videocutter.extract

sealed interface ExtractUiState {
    data object Idle : ExtractUiState
    data class Extracting(val progress: Int) : ExtractUiState
    data class Success(val message: String) : ExtractUiState
    data class Error(val message: String) : ExtractUiState
}
