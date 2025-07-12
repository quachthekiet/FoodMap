package com.prm392.foodmap.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.prm392.foodmap.R;
import com.prm392.foodmap.models.Restaurant;
import com.prm392.foodmap.models.RestaurantWithKey;
import com.prm392.foodmap.utils.ImageHelper;

import java.util.List;

public class MyRestaurantAdapter extends RecyclerView.Adapter<MyRestaurantAdapter.ViewHolder> {

    private Context context;

    public interface OnUpdateClickListener {
        void onUpdateClick(RestaurantWithKey restaurantWithKey);
    }

    private final List<RestaurantWithKey> restaurants;
    private final OnUpdateClickListener updateListener;

    public MyRestaurantAdapter(Context context, List<RestaurantWithKey> restaurants, OnUpdateClickListener updateListener) {
        this.context = context;
        this.restaurants = restaurants;
        this.updateListener = updateListener;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_restaurant_manage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RestaurantWithKey rwk = restaurants.get(position);
        Restaurant r = rwk.getRestaurant();

        holder.tvName.setText(r.getName());
        holder.tvAddress.setText(r.getAddress());
        if (!r.isVerified()) {
            holder.tvStatus.setText("Chờ duyệt");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
        } else if (r.isVisible()) {
            holder.tvStatus.setText("Đang hoạt động");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
        } else {
            holder.tvStatus.setText("Ngừng hoạt động");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        }
        if (r.getImages() != null && !r.getImages().isEmpty()) {
            String firstImageUrl = r.getImages().values().iterator().next();
            ImageHelper.loadImage(holder.ivThumbnail.getContext(), firstImageUrl, holder.ivThumbnail);
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.logo);
        }

        holder.btnUpdate.setOnClickListener(v -> {
            if (updateListener != null) {
                updateListener.onUpdateClick(rwk);
            }
        });
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress;
        ImageView ivThumbnail;
        Button btnUpdate;

        TextView tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.myRes_tvName);
            tvAddress = itemView.findViewById(R.id.myRes_tvAddress);
            tvStatus = itemView.findViewById(R.id.myRes_tvStatus);
            ivThumbnail = itemView.findViewById(R.id.myRes_ivThumbnail);
            btnUpdate = itemView.findViewById(R.id.myRes_btnUpdate);
        }
    }
}