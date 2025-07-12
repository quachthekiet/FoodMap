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

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {

    private Context context;
    private List<Restaurant> restaurantList;

    public RestaurantAdapter(Context context, List<Restaurant> restaurantList) {
        this.context = context;
        this.restaurantList = restaurantList;
    }

    public void setRestaurantList(List<Restaurant> newList) {
        this.restaurantList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_restaurant, parent, false);
        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
        Restaurant res = restaurantList.get(position);
        holder.txtName.setText(res.name);
        holder.txtAddress.setText(res.address);
        holder.ratingBar.setRating((float) res.averageRating);
        holder.txtRating.setText(String.format("%d", res.reviewCount));

        // Load ảnh đầu tiên nếu có
        if (res.images != null && !res.images.isEmpty()) {
            String imageUrl = res.images.values().iterator().next(); // Lấy ảnh đầu
            Glide.with(context).load(imageUrl).into(holder.imgRestaurant);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RestaurantActivity.class);
            intent.putExtra("RESTAURANT_ID", res.getKey());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return restaurantList == null ? 0 : restaurantList.size();
    }

    public static class RestaurantViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRestaurant;
        TextView txtName, txtAddress, txtRating;
        RatingBar ratingBar;


        public RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRestaurant = itemView.findViewById(R.id.l_imgRestaurant);
            txtName = itemView.findViewById(R.id.l_txtName);
            txtAddress = itemView.findViewById(R.id.l_txtAddress);
            ratingBar = itemView.findViewById(R.id.l_ratingBar);
            txtRating = itemView.findViewById(R.id.l_txtRatingCount);
        }
    }
}
