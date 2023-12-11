package com.example.helloworld

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import com.example.helloworld.ui.device.adapter.DeviceAdapter
import com.example.helloworld.ui.wifi.adapter.WifiAdapter


import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner

//import android.bluetooth.*

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.example.helloworld.ui.data.Device
import com.google.android.material.snackbar.Snackbar

//import android.content.pm.PackageManager
//import android.os.Build



class ScanActivity : AppCompatActivity() {

    companion object {
        // Variable ajoutée par moi car elle n'était pas dans le code fournit apr le prof
        // Demande de permission
        private const val PERMISSION_REQUEST_LOCATION = 1
//        private const val REQUEST_BLUETOOTH_CONNECT = 2

        // TEST : Ajouté pour tester
        fun getStartIntent(context: Context): Intent {
            return Intent(context, ScanActivity::class.java)
        }
    }

    // Gestion du Bluetooth
    // L'Adapter permettant de se connecter
    private var bluetoothAdapter: BluetoothAdapter? = null

    // La connexion actuellement établie
    private var currentBluetoothGatt: BluetoothGatt? = null

    // « Interface système nous permettant de scanner »
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    // Parametrage du scan BLE
    private val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

    // On ne retourne que les « Devices » proposant le bon UUID
    private var scanFilters: List<ScanFilter> = arrayListOf(
    //  ScanFilter.Builder().setServiceUuid(ParcelUuid(BluetoothLEManager.DEVICE_UUID)).build()
    )

    // Variable de fonctionnement
    private var mScanning = false
    private val handler = Handler(Looper.getMainLooper())

    // DataSource de notre adapter.
    private val bleDevicesFoundList = arrayListOf<Device>()



    private var rvDevices: RecyclerView? = null
    // TODO : wifi
    private var rvWifi: RecyclerView? = null
    private var startScan: Button? = null
    private var currentConnexion: TextView? = null
    private var disconnect: Button? = null
    private var toggleLed: Button? = null
    // TODO : anoimation
    private var toggleAnimation: Button? = null
    private var ledStatus: ImageView? = null
    private var ledCount: TextView? = null      // Ajouté par moi

    // Ce qui se passe à la création de l'activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        setupRecycler()

        rvWifi = findViewById<RecyclerView>(R.id.rvWifi)
        // TODO : wifi
        rvDevices = findViewById<RecyclerView>(R.id.rvDevices)
//        currentConnexion = findViewById<View>(R.id.currentConnexion)
        currentConnexion = findViewById<TextView>(R.id.currentConnexion)
//        disconnect = findViewById<View>(R.id.disconnect)
        disconnect = findViewById<Button>(R.id.disconnect)
        startScan = findViewById<Button>(R.id.startScan)
//        toggleLed = findViewById<View>(R.id.toggleLed)
        toggleLed = findViewById<Button>(R.id.toggleLed)
        toggleAnimation = findViewById<Button>(R.id.toggleAnimation)

//        ledStatus = findViewById<View>(R.id.ledStatus)
        ledStatus = findViewById<ImageView>(R.id.ledStatus)
        ledCount = findViewById<Button>(R.id.ledCount)      // Ajouté par moi

        startScan?.setOnClickListener {
            askForPermission()  // Après la demande de permission ça lance automatiquement le scan si l'autorisation est acceptée
        }

        disconnect?.setOnClickListener {
            // Appeler la bonne méthode
            disconnectFromCurrentDevice()
        }

        toggleLed?.setOnClickListener {
            // Appeler la bonne méthode
            toggleLed()
        }

        toggleAnimation?.setOnClickListener {
            // Appeler la bonne méthode
            sendAnimation()
        }

