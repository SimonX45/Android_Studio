package com.example.helloworld

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar



class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Message de bienvenue
        Toast.makeText(this, getString(R.string.message_de_bienvenue), Toast.LENGTH_SHORT).show()   // Durée d'affichage : Snackbar.LENGTH_SHORT / Snackbar.LENGTH_LONG / Snackbar.LENGTH_INDEFINITE

        // Initialisation du bouton
        val bouton_connexion = findViewById<Button>(R.id.bouton_connexion)
        bouton_connexion.setOnClickListener {   // Clic du bouton
            connexionAuPeripheriqueBLE()
        }

        // Initialisation du deuxième bouton
        val deuxieme_bouton = findViewById<Button>(R.id.deuxieme_bouton)
        deuxieme_bouton.setOnClickListener {   // Clic du bouton
            clicDeuxiemeBouton()
        }


    }

    // Snackbar
    private fun connexionAuPeripheriqueBLE() {
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_de_connexion), Snackbar.LENGTH_LONG).setAction("Action") { // Ajoute un bouton "Action" au message
        // Ce qui se passe quand on clique sur le bouton
        Toast.makeText(this, "Action du bouton Snackbar ", Toast.LENGTH_SHORT).show()
        }.show()
    }

    // Clic sur le deuxième bouton
    private fun clicDeuxiemeBouton() {
        MaterialDialog(this).show {
            title(R.string.titre_bouton)
            message(R.string.message_bouton)
        }
    }

}