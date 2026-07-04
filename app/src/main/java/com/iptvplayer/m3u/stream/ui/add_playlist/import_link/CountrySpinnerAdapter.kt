package com.iptvplayer.m3u.stream.ui.add_playlist.import_link

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.model.entity.Country


class CountrySpinnerAdapter(
    context: Context,
    private val countries: List<Country>,
    private var selectedPosition: Int = 0
) : android.widget.ArrayAdapter<Country>(context, 0, countries) {

    interface OnCountrySelectedListener {
        fun onCountrySelected(country: Country, position: Int)
    }

    var listener: OnCountrySelectedListener? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_country_selected, parent, false)
//        if (position == 0) {
//            // Ẩn flag, chỉ hiện text "Select Country"
//            view.findViewById<ImageView>(R.id.ivFlag)?.visibility = View.GONE
//            view.findViewById<TextView>(R.id.tvCountryName)?.apply {
//                text = ""
//            }
//        }
        val country = countries[position]
        view.findViewById<ImageView>(R.id.ivFlag).setImageResource(country.icon)
        view.findViewById<TextView>(R.id.tvCountryName).text = country.name

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_country_dropdown, parent, false)

        val country = countries[position]
        view.findViewById<ImageView>(R.id.ivFlag).setImageResource(country.icon)
        view.findViewById<TextView>(R.id.tvCountryName).text = country.name

        val radioButton = view.findViewById<RadioButton>(R.id.rbSelect)
        radioButton.isChecked = position == selectedPosition

        view.setOnClickListener {
            selectedPosition = position
            listener?.onCountrySelected(country, position)
            notifyDataSetChanged()
        }
        radioButton.setOnClickListener {
            selectedPosition = position
            listener?.onCountrySelected(country, position)
            notifyDataSetChanged()
        }

        return view
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }


}