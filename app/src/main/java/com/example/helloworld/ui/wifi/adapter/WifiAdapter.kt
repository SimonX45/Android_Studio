package com.example.helloworld.ui.wifi.adapter

import com.example.helloworld.ui.data.Wifi
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.helloworld.R

// TODO : créer graphiquement le textview "title" et le layout "list_item_wifi"
class WifiAdapter(private val wifiList: ArrayList<Wifi>, private val onClick: ((selectedWifi: Wifi) -> Unit)? = null) : RecyclerView.Adapter<WifiAdapter.ViewHolder>() {

    // Comment s'affiche ma vue
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /**
         * Méthode appelée par la vue pour afficher les données
         * Ici nous faisons le lien entre les données et la vue (itemView)
         */
        fun showItem(wifi: Wifi, onClick: ((selectedWifi: Wifi) -> Unit)? = null) {
            itemView.findViewById<TextView>(R.id.title).text = wifi.name

            // Action au clique sur un élément de la liste
            if (onClick != null) {
                itemView.setOnClickListener {
                    onClick(wifi)
                }
            }
        }
    }

    // Retourne une « vue » / « layout » pour chaque élément de la liste
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_wifi, parent, false)
        return ViewHolder(view)
    }

    // Connect la vue ET la données, cette méthode est appelée à chaque fois que l'élément devient visible à l'écran
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.showItem(wifiList[position], onClick)
    }

    // Retourne le nombre d'éléments dans la liste
    override fun getItemCount(): Int {
        return wifiList.size
    }

}