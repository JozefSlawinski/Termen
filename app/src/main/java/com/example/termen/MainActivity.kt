package com.example.termen

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.termen.ui.theme.TermenTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermissions()
        bluetoothManager = BluetoothManager(this)  // Przekazanie kontekstu do BluetoothManager
        bluetoothManager.checkPermissionsAndDiscover()
        setContent {
            TermenTheme {
                // A surface container using the 'background' color from the theme
                Surface {
                    TemperatureScreen()
                }
            }
        }
    }


    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BluetoothManager.REQUEST_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Wszystkie uprawnienia zostały przyznane, można kontynuować odkrywanie urządzeń
                bluetoothManager.discoverDevices()
            } else {
                // Niektóre lub wszystkie uprawnienia zostały odrzucone przez użytkownika
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        // Rozłącz i zakończ odkrywanie
        bluetoothManager.disconnect()
        bluetoothAdapter?.cancelDiscovery()
    }
    @Composable
    fun TemperatureScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            var temperature1 by remember { mutableStateOf("Waiting for data...") }
            var temperature2 by remember { mutableStateOf("Waiting for data...") }

            // Assume updateTemperatures is a method to get temperatures from ESP32
            // updateTemperatures(temperature1, temperature2)

            TemperatureCard(label = "Sensor 1", temperature = temperature1)
            Spacer(modifier = Modifier.height(8.dp))
            TemperatureCard(label = "Sensor 2", temperature = temperature2)

            LaunchedEffect(key1 = true) {
                // This is a simple example and may not work as expected in a real-world scenario.
                // You might want to have a better mechanism to update temperatures.
                while (true) {
                    bluetoothManager.updateTemperatures(
                        temperature1 = { temperature1 = it },
                        temperature2 = { temperature2 = it }
                    )
                    // Delay to prevent continuous reading. Adjust the delay as needed.
                    kotlinx.coroutines.delay(2000L)
                }
            }
        }


    }

    @Composable
    fun TemperatureCard(label: String, temperature: String) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = label)
                Text(text = temperature)
            }
        }
    }
    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1
    }
}