        // On cache une partie de l'interface au départ, qui sera affichée une fois que l'on sera connecté au Device
        setUiMode(false)

    }

    /**
     * Gère l'action après la demande de permission.
     * 2 cas possibles :
     * - Réussite 🎉.
     * - Échec (refus utilisateur).
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Ici placer les messages de demande de permission et expliquer pourquoi

        Toast.makeText(this, "onRequestPermissionsResult", Toast.LENGTH_SHORT).show()

        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && locationServiceEnabled()) {
                // Permission OK & service de localisation actif => Nous pouvons lancer l'initialisation du BLE.
                // En appelant la méthode setupBLE(), La méthode setupBLE() va initialiser le BluetoothAdapter et lancera le scan.

                Toast.makeText(this, "Permission OK", Toast.LENGTH_SHORT).show()

                setupBLE()
            } else if (!locationServiceEnabled()) {
                // Inviter à activer la localisation

                Toast.makeText(this, "Veuillez activer la localisation", Toast.LENGTH_SHORT).show()


                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {
                // Permission KO => Gérer le cas.
                // Vous devez ici modifier le code pour gérer le cas d'erreur (permission refusé)
                // Avec par exemple une Dialog

                // Demande à l'utilisateur d'accepter la permission de localisation

                Toast.makeText(this, "Veuillez accepter la permission de localisation", Toast.LENGTH_SHORT).show()

                MaterialDialog(this).show {
                    title(R.string.titre_bouton)
                    message(R.string.message_permission_localisation_refusee)
                }
            }
        }
    }

    /**
     * Permet de vérifier si l'application possede la permission « Localisation ». OBLIGATOIRE pour scanner en BLE
     * Sur Android 11, il faut la permission « BLUETOOTH_CONNECT » et « BLUETOOTH_SCAN »
     * Sur Android 10 et inférieur, il faut la permission « ACCESS_FINE_LOCATION » qui permet de scanner en BLE
     */
    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Demande de la permission (ou des permissions) à l'utilisateur.
     * Sur Android 11, il faut la permission « BLUETOOTH_CONNECT » et « BLUETOOTH_SCAN »
     * Sur Android 10 et inférieur, il faut la permission « ACCESS_FINE_LOCATION » qui permet de scanner en BLE
     */
    private fun askForPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_LOCATION)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), PERMISSION_REQUEST_LOCATION)
        }
    }

    private fun locationServiceEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is new method provided in API 28
            val lm = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.isLocationEnabled
        } else {
            // This is Deprecated in API 28
            val mode = Settings.Secure.getInt(this.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
            mode != Settings.Secure.LOCATION_MODE_OFF
        }
    }

    /**
     * La méthode « registerForActivityResult » permet de gérer le résultat d'une activité.
     * Ce code est appelé à chaque fois que l'utilisateur répond à la demande d'activation du Bluetooth (visible ou non)
     * Si l'utilisateur accepte et donc que le BLE devient disponible, on lance le scan.
     * Si l'utilisateur refuse, on affiche un message d'erreur (Toast).
     */
    val registerForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            // Le Bluetooth est activé, on lance le scan
            scanLeDevice()
        } else {
            // TODO : Bluetooth non activé, vous DEVEZ gérer ce cas autrement qu'avec un Toast.

            // Affiche un dialog pour demander à l'utilisateur d'activer le Bluetooth

        }
    }

    /**
     * Récupération de l'adapter Bluetooth & vérification si celui-ci est actif.
     * Si il n'est pas actif, on demande à l'utilisateur de l'activer. Dans ce cas, au résultat le code présent dans « registerForResult » sera appelé.
     * Si il est déjà actif, on lance le scan.
     */
    @SuppressLint("MissingPermission")
    private fun setupBLE() {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?)?.let { bluetoothManager ->
            bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter != null && !bluetoothManager.adapter.isEnabled) {

                // Demande à activer le Bluetooth
//                MaterialDialog(this).show {
//                    title(R.string.titre_bouton)
//                    message(R.string.message_bluetooth_non_active)
//                }
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_bluetooth_non_active), Snackbar.LENGTH_LONG).setAction("Activer") { // Ajoute un bouton "Action" au message
                    // Ce qui se passe quand on clique sur le bouton
                    registerForResult.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }.show()

            } else {
                scanLeDevice()
            }
        }
    }


    // Le scan va durer 10 secondes seulement, sauf si vous passez une autre valeur comme paramètre.
    @SuppressLint("MissingPermission")
    private fun scanLeDevice(scanPeriod: Long = 10000) {
        if (!mScanning) {
            // Vérifiez si les permissions sont accordées
            if (hasPermission()) {
                bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

                // On vide la liste qui contient les devices actuellement trouvés
                bleDevicesFoundList.clear()

                // Évite de scanner en double
                mScanning = true

                // On lance une tache qui durera « scanPeriod »
                handler.postDelayed({
                    mScanning = false
//                    if (ActivityCompat.checkSelfPermission(
//                            this, // Assurez-vous que 'this' pointe vers votre activité
//                            Manifest.permission.BLUETOOTH_SCAN
//                        ) != PackageManager.PERMISSION_GRANTED
//                    ) {
//                        // La permission n'est pas accordée, vous pouvez demander la permission ici
//                        return@postDelayed
//                    }
                    bluetoothLeScanner?.stopScan(leScanCallback)
                    Toast.makeText(this, getString(R.string.scan_ended), Toast.LENGTH_SHORT).show()
                }, scanPeriod)

                // On lance le scan
                bluetoothLeScanner?.startScan(scanFilters, scanSettings, leScanCallback)
            } else {
                // Demander les permissions ou gérer l'absence de permission
                askForPermission()
            }
        }
    }



    // Callback appelé à chaque périphérique trouvé.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

