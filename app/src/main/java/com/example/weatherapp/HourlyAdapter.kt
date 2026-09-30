package com.example.weatherapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

data class HourlyItem(
    val label: String,
    val temperature: Double,
    val isNow: Boolean
)

class HourlyAdapter(
    private val items: List<HourlyItem>
) : RecyclerView.Adapter<HourlyAdapter.ViewHolder>() {

    private val minTemp = items.minOfOrNull { it.temperature } ?: 0.0
    private val maxTemp = items.maxOfOrNull { it.temperature } ?: 0.0

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val time: TextView = view.findViewById(R.id.tvHourTime)
        val bar: View = view.findViewById(R.id.viewHourBar)
        val temp: TextView = view.findViewById(R.id.tvHourTemp)
        val root: View = view.findViewById(R.id.hourRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val density = holder.itemView.resources.displayMetrics.density

        holder.time.text = if (item.isNow) "Now" else item.label
        holder.temp.text = "${item.temperature.roundToInt()}°"
        holder.root.setBackgroundResource(
            if (item.isNow) R.drawable.bg_hour_selected else android.R.color.transparent
        )

        // Bar height between 16dp and 72dp, proportional to temperature
        val range = (maxTemp - minTemp).takeIf { it > 0.0 } ?: 1.0
        val fraction = ((item.temperature - minTemp) / range).toFloat()
        val heightDp = 16 + fraction * 56
        holder.bar.layoutParams = holder.bar.layoutParams.apply {
            height = (heightDp * density).toInt()
        }
    }

    override fun getItemCount(): Int = items.size
}