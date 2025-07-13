// AdminAdapter.java
package com.prm392.foodmap.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prm392.foodmap.R;
import com.prm392.foodmap.activities.RestaurantActivity;
import com.prm392.foodmap.models.Restaurant;

import java.util.List;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.VH> {

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    public interface OnVisibilityChangedListener {
        void onVisibilityChanged();
    }

    private final Context context;
    private List<Restaurant> data;
    private final int layoutResId;
    private final OnRestaurantClickListener clickListener;
    private final OnVisibilityChangedListener visibilityChangedListener;

    public AdminAdapter(Context context, List<Restaurant> data,
                        int layoutResId,
                        OnRestaurantClickListener clickListener,
                        OnVisibilityChangedListener visibilityChangedListener) {
        this.context = context;
        this.data = data;
        this.layoutResId = layoutResId;
        this.clickListener = clickListener;
        this.visibilityChangedListener = visibilityChangedListener;
    }

    public void setRestaurantList(List<Restaurant> newList) {
        data = newList;
        notifyDataSetChanged();
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
        Log.d("ADAPTER", "Inflating layout: " + layoutResId);
        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Log.d("ADAPTER_DEBUG", "Using AdminAdapter layout");

        Restaurant res = data.get(pos);

        h.txtName.setText(res.name);
        h.txtAddress.setText(res.address);
        h.ratingBar.setRating((float) res.averageRating);
        h.txtRating.setText("(" + res.reviewCount + ")");

        if (res.images != null && !res.images.isEmpty()) {
            String url = res.images.values().iterator().next();
            Glide.with(context).load(url).into(h.img);
        }
        loadRating(res, h);
        h.swVisible.setOnCheckedChangeListener(null);
        h.swVisible.setChecked(res.isVisible);
        h.swVisible.setOnCheckedChangeListener((b, checked) -> {
            res.isVisible = checked;
            FirebaseDatabase.getInstance()
                    .getReference("restaurants")
                    .child(res.getKey())
                    .child("isVisible")
                    .setValue(checked);
            if (visibilityChangedListener != null) {
                visibilityChangedListener.onVisibilityChanged();
            }
        });

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onRestaurantClick(res);
            Intent i = new Intent(context, RestaurantActivity.class);
            i.putExtra("RESTAURANT_ID", res.getKey());
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    private void loadRating(Restaurant res, VH h) {
        FirebaseDatabase.getInstance()
                .getReference("reviews")
                .child(res.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int total = 0;
                        float sum = 0;
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Long ratingVal = snap.child("rating").getValue(Long.class);
                            if (ratingVal != null) {
                                sum += ratingVal;
                                total++;
                            }
                        }

                        if (total > 0) {
                            float avg = sum / total;
                            res.averageRating = avg;
                            res.reviewCount = total;

                            h.ratingBar.setRating(avg);
                            h.txtRating.setText("(" + total + ")");
                        } else {
                            h.ratingBar.setRating(0);
                            h.txtRating.setText("(0)");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("RATING", "Failed to load ratings: " + error.getMessage());
                    }
                });
    }
}
