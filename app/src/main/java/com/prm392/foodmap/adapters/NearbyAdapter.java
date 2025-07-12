package com.prm392.foodmap.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.prm392.foodmap.R;
import com.prm392.foodmap.activities.RestaurantActivity;
import com.prm392.foodmap.models.Restaurant;

import java.util.List;

public class NearbyAdapter extends RecyclerView.Adapter<NearbyAdapter.NearbyRestaurantViewHolder> {

    public interface OnRestaurantClickListener {
        void onClick(Restaurant restaurant);
    }
    private Context context;
    private List<Restaurant> restaurantList;
    private OnRestaurantClickListener listener;

    public NearbyAdapter(Context context, List<Restaurant> restaurantList, OnRestaurantClickListener listener) {
        this.context = context;
        this.restaurantList = restaurantList;
        this.listener = listener;
    }

    public void setRestaurantList(List<Restaurant> newList) {
        this.restaurantList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NearbyAdapter.NearbyRestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nearby, parent, false);
        return new NearbyAdapter.NearbyRestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NearbyAdapter.NearbyRestaurantViewHolder holder, int position) {
        Restaurant res = restaurantList.get(position);
        holder.txtName.setText(res.name);
        holder.txtAddress.setText(res.address);
        holder.ratingBar.setRating((float) res.averageRating);
        holder.txtRating.setText(String.format("%d", res.reviewCount));
        holder.txtDistance.setText(String.format("(%.1f km)", res.distance));

        // Load ảnh đầu tiên nếu có
        if (res.images != null && !res.images.isEmpty()) {
            String imageUrl = res.images.values().iterator().next(); // Lấy ảnh đầu
            Glide.with(context).load(imageUrl).into(holder.imgRestaurant);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(res);
            }
            Intent intent = new Intent(context, RestaurantActivity.class);
            intent.putExtra("RESTAURANT_ID", res.getKey());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return restaurantList == null ? 0 : restaurantList.size();
    }

    public static class NearbyRestaurantViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRestaurant;
        TextView txtName, txtAddress, txtRating, txtDistance;
        RatingBar ratingBar;


        public NearbyRestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRestaurant = itemView.findViewById(R.id.n_imgRestaurant);
            txtName = itemView.findViewById(R.id.n_txtName);
            txtAddress = itemView.findViewById(R.id.n_txtAddress);
            ratingBar = itemView.findViewById(R.id.n_ratingBar);
            txtRating = itemView.findViewById(R.id.n_txtRatingCount);
            txtDistance = itemView.findViewById(R.id.n_tvDistance);
        }
    }

}
