package com.luminai.travel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LuminAILocationParser {

    suspend fun parseLocation(input: String): Triple<Double, Double, String>? = 
        withContext(Dispatchers.IO) {
        
        parseCoordinates(input)?.let { return@withContext it }
        geocodeAddress(input)
    }

    private fun parseCoordinates(input: String): Triple<Double, Double, String>? {
        val regex = """(-?\d+\.?\d*)[,\s]+(-?\d+\.?\d*)""".toRegex()
        val match = regex.find(input.trim())
        
        return match?.let {
            val (lat, lon) = it.destructured
            try {
                val latD = lat.toDouble()
                val lonD = lon.toDouble()
                if (latD in -90.0..90.0 && lonD in -180.0..180.0) {
                    Triple(latD, lonD, "Coords: $latD, $lonD")
                } else null
            } catch (e: NumberFormatException) { null }
        }
    }

    private fun geocodeAddress(address: String): Triple<Double, Double, String>? {
        return try {
            val encoded = URLEncoder.encode(address, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1")
            
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "LUMINAI-Travel/1.0")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            
            val json = JSONArray(response)
            if (json.length() > 0) {
                val result = json.getJSONObject(0)
                Triple(
                    result.getDouble("lat"),
                    result.getDouble("lon"),
                    result.getString("display_name")
                )
            } else null
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
