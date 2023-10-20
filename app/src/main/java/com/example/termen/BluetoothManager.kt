package com.example.termen

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SerialPortService ID
    private var lastDeviceAddress: String? = null

    private val disconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String = intent!!.action!!
            if (action == BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED || action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                // Handle disconnection
                Log.e("BluetoothManager", "Bluetooth disconnected")
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action!!
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    // Check if the found device is the ESP32-WROOM-32
                    if (device.name == "ESP32-WROOM-32") {
                        bluetoothAdapter?.cancelDiscovery()
                        connect(device.address)
                    }
                }
            }
        }
    }

    init {
        context.registerReceiver(disconnectReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
        context.registerReceiver(disconnectReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED))
    }

    fun checkPermissionsAndDiscover() {
        val requiredPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (requiredPermissions.any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(
                context as MainActivity,
                requiredPermissions,
                REQUEST_PERMISSIONS
            )
        } else {
            discoverDevices()
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverDevices() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)
        bluetoothAdapter?.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String): Boolean {
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        lastDeviceAddress = deviceAddress
        if (device != null) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream
                return true
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Connection failed: ${e.message}")
            }
        }
        return false
    }


    fun disconnect() {
        bluetoothSocket?.close()
        outputStream?.close()
        inputStream?.close()
        context.unregisterReceiver(receiver)
    }

    fun sendData(data: String) {
        if (bluetoothSocket?.isConnected == true) {
            try {
                outputStream?.write(data.toByteArray())
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Write data failed: ${e.message}")
            }
        } else {
            reconnect()
        }
    }

    fun readData(): String? {
        if (bluetoothSocket?.isConnected == true) {
            try {
                val buffer = ByteArray(1024)
                val bytesRead = inputStream?.read(buffer) ?: -1
                return if (bytesRead > 0) {
                    String(buffer, 0, bytesRead)
                } else {
                    null
                }
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Read data failed: ${e.message}")
                reconnect()
                return null
            }
        } else {
            reconnect()
            return null
        }
    }

    fun updateTemperatures(temperature1: (String) -> Unit, temperature2: (String) -> Unit) {
            val data = readData()?.trim()
            if (data != null) {
                val temperatures = data.split(",")
            if (temperatures.size >= 2) {
                val temp1 = temperatures[0].split(":").getOrNull(1)?.subSequence(0,5).toString()
                val temp2 = temperatures[1].split(":").getOrNull(1)?.subSequence(0,5).toString()
                if (temp1 != null && temp2 != null) {
                    temperature1(temp1.trim())  // również usunięcie białych znaków z temp1 i temp2
                    temperature2(temp2.trim())
                }
            }

            // Dodaj toast, aby wyświetlić otrzymane dane
//            Toast.makeText(context, data, Toast.LENGTH_SHORT).show()
        } else {
            // Opcjonalnie, możesz również wyświetlić toast, gdy dane są null
//            Toast.makeText(context, "No data received", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reconnect() {
        val deviceAddress = lastDeviceAddress
        if (deviceAddress != null) {
            // Jeśli znasz ostatni adres urządzenia, spróbuj połączyć się ponownie
            if (!connect(deviceAddress)) {
                // Jeśli ponowne połączenie nie powiedzie się, zainformuj użytkownika lub zastosuj inne strategie
                Toast.makeText(context, "Reconnection failed", Toast.LENGTH_SHORT).show()
            }else{
                showCustomToast("Reconnected")
            }
        } else {
            // Jeśli nie znasz ostatniego adresu urządzenia, możesz spróbować znaleźć urządzenie ponownie
            discoverDevices()
        }
    }
    fun showCustomToast(message: String) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.custom_toast, null)

        val text = layout.findViewById<TextView>(R.id.text)
        text.text = message

        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }

    companion object {
        const val REQUEST_PERMISSIONS = 1
    }
}
