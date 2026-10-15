package com.drbep.tvplayer

import androidx.compose.runtime.*

class U7dCatalogState {
    var entries by mutableStateOf<List<U7dCatalogEntry>>(emptyList())
    var pageIds by mutableStateOf<Set<String>>(emptySet())
    var querying by mutableStateOf(false)
    var nextChange by mutableStateOf(Long.MAX_VALUE)
    var total by mutableStateOf(0)
    var offset by mutableStateOf(0)
    var channels by mutableStateOf<Map<String,String>>(emptyMap())
    var genres by mutableStateOf<List<String>>(emptyList())
    var queryPage: Runnable = Runnable { }
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
