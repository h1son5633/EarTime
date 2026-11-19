package com.h1son5633.eartime

import android.app.Application
import androidx.room.Room
import com.h1son5633.eartime.data.EarTimeDb
import com.h1son5633.eartime.tracking.SessionManager

class EarTimeApp : Application() {
    val db: EarTimeDb by lazy {
        Room.databaseBuilder(this, EarTimeDb::class.java, "eartime.db").build()
    }
    val sessionManager: SessionManager by lazy { SessionManager(db.sessionDao()) }
}
