package com.goodnotepad

import android.app.Application
import com.goodnotepad.data.AppDatabase

class NotepadApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
