package com.prm392.foodmap.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.prm392.foodmap.R;
import com.prm392.foodmap.activities.RestaurantActivity;
import com.prm392.foodmap.models.Restaurant;
import com.prm392.foodmap.utils.ImageHelper;

import java.util.List;

public class MyFavoriteAdapter extends RecyclerView.Adapter<MyFavoriteAdapter.FavViewHolder> {
    private Context context;
    private List<Restaurant> favoriteList;
    private String userId;

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant res);
    }

    private final OnRestaurantClickListener listener;

    public MyFavoriteAdapter(Context context, List<Restaurant> favoriteList, String userId, OnRestaurantClickListener listener) {
        this.context = context;
        this.favoriteList = favoriteList;
        this.userId = userId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite, parent, false);
        return new FavViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavViewHolder holder, int position) {
        Restaurant res = favoriteList.get(position);

        holder.txtName.setText(res.name);
        holder.txtAddress.setText(res.address);
        holder.txtRating.setText(String.format("%d", res.reviewCount));
        holder.ratingBar.setRating(res.averageRating);

        if (res.images != null && !res.images.isEmpty()) {
            String url = res.images.values().iterator().next();
            ImageHelper.loadImage(context, url, holder.imgRestaurant);
        } else {
            holder.imgRestaurant.setImageResource(R.drawable.error_image);
        }

        holder.itemView.setOnClickListener(v -> {
            listener.onRestaurantClick(res);
            Intent intent = new Intent(context, RestaurantActivity.class);
            intent.putExtra("RESTAURANT_ID", res.getKey());
            context.startActivity(intent);
        });

        holder.btnFavorite.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Bỏ yêu thích?")
                    .setMessage("Bạn có chắc muốn xóa nhà hàng này khỏi danh sách yêu thích?")
                    .setPositiveButton("Có", (dialog, which) -> {
                        holder.btnFavorite.setEnabled(false);
                        removeFavorite(holder.getBindingAdapterPosition(), res, holder.btnFavorite);
                    })
                    .setNegativeButton("Không", null)
                    .show();
        });
    }

    private void removeFavorite(int position, Restaurant res, ImageButton btn) {
        if (position == RecyclerView.NO_POSITION) return;

        DatabaseReference favRef = FirebaseDatabase.getInstance()
                .getReference("favorites")
                .child(userId)
                .child(res.getKey());

        favRef.removeValue().addOnCompleteListener(task -> {
            btn.setEnabled(true);

            if (task.isSuccessful()) {
                if (position < favoriteList.size()) {
                    favoriteList.remove(position);
                    notifyItemRemoved(position);
                }
                Toast.makeText(context, "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Lỗi khi bỏ yêu thích", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    static class FavViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRestaurant;
        TextView txtName, txtAddress, txtRating;
        RatingBar ratingBar;
        ImageButton btnFavorite;

        public FavViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRestaurant = itemView.findViewById(R.id.myf_imgRestaurant);
            txtName = itemView.findViewById(R.id.myf_txtName);
            txtAddress = itemView.findViewById(R.id.myf_txtAddress);
            txtRating = itemView.findViewById(R.id.myf_txtRatingCount);
            ratingBar = itemView.findViewById(R.id.myf_ratingBar);
            btnFavorite = itemView.findViewById(R.id.myf_btnFavorite);
        }
    }
}