//            // Vérification de la permission
//            if (ActivityCompat.checkSelfPermission(
//                    this@ScanActivity, // Assurez-vous que 'this@ScanActivity' pointe vers votre activité
//                    Manifest.permission.BLUETOOTH_CONNECT
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                // La permission n'est pas accordée, vous pouvez demander la permission ici
//                // TODO: Demander la permission BLUETOOTH_CONNECT
//                return
//            }

            // Création de l'objet Device
            val device = Device(result.device.name, result.device.address, result.device)
            if (!device.name.isNullOrBlank() && !bleDevicesFoundList.contains(device)) {
                bleDevicesFoundList.add(device)
                // Indique à l'adapter que nous avons ajouté un élément
                findViewById<RecyclerView>(R.id.rvDevices).adapter?.notifyItemInserted(bleDevicesFoundList.size - 1)
            }
        }
    }

    private fun setupRecycler() {
        val rvDevice = findViewById<RecyclerView>(R.id.rvDevices) // Récupération du RecyclerView présent dans le layout
        rvDevice.layoutManager = LinearLayoutManager(this) // Définition du LayoutManager, Comment vont être affichés les éléments, ici en liste
        rvDevice.adapter = DeviceAdapter(bleDevicesFoundList) { device ->
            // Le code écrit ici sera appelé lorsque l'utilisateur cliquera sur un élément de la liste.
            // C'est un « callback », c'est-à-dire une méthode qui sera appelée à un moment précis.
            // Évidemment, vous pouvez faire ce que vous voulez. Nous nous connecterons plus tard à notre périphérique

            // Pour la démo, nous allons afficher un Toast avec le nom du périphérique choisi par l'utilisateur.
            //Toast.makeText(this@ScanActivity, "Clique sur $device", Toast.LENGTH_SHORT).show()

            // Connect to the device
            BluetoothLEManager.currentDevice = device.device    // A garder ?
            connectToCurrentDevice()

        }
    }

    private fun setupWifiRecycler() {
        val rvDevice = findViewById<RecyclerView>(R.id.rvDevices) // Récupération du RecyclerView présent dans le layout
        rvDevice.layoutManager = LinearLayoutManager(this) // Définition du LayoutManager, Comment vont être affichés les éléments, ici en liste
        rvDevice.adapter = DeviceAdapter(bleDevicesFoundList) { device ->
            // Le code écrit ici sera appelé lorsque l'utilisateur cliquera sur un élément de la liste.
            // C'est un « callback », c'est-à-dire une méthode qui sera appelée à un moment précis.
            // Évidemment, vous pouvez faire ce que vous voulez. Nous nous connecterons plus tard à notre périphérique

            // Pour la démo, nous allons afficher un Toast avec le nom du périphérique choisi par l'utilisateur.
            //Toast.makeText(this@ScanActivity, "Clique sur $device", Toast.LENGTH_SHORT).show()

            // Connect to the device
            BluetoothLEManager.currentDevice = device.device    // A garder ?
            connectToCurrentDevice()

        }
    }

    override fun onResume() {
        super.onResume()

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // Test si le téléphone est compatible BLE, si c'est pas le cas, on finish() l'activity
            Toast.makeText(this, getString(R.string.not_compatible), Toast.LENGTH_SHORT).show()
            finish()
        } else if (hasPermission() && locationServiceEnabled()) {
            // Lancer suite => Activation BLE + Lancer Scan
            setupBLE()      // Lance le scan dans cette fonction
        } else if(!hasPermission()) {
            // On demande la permission
            askForPermission()
        } else {
            // On demande d'activer la localisation
            // Idéalement on demande avec un activité.
            // À vous de me proposer mieux (Une activité, une dialog, etc)
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    @SuppressLint("MissingPermission")
    private fun disconnectFromCurrentDevice() {
        currentBluetoothGatt?.disconnect()
        BluetoothLEManager.currentDevice = null
        setUiMode(false)
    }

    @SuppressLint("MissingPermission")
    private fun connectToCurrentDevice() {
        BluetoothLEManager.currentDevice?.let { device ->
            //Toast.makeText(this, "Connexion en cours … $device", Toast.LENGTH_SHORT).show()


            currentBluetoothGatt = device.connectGatt(
                this,
                false,
                BluetoothLEManager.GattCallback(
                    onConnect = {
                        // On indique à l'utilisateur que nous sommes correctement connecté
                        runOnUiThread {
                            //Toast.makeText(this, "Connecté à : $device", Toast.LENGTH_SHORT).show()
                            // Nous sommes connecté au device, on active les notifications pour être notifié si la LED change d'état.

                            // À IMPLÉMENTER
                            // Vous devez appeler la méthode qui active les notifications BLE
                            enableListenBleNotify()

                            // On change la vue « pour être en mode connecté »
                            setUiMode(true)


                            // On sauvegarde dans les « LocalPréférence » de l'application le nom du dernier préphérique sur lequel nous nous sommes connecté


                            // À IMPLÉMENTER EN FONCTION DE CE QUE NOUS AVONS DIT ENSEMBLE
                        }
                    },
                    onNotify = { runOnUiThread {

                        //Toast.makeText(this, "onNotify", Toast.LENGTH_SHORT).show()

                        // VOUS DEVEZ APPELER ICI LA MÉTHODE QUI VA GÉRER LE CHANGEMENT D'ÉTAT DE LA LED DANS L'INTERFACE
                        // Si it (BluetoothGattCharacteristic) est pour l'UUID CHARACTERISTIC_NOTIFY_STATE
                        // Alors vous devez appeler la méthode qui va gérer le changement d'état de la LED

                        // LED ON / OFF
                        if(it.getUuid() == BluetoothLEManager.CHARACTERISTIC_NOTIFY_STATE) {
                             handleToggleLedNotificationUpdate(it)

                        // LED COUNT
                         } else if (it.getUuid() == BluetoothLEManager.CHARACTERISTIC_GET_COUNT) {
                             handleCountLedChangeNotificationUpdate(it)

                             // Toast affichant le nombre de fois que la led a été allumée
                             //Toast.makeText(this, it.getStringValue(0), Toast.LENGTH_SHORT).show()

                        // WIFI SCAN
                        } else if (it.getUuid() == BluetoothLEManager.CHARACTERISTIC_GET_WIFI_SCAN) {
                            // À IMPLÉMENTER
                            handleOnNotifyNotificationReceived(it)
                        }
                    } },
                    onDisconnect = { runOnUiThread { disconnectFromCurrentDevice() } })
            )
        }
    }


    // Ajout de safe calls avec le point d'interrogation car les variables rvDevices, startScan etc... peuvent être null (voir la déclaration plus haute de ces variables private "var rvDevices: RecyclerView? = null"
    @SuppressLint("MissingPermission")
    private fun setUiMode(isConnected: Boolean) {
        if (isConnected) {
            // Connecté à un périphérique
            bleDevicesFoundList.clear()
            rvDevices?.visibility = View.GONE // Cache la liste des appareils trouvés
            startScan?.visibility = View.GONE // Cache le bouton de démarrage du scan
            currentConnexion?.visibility = View.VISIBLE // Affiche l'état de la connexion

            // Met à jour le texte de l'état de la connexion
            currentConnexion?.text = getString(R.string.connected_to, BluetoothLEManager.currentDevice?.name)
            disconnect?.visibility = View.VISIBLE // Affiche le bouton de déconnexion
            toggleLed?.visibility = View.VISIBLE // Affiche le bouton pour activer/désactiver le LED
            toggleAnimation?.visibility = View.VISIBLE // Affiche le bouton pour activer/désactiver le LED
            // A vérifier :
            ledStatus?.visibility = View.VISIBLE   // Affiche l'état du LED
            ledCount?.visibility = View.VISIBLE    // Affiche le nombre de fois que la led a été allumée
        } else {
            // Non connecté, reset de la vue
            rvDevices?.visibility = View.VISIBLE // Affiche la liste des appareils trouvés
            startScan?.visibility = View.VISIBLE // Affiche le bouton de démarrage du scan
            ledStatus?.visibility = View.GONE // Cache l'état du LED
            currentConnexion?.visibility = View.GONE // Cache l'état de la connexion
            disconnect?.visibility = View.GONE // Cache le bouton de déconnexion
            toggleAnimation?.visibility = View.GONE // Affiche le bouton pour activer/désactiver le LED
            // A vérifier :
            toggleLed?.visibility = View.GONE // Cache le bouton pour activer/désactiver le LED
            ledCount?.visibility = View.GONE    // Cache le nombre de fois que la led a été allumée
        }
    }

    /**
     * Récupération de « service » BLE (via UUID) qui nous permettra d'envoyer / recevoir des commandes
     */
    private fun getMainDeviceService(): BluetoothGattService? {
        return currentBluetoothGatt?.let { bleGatt ->
            val service = bleGatt.getService(BluetoothLEManager.DEVICE_UUID)
            service?.let {
                return it
            } ?: run {
                Toast.makeText(this, getString(R.string.uuid_not_found), Toast.LENGTH_SHORT).show()
                return null;
            }
        } ?: run {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show()
            return null
        }
    }

    /**
     * On change l'état de la LED (via l'UUID de toggle)
     */
    @SuppressLint("MissingPermission")
    private fun toggleLed() {
        getMainDeviceService()?.let { service ->
            val toggleLed = service.getCharacteristic(BluetoothLEManager.CHARACTERISTIC_TOGGLE_LED_UUID)
            toggleLed.setValue("1")
            currentBluetoothGatt?.writeCharacteristic(toggleLed)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableListenBleNotify() {
        getMainDeviceService()?.let { service ->
            Toast.makeText(this, getString(R.string.enable_ble_notifications), Toast.LENGTH_SHORT).show()
            // Indique que le GATT Client va écouter les notifications sur le charactérisque
            val notificationStatus = service.getCharacteristic(BluetoothLEManager.CHARACTERISTIC_NOTIFY_STATE)
            val notificationLedCount = service.getCharacteristic(BluetoothLEManager.CHARACTERISTIC_GET_COUNT)
            val wifiScan = service.getCharacteristic(BluetoothLEManager.CHARACTERISTIC_GET_WIFI_SCAN)

            currentBluetoothGatt?.setCharacteristicNotification(notificationStatus, true)
            currentBluetoothGatt?.setCharacteristicNotification(notificationLedCount, true)
            currentBluetoothGatt?.setCharacteristicNotification(wifiScan, true)
        }
    }

    // LED ON / OFF
    private fun handleToggleLedNotificationUpdate(characteristic: BluetoothGattCharacteristic) {
        if (characteristic.getStringValue(0).equals("1", ignoreCase = true)) {
            ledStatus?.setImageResource(R.drawable.led_on)
        } else {
            ledStatus?.setImageResource(R.drawable.led_off)
        }
    }


    // LED COUNT
    private fun handleCountLedChangeNotificationUpdate(characteristic: BluetoothGattCharacteristic) {
//        characteristic.getStringValue(0).toIntOrNull()?.let {
//            ledCount?.text = getString(R.string.led_count, it)  // ledCount?.text = getString(R.string.led_count, it)
//        }

        val stringValue = characteristic.getStringValue(0)
        // Affiche stringValue dans un toast
        Toast.makeText(this, stringValue, Toast.LENGTH_SHORT).show()
        // affiche un toast "hello"
        Toast.makeText(this, "hello", Toast.LENGTH_SHORT).show()
    }


    // WIFI SCAN
    private fun handleOnNotifyNotificationReceived(characteristic: BluetoothGattCharacteristic) {
        // TODO : Vous devez ici récupérer la liste des réseaux WiFi disponibles et les afficher dans une liste.
        // Vous pouvez utiliser un RecyclerView pour afficher la liste des réseaux WiFi disponibles.
        // Vous devez créer un nouvel Activity pour afficher la liste des réseaux WiFi disponibles.

        // OK Vous devez créer un nouvel Adapter pour afficher la liste des réseaux WiFi disponibles.
        // Vous devez créer un nouvel ViewHolder pour afficher la liste des réseaux WiFi disponibles.
        // Vous devez créer un nouvel Layout pour afficher la liste des réseaux WiFi disponibles.
        // Vous devez créer un nouvel Layout pour afficher un élément de la liste des réseaux WiFi disponibles.
        // Vous devez créer un nouvel objet « Data » pour stocker les informations d'un réseau WiFi disponible.

        // Toast affichant characteristic
        //Toast.makeText(this, characteristic.getStringValue(0), Toast.LENGTH_SHORT).show()

        //Toast.makeText(this, getString(R.string.enable_ble_notifications), Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun sendAnimation() {
        getMainDeviceService()?.let { service ->
            val toggleLed = service.getCharacteristic(BluetoothLEManager.CHARACTERISTIC_TOGGLE_LED_UUID)
            toggleLed.setValue("101010101111011111000010101010")
            currentBluetoothGatt?.writeCharacteristic(toggleLed)
        }
    }
}