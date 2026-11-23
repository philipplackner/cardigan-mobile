package com.plcoding.cardiganapp

import android.app.Application
import android.content.pm.ApplicationInfo
import com.plcoding.cardiganapp.di.initKoin
import org.koin.android.ext.koin.androidContext

class CardiganApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // AGP 9 doesn't generate BuildConfig by default, so the debug flag comes from the manifest.
        initKoin(isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            androidContext(this@CardiganApplication)
        }
    }
}
