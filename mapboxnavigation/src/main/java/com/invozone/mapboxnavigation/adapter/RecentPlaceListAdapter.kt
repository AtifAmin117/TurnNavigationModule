package com.example.mapboxturnmodule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.invozone.mapboxnavigation.databinding.ItemRecentPlaceListBinding
import com.invozone.mapboxnavigation.listener.CustomOnClickListener
import com.invozone.mapboxnavigation.model.MainPlace

class RecentPlaceListAdapter(
    private val recentMainPlaceList: List<MainPlace>,
    val recentMainPlaceClickListener: CustomOnClickListener<MainPlace>
    ) : RecyclerView.Adapter<RecentPlaceListAdapter.RecentPlaceListViewHolder>() {

    inner class RecentPlaceListViewHolder(val binding: ItemRecentPlaceListBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener{
                recentMainPlaceClickListener.onClick(recentMainPlaceList[bindingAdapterPosition],bindingAdapterPosition,it)
            }
        }
        fun bind(recentMainPlace: MainPlace) {
            binding.txtPlaceName.text = recentMainPlace.place_name
            binding.txtPlaceDetail.text = recentMainPlace.place_address
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentPlaceListViewHolder {
       val binding: ItemRecentPlaceListBinding = ItemRecentPlaceListBinding.inflate(
           LayoutInflater.from(parent.context),parent,false)
        return RecentPlaceListViewHolder(
            binding
        )
    }

    override fun onBindViewHolder(holder: RecentPlaceListViewHolder, position: Int) {
        holder.bind(recentMainPlaceList[position])
    }

    override fun getItemCount(): Int {
        return recentMainPlaceList.size
    }
}