package com.luminai.travel

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mockProvider: MockLocationProvider
    private lateinit var parser: LuminAILocationParser
    private lateinit var etInput: EditText
    private lateinit var progressBar: ProgressBar
    private var currentMarker: Marker? = null

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )?.firstOrNull()
            text?.let {
                etInput.setText(it)
                processLocation(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))
        setContentView(R.layout.activity_main)
        
        mapView = findViewById(R.id.mapView)
        etInput = findViewById(R.id.etInput)
        progressBar = findViewById(R.id.progressBar)
        
        mockProvider = MockLocationProvider(this)
        parser = LuminAILocationParser()
        
        setupMap()
        checkPermissions()
        
        findViewById<View>(R.id.btnTeleport).setOnClickListener {
            val input = etInput.text.toString()
            if (input.isNotBlank()) processLocation(input)
        }
        
        findViewById<View>(R.id.btnVoice).setOnClickListener {
            startVoiceRecognition()
        }
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(38.7223, -9.1393))
    }

    private fun checkPermissions() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
        if (perms.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }) {
            ActivityCompat.requestPermissions(this, perms, 100)
        }
    }

    private fun startVoiceRecognition() {
        val intent = RecognizerIntent().apply {
            action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga uma cidade ou morada...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voz não disponível", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processLocation(input: String) {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val coords = parser.parseLocation(input)
            
            runOnUiThread {
                progressBar.visibility = View.GONE
                
                coords?.let { (lat, lon, name) ->
                    updateMap(lat, lon, name)
                    teleport(lat, lon, name)
                } ?: Toast.makeText(this@MainActivity, 
                    "Localização não encontrada", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateMap(lat: Double, lon: Double, title: String) {
        val point = GeoPoint(lat, lon)
        currentMarker?.let { mapView.overlays.remove(it) }
        
        currentMarker = Marker(mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            snippet = "Lat: %.4f, Lon: %.4f".format(lat, lon)
        }
        mapView.overlays.add(currentMarker!!)
        mapView.controller.animateTo(point)
    }

    private fun teleport(lat: Double, lon: Double, name: String) {
        try {
            val loc = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = lat
                longitude = lon
                accuracy = 5f
                time = System.currentTimeMillis()
            }
            mockProvider.setMockLocation(loc)
            Toast.makeText(this, "🚀 Teleportado para $name", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro Mock GPS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
