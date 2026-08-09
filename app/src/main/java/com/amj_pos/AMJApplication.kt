package com.amj_pos

import android.app.Application
import com.amj_pos.di.AppContainer
import com.amj_pos.di.AppContainerImpl

class AMJApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
    }
}
