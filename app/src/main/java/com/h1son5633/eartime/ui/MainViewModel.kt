package com.h1son5633.eartime.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.h1son5633.eartime.EarTimeApp
import com.h1son5633.eartime.data.SessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as EarTimeApp).db.sessionDao()

    val sessions: StateFlow<List<SessionEntity>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
