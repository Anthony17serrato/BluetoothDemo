package com.serratocreations.bluetoothdemoclient.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.MacAddress
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.serratocreations.bluetoothdemoclient.common.permissions.PermissionsHelper
import com.serratocreations.bluetoothdemoclient.ui.theme.BluetoothDemoTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.Executor
import javax.inject.Inject

private const val SELECT_DEVICE_REQUEST_CODE = 0
private const val BLE_PERMISSIONS_REQUEST_CODE = 1

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var permissionsHelper: PermissionsHelper

    private val deviceManager: CompanionDeviceManager by lazy {
        getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    }
    val mBluetoothAdapter: BluetoothAdapter by lazy {
        val java = BluetoothManager::class.java
        getSystemService(java)!!.adapter
    }
    private val executor: Executor = Executor { it.run() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionsHelper.requestPermissionsIfRequired(
            permissions = arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
            requestCode = BLE_PERMISSIONS_REQUEST_CODE
        )
        setContent {
            BluetoothDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Text(
                            text = "Welcome, Companion device is not paired.",
                            style = MaterialTheme.typography.displayMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { initiateCompanionDevicePairing() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(text = "Look for companion device")
                        }
                    }
                }
            }
        }
    }

    private fun initiateCompanionDevicePairing() {
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID(0x123abcL, -1L)))
            .build()
        // To skip filters based on names and supported feature flags (UUIDs),
        // omit calls to setNamePattern() and addServiceUuid()
        // respectively, as shown in the following  Bluetooth example.
        val deviceFilter: BluetoothLeDeviceFilter = BluetoothLeDeviceFilter.Builder()
            //.setNamePattern(Pattern.compile("My device"))
            //.setScanFilter(scanFilter)
            .build()
        // The argument provided in setSingleDevice() determines whether a single
        // device name or a list of them appears.
        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .setSingleDevice(true)
            .build()
        // When the app tries to pair with a Bluetooth device, show the
        // corresponding dialog box to the user.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            deviceManager.associate(pairingRequest,
                executor,
                object : CompanionDeviceManager.Callback() {
                    // Called when a device is found. Launch the IntentSender so the user
                    // can select the device they want to pair with.
                    override fun onAssociationPending(intentSender: IntentSender) {
                        startIntentSenderForResult(
                            intentSender,
                            SELECT_DEVICE_REQUEST_CODE,
                            null,
                            0,
                            0,
                            0
                        )
                    }

                    override fun onAssociationCreated(associationInfo: AssociationInfo) {
                        // AssociationInfo object is created and get association id and the
                        // macAddress.
                        var associationId = associationInfo.id
                        var macAddress: MacAddress? = associationInfo.deviceMacAddress
                    }

                    override fun onFailure(errorMessage: CharSequence?) {
                        // Handle the failure.
                    }
                }
            )
        } else {
            deviceManager.associate(pairingRequest,
                object : CompanionDeviceManager.Callback() {

                    override fun onDeviceFound(chooserLauncher: IntentSender) {
                        startIntentSenderForResult(chooserLauncher,
                            SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0)
                    }

                    override fun onFailure(error: CharSequence?) {
                        // Handle the failure.
                    }
                }, null)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SELECT_DEVICE_REQUEST_CODE -> when(resultCode) {
                Activity.RESULT_OK -> {
                    // The user chose to pair the app with a Bluetooth device.
                    val scanResult: ScanResult? =
                        data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    scanResult?.let { scanResultNotNull ->
                        if (!permissionsHelper.hasPermissions(Manifest.permission.BLUETOOTH_CONNECT)) {
                            Timber.e("Missing Permission")
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return
                        } else {
                            scanResultNotNull.device.createBond()
                            // Maintain continuous interaction with a paired device.
                        }
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}