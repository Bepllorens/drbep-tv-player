package com.drbep.tvplayer

import androidx.compose.runtime.*

class U7dCatalogState {
    var entries by mutableStateOf<List<U7dCatalogEntry>>(emptyList())
    var loading by mutableStateOf(false)
    var status by mutableStateOf("Cargando programación…")
    var day by mutableStateOf("")
    var channel by mutableStateOf("")
    var genre by mutableStateOf("")
    var query by mutableStateOf("")
    var savedOnly by mutableStateOf(false)
    var selectedId by mutableStateOf("")
    var format by mutableStateOf("Auto")
    var revision by mutableStateOf(0)
}
