package com.example.pecimobileapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.pecimobileapp.ui.navigation.AppNavigation
import com.example.pecimobileapp.ui.theme.PeciMobileAppTheme
import com.example.pecimobileapp.viewmodels.BluetoothViewModel

class MainActivity : ComponentActivity() {
    private val bluetoothViewModel: BluetoothViewModel by viewModels()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Todas as permissões concedidas: verifique se o Bluetooth está habilitado
            ensureBluetoothIsEnabled()
        } else {
            // Algumas permissões foram negadas
            Toast.makeText(
                this,
                "As permissões de Bluetooth são necessárias para o funcionamento do app",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Usuário habilitou o Bluetooth
            bluetoothViewModel.updatePairedDevices()
        } else {
            // Usuário não habilitou o Bluetooth
            Toast.makeText(
                this,
                "Bluetooth é obrigatório para o funcionamento do app",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PeciMobileAppTheme {
                // Em vez de chamar MainScreen diretamente, utilizamos o grafo de navegação:
                AppNavigation(viewModel = bluetoothViewModel)
            }
        }

        // Verifica se o dispositivo tem suporte ao Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não está disponível neste dispositivo", Toast.LENGTH_LONG).show()
            return
        }

        // Verifica e solicita as permissões de Bluetooth
        checkBluetoothPermissions()
    }

    private fun checkBluetoothPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ precisa das permissões BLUETOOTH_SCAN e BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            // Versões mais antigas: BLUETOOTH_ADMIN e permissões de localização
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestBluetoothPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            ensureBluetoothIsEnabled()
        }
    }

    private fun ensureBluetoothIsEnabled() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            bluetoothViewModel.updatePairedDevices()
        }
    }
}
