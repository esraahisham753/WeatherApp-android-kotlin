package com.example.weatherapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

data class DailyItem(
    val day: String,
    val emoji: String,
    val rainChance: Int?,
    val min: Double,
    val max: Double
)

class DailyAdapter(
    private val items: List<DailyItem>
) : RecyclerView.Adapter<DailyAdapter.ViewHolder>() {

    private val weekMin = items.minOfOrNull { it.min } ?: 0.0
    private val weekMax = items.maxOfOrNull { it.max } ?: 0.0

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val day: TextView = view.findViewById(R.id.tvDayName)
        val emoji: TextView = view.findViewById(R.id.tvDayEmoji)
        val rain: TextView = view.findViewById(R.id.tvDayRain)
        val min: TextView = view.findViewById(R.id.tvDayMin)
        val max: TextView = view.findViewById(R.id.tvDayMax)
        val spaceStart: View = view.findViewById(R.id.rangeSpaceStart)
        val fill: View = view.findViewById(R.id.rangeFill)
        val spaceEnd: View = view.findViewById(R.id.rangeSpaceEnd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.day.text = item.day
        holder.emoji.text = item.emoji
        holder.min.text = "${item.min.roundToInt()}°"
        holder.max.text = "${item.max.roundToInt()}°"

        val rain = item.rainChance
        holder.rain.text = if (rain != null && rain >= 10) "$rain%" else ""

        // Position the range bar within the week's overall min/max
        val total = (weekMax - weekMin).takeIf { it > 0.0 } ?: 1.0
        val start = ((item.min - weekMin) / total).toFloat()
        val length = ((item.max - item.min) / total).toFloat().coerceAtLeast(0.05f)
        val end = (1f - start - length).coerceAtLeast(0f)

        setWeight(holder.spaceStart, start)
        setWeight(holder.fill, length)
        setWeight(holder.spaceEnd, end)
    }

    private fun setWeight(view: View, weight: Float) {
        view.layoutParams = (view.layoutParams as LinearLayout.LayoutParams).apply {
            this.weight = weight
        }
    }

    override fun getItemCount(): Int = items.size
}