package com.luminai.travel

import android.app.Application
import org.osmdroid.config.Configuration

class LuminAIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = "LUMINAI-Travel/1.0"
    }
}
