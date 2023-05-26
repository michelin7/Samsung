package com.example.myapplication2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.util.Objects


class PodsAdapter(
    private val pods: ArrayList<Pod>,
    private val onClickListener: PodsOnClickListener
) :
    RecyclerView.Adapter<PodsAdapter.PodsHolder>() {


    inner class PodsHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.card)
        val title: TextView = view.findViewById(R.id.title)
        val content: TextView = view.findViewById(R.id.content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodsHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_pod, parent, false)
        return PodsHolder(itemView)
    }

    override fun onBindViewHolder(holder: PodsHolder, position: Int) {
        val item = pods[position]
        holder.title.text = item.title
        holder.content.text = item.content
        holder.card.setOnClickListener {
            onClickListener.onClicked(item)
        }
    }

    override fun getItemCount(): Int {
        return pods.size
    }
}