package com.jnetaol.btkbmouse

import android.app.Application
import com.jnetaol.btkbmouse.data.db.AppDatabase
import com.jnetaol.btkbmouse.logger.DebugLogger

class BTKBMouseApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        DebugLogger.i("BTKBMouseApp", "BK-000 Application started")
        database = AppDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: BTKBMouseApp
            private set
    }
}
