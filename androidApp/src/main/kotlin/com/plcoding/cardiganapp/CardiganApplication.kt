package com.plcoding.cardiganapp

import android.app.Application
import com.plcoding.cardiganapp.di.initKoin
import org.koin.android.ext.koin.androidContext

class CardiganApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CardiganApplication)
        }
    }
}
