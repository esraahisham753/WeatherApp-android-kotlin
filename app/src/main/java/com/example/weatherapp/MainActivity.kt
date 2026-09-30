package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.utils.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var root: View
    private lateinit var progress: ProgressBar
    private lateinit var content: View
    private lateinit var tvLocation: TextView
    private lateinit var tvUpdated: TextView
    private lateinit var tvEmoji: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvHighLow: TextView
    private lateinit var rvHourly: RecyclerView
    private lateinit var rvDaily: RecyclerView
    private lateinit var cardDaily: View
    private lateinit var statsGrid: GridLayout

    // --- State used by the onResume retry logic ---
    private var hasData = false              // weather already displayed
    private var isLocating = false           // waiting for a GPS/network fix
    private var isFetching = false           // API call in flight
    private var awaitingPermission = false   // system permission dialog showing
    private var permissionDenied = false     // user denied during this session
    private var settingsLaunched = false     // location-settings screen already opened
    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.main)
        WindowCompat.getInsetsController(window, root).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindViews()

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onResume() {
        super.onResume()
        // Runs on first launch AND when returning from any settings screen
        loadWeatherIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        // Cancel a pending location request so onResume can cleanly start a new one
        if (isLocating) {
            locationCallback?.let { mFusedLocationProviderClient.removeLocationUpdates(it) }
            locationCallback = null
            isLocating = false
        }
    }

    private fun loadWeatherIfNeeded() {
        if (hasData || isLocating || isFetching || awaitingPermission) return

        if (!isLocationEnabled()) {
            showLoading(false)
            tvCondition.text = "Turn on location to see the weather"
            // Open settings only once, otherwise returning without enabling would loop
            if (!settingsLaunched) {
                settingsLaunched = true
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            return
        }
        settingsLaunched = false

        if (isPermissionGranted()) {
            permissionDenied = false
            requestLocationData()
        } else if (permissionDenied) {
            // Already denied this session: don't re-prompt on every resume
            showLoading(false)
            tvCondition.text = "Location permission is required"
        } else {
            requestPermissions()
        }
    }

    private fun bindViews() {
        progress = findViewById(R.id.progress)
        content = findViewById(R.id.content)
        tvLocation = findViewById(R.id.tvLocation)
        tvUpdated = findViewById(R.id.tvUpdated)
        tvEmoji = findViewById(R.id.tvEmoji)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvCondition = findViewById(R.id.tvCondition)
        tvHighLow = findViewById(R.id.tvHighLow)
        rvHourly = findViewById(R.id.rvHourly)
        rvDaily = findViewById(R.id.rvDaily)
        cardDaily = findViewById(R.id.cardDaily)
        statsGrid = findViewById(R.id.statsGrid)

        rvHourly.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvDaily.layoutManager = LinearLayoutManager(this)
        rvDaily.isNestedScrollingEnabled = false
    }

    private fun showLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        content.visibility = if (loading) View.INVISIBLE else View.VISIBLE
    }

    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != Constants.REQUEST_CODE_LOCATION) return
        awaitingPermission = false

        if (isPermissionGranted()) {
            permissionDenied = false
            if (!isLocating && !isFetching && !hasData) requestLocationData()
        } else {
            permissionDenied = true
            showLoading(false)
            tvCondition.text = "Location permission is required"
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            showAlertDialog()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        isLocating = true
        showLoading(true)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 1000)
            .setMaxUpdates(1)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                isLocating = false
                locationCallback = null
                getWeather(location.latitude, location.longitude)
            }
        }
        locationCallback = callback

        mFusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showAlertDialog() {
        AlertDialog.Builder(this)
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .setTitle("Grant Location Permissions")
            .setMessage("The app needs to access your location to display the weather. It can be granted under app settings")
            .show()
    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
            || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            showLoading(false)
            tvCondition.text = "Location permission is required"
            showAlertDialog()
        } else {
            awaitingPermission = true
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                Constants.REQUEST_CODE_LOCATION
            )
        }
    }

    private fun getWeather(lat: Double, long: Double) {
        if (!Constants.isNetworkAvailable(this)) {
            showLoading(false)
            tvCondition.text = "No internet connection"
            Toast.makeText(this, "There's no internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        isFetching = true

        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val serviceAPI = retrofit.create(WeatherServiceAPI::class.java)
        val call = serviceAPI.getWeatherDetails(
            lat, long, Constants.METRIC_UNIT, Constants.HOURLY, Constants.DAILY
        )

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                isFetching = false
                val weather = response.body()
                if (response.isSuccessful && weather != null) {
                    hasData = true
                    displayWeather(weather)
                } else {
                    showLoading(false)
                    Toast.makeText(this@MainActivity, "An error occurred", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                isFetching = false
                showLoading(false)
                Toast.makeText(this@MainActivity, "Failed to load weather: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun displayWeather(weather: WeatherResponse) {
        val current = weather.currentWeather
        val daily = weather.daily
        val isDay = current?.isDay != 0

        root.setBackgroundResource(if (isDay) R.drawable.bg_gradient_day else R.drawable.bg_gradient_night)

        // Header
        tvLocation.text = weather.timezone.substringAfter('/').replace('_', ' ')
        tvUpdated.text = current?.time?.let { "Updated ${it.substringAfter('T')}" } ?: ""

        // Hourly window (next 24 hours starting from the current hour)
        val times = weather.hourly.time
        val temps = weather.hourly.temperature2m
        val nowHour = current?.time?.take(13) ?: times.firstOrNull()?.take(13) ?: ""
        val startIndex = times.indexOfFirst { it.take(13) >= nowHour }.coerceAtLeast(0)
        val endIndex = minOf(startIndex + 24, times.size, temps.size)

        // Hero section
        if (current != null) {
            tvEmoji.text = WeatherCodes.emoji(current.weatherCode, isDay)
            tvTemperature.text = "${current.temperature.roundToInt()}°"
            tvCondition.text = WeatherCodes.describe(current.weatherCode)
        } else {
            val first = temps.getOrNull(startIndex) ?: 0.0
            tvEmoji.text = "🌡️"
            tvTemperature.text = "${first.roundToInt()}°"
            tvCondition.text = ""
        }

        // Today's high / low
        val maxToday = daily?.temperatureMax?.firstOrNull()
        val minToday = daily?.temperatureMin?.firstOrNull()
        tvHighLow.text = if (maxToday != null && minToday != null) {
            "H: ${maxToday.roundToInt()}°   L: ${minToday.roundToInt()}°"
        } else ""

        // Hourly forecast
        val hourlyItems = (startIndex until endIndex).map { i ->
            HourlyItem(
                label = times[i].substringAfter('T'),
                temperature = temps[i],
                isNow = i == startIndex
            )
        }
        rvHourly.adapter = HourlyAdapter(hourlyItems)

        // 7-day forecast
        val dailyItems = buildDailyItems(daily)
        cardDaily.visibility = if (dailyItems.isEmpty()) View.GONE else View.VISIBLE
        rvDaily.adapter = DailyAdapter(dailyItems)

        // Details grid
        val feelsLike = weather.hourly.apparentTemperature?.getOrNull(startIndex)
        val humidity = weather.hourly.relativeHumidity2m?.getOrNull(startIndex)
        val rainChance = weather.hourly.precipitationProbability?.getOrNull(startIndex)
        val uv = daily?.uvIndexMax?.firstOrNull()
        val sunrise = daily?.sunrise?.firstOrNull()
        val sunset = daily?.sunset?.firstOrNull()

        statsGrid.removeAllViews()
        addStat("WIND", current?.let { "${it.windSpeed.roundToInt()} km/h" } ?: "--")
        addStat("DIRECTION", current?.let { "${WeatherCodes.compass(it.windDirection)} ${it.windDirection.roundToInt()}°" } ?: "--")
        addStat("FEELS LIKE", feelsLike?.let { "${it.roundToInt()}°" } ?: "--")
        addStat("HUMIDITY", humidity?.let { "$it%" } ?: "--")
        addStat("UV INDEX", uv?.let { "${it.roundToInt()} · ${uvLabel(it)}" } ?: "--")
        addStat("RAIN CHANCE", rainChance?.let { "$it%" } ?: "--")
        addStat("SUNRISE", sunrise?.substringAfter('T') ?: "--")
        addStat("SUNSET", sunset?.substringAfter('T') ?: "--")
        addStat("ELEVATION", "${weather.elevation.roundToInt()} m")

        showLoading(false)
    }

    private fun buildDailyItems(daily: Daily?): List<DailyItem> {
        if (daily == null) return emptyList()

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEE", Locale.getDefault())

        return daily.time.indices.mapNotNull { i ->
            val min = daily.temperatureMin.getOrNull(i) ?: return@mapNotNull null
            val max = daily.temperatureMax.getOrNull(i) ?: return@mapNotNull null
            val code = daily.weatherCode.getOrNull(i) ?: 0

            val dayLabel = if (i == 0) {
                "Today"
            } else {
                runCatching { outputFormat.format(inputFormat.parse(daily.time[i])!!) }
                    .getOrDefault(daily.time[i])
            }

            DailyItem(
                day = dayLabel,
                emoji = WeatherCodes.emoji(code, true),
                rainChance = daily.precipitationProbabilityMax?.getOrNull(i),
                min = min,
                max = max
            )
        }
    }

    private fun addStat(label: String, value: String) {
        val view = layoutInflater.inflate(R.layout.item_stat, statsGrid, false)
        view.findViewById<TextView>(R.id.tvStatLabel).text = label
        view.findViewById<TextView>(R.id.tvStatValue).text = value
        view.layoutParams = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply { width = 0 }
        statsGrid.addView(view)
    }

    private fun uvLabel(uv: Double): String = when {
        uv < 3 -> "Low"
        uv < 6 -> "Moderate"
        uv < 8 -> "High"
        uv < 11 -> "Very high"
        else -> "Extreme"
    }
}