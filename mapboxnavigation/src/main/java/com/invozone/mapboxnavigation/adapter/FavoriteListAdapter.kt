package com.example.mapboxturnmodule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.invozone.mapboxnavigation.databinding.ItemFavoritePlaceListBinding
import com.invozone.mapboxnavigation.listener.CustomOnClickListener
import com.invozone.mapboxnavigation.model.Favorite


class FavoriteListAdapter(private val favoriteList: List<Favorite>,
                          val favClickListener: CustomOnClickListener<Favorite>
) : RecyclerView.Adapter<FavoriteListAdapter.FavoriteListViewHolder>() {

    inner class FavoriteListViewHolder(val binding: ItemFavoritePlaceListBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener{
                favClickListener.onClick(favoriteList[bindingAdapterPosition],bindingAdapterPosition,it)
            }
        }
        fun bind(fav: Favorite) {
            binding.txtPlaceName.text = fav.place_name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteListViewHolder {
       val binding: ItemFavoritePlaceListBinding = ItemFavoritePlaceListBinding.inflate(
           LayoutInflater.from(parent.context),parent,false)
        return FavoriteListViewHolder(
            binding
        )
    }

    override fun onBindViewHolder(holder: FavoriteListViewHolder, position: Int) {
        if(position == favoriteList.size-1){
            holder.binding.singleLineDivider.visibility = View.INVISIBLE
            holder.binding.multipleLineDivider.visibility = View.VISIBLE
        }else{
            holder.binding.singleLineDivider.visibility = View.VISIBLE
            holder.binding.multipleLineDivider.visibility = View.INVISIBLE
        }

        holder.bind(favoriteList[position])
    }

    override fun getItemCount(): Int {
        return favoriteList.size
    }
}