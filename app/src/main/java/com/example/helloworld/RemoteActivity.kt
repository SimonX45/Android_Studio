package com.example.helloworld

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

class RemoteActivity : AppCompatActivity() {

    // Créer une méthode static qui retourne une intent
    companion object {
        private const val IDENTIFIANT_ID = "IDENTIFIANT_ID"

        fun getStartIntent(context: Context, identifiant: String?): Intent {    // Identifian optionnel avec ?
            return Intent(context, RemoteActivity::class.java).apply { // Créer une intent
                putExtra(IDENTIFIANT_ID, identifiant)                           // Ajoute un extra à l'intent
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote)

        // récupère le texte de textView2
        val textView2 = findViewById<TextView>(R.id.textView2)

        // Met le texte de textView2 à getIdentifiant()
        textView2.text = getIdentifiant()
    }

    // Retourne l'identifiant passé en paramètre à l'activité
    private fun getIdentifiant(): String? {
        return intent.extras?.getString(IDENTIFIANT_ID, null)
    }
}

