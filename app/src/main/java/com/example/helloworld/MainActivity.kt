package com.example.helloworld

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast



class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation du bouton
        val buttonBle = findViewById<Button>(R.id.button)
        buttonBle.setOnClickListener {
            // Gestion de l'événement de clic
            connexion_au_peripherique_BLE()
        }
    }
    private fun connexion_au_peripherique_BLE() {
        // Code pour démarrer la connexion BLE
        Toast.makeText(this, "Connexion au périphérique BLE...", Toast.LENGTH_SHORT).show()
    }
}