package com.carstenkeller.videocutter.export

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(val progress: Int) : ExportUiState
    data class Success(val message: String) : ExportUiState
    data class Error(val message: String) : ExportUiState
}
