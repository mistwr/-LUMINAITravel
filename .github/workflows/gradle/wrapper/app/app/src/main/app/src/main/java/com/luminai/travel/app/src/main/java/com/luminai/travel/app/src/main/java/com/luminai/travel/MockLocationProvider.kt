package com.luminai.travel

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock

class MockLocationProvider(private val context: Context) {

    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val provider = LocationManager.GPS_PROVIDER

    fun setMockLocation(loc: Location) {
        try {
            lm.removeTestProvider(provider)
        } catch (e: IllegalArgumentException) { }

        lm.addTestProvider(
            provider, false, false, false, false,
            false, false, false, 
            android.location.Criteria.POWER_LOW,
            android.location.Criteria.ACCURACY_FINE
        )
        
        lm.setTestProviderEnabled(provider, true)
        
        val mock = Location(provider).apply {
            latitude = loc.latitude
            longitude = loc.longitude
            accuracy = 5f
            altitude = 0.0
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        
        lm.setTestProviderLocation(provider, mock)
    }

    fun stopMock() {
        try {
            lm.removeTestProvider(provider)
        } catch (e: Exception) { }
    }
}
