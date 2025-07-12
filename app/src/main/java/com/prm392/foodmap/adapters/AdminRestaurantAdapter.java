package com.prm392.foodmap.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import com.prm392.foodmap.R;
import com.prm392.foodmap.models.Restaurant;

import java.util.List;

public class AdminRestaurantAdapter extends RecyclerView.Adapter<AdminRestaurantAdapter.VH> {
    private final Context context;
    private List<Restaurant> data;
    private final int layoutResId;
    private final RestaurantAdapter.OnRestaurantClickListener clickListener;

    public AdminRestaurantAdapter(Context ctx, List<Restaurant> list, int layoutResId,
                                  RestaurantAdapter.OnRestaurantClickListener listener) {
        this.context = ctx;
        this.data = list;
        this.layoutResId = layoutResId;
        this.clickListener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView txtName, txtAddress, txtRating;
        RatingBar ratingBar;
        Switch swVisible;

        VH(View item) {
            super(item);
            img = item.findViewById(R.id.l_imgRestaurant);
            txtName = item.findViewById(R.id.l_txtName);
            txtAddress = item.findViewById(R.id.l_txtAddress);
            ratingBar = item.findViewById(R.id.l_ratingBar);
            txtRating = item.findViewById(R.id.l_txtRatingCount);
            swVisible = item.findViewById(R.id.switchVisible);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Restaurant res = data.get(pos);

        h.txtName.setText(res.name);
        h.txtAddress.setText(res.address);
        h.ratingBar.setRating((float) res.averageRating);
        h.txtRating.setText(String.valueOf(res.reviewCount));

        if (res.images != null && !res.images.isEmpty()) {
            String url = res.images.values().iterator().next();
            Glide.with(context).load(url).into(h.img);
        }

        if (h.swVisible != null) {
            h.swVisible.setOnCheckedChangeListener(null);
            h.swVisible.setChecked(res.isVisible);
            h.swVisible.setOnCheckedChangeListener((b, checked) -> {
                res.isVisible = checked;
                FirebaseDatabase.getInstance(
                                "https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .getReference("restaurants")
                        .child(res.getKey())
                        .child("isVisible")
                        .setValue(checked);
            });
        }

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onRestaurantClick(res);

        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    public void setRestaurantList(List<Restaurant> newList) {
        data = newList;
        notifyDataSetChanged();
    }
}

