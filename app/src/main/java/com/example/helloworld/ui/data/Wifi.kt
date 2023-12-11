package com.example.helloworld.ui.data

import android.bluetooth.BluetoothDevice

// Représente les données
data class Wifi (
    var name: String?
) {
    override fun equals(other: Any?): Boolean {
        // On compare les MAC, pour ne pas ajouté deux fois le même device dans la liste.
        return other is Wifi && other.name == this.name
    }
}