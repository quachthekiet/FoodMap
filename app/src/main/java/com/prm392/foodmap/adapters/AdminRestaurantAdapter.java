// AdminRestaurantAdapter.java
package com.prm392.foodmap.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.WriterException;
import com.prm392.foodmap.R;
import com.prm392.foodmap.interfaces.DataCallback;
import com.prm392.foodmap.models.Restaurant;
import com.prm392.foodmap.utils.QRCodeHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AdminRestaurantAdapter extends RecyclerView.Adapter<AdminRestaurantAdapter.VH> {

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    private final Context context;
    private final List<Restaurant> data;
    private final int layoutResId;
    private final OnRestaurantClickListener clickListener;

    public AdminRestaurantAdapter(Context context, List<Restaurant> data,
                                  int layoutResId,
                                  OnRestaurantClickListener clickListener) {
        this.context = context;
        this.data = data;
        this.layoutResId = layoutResId;
        this.clickListener = clickListener;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView txtName, txtAddress, txtRatingCount;
        RatingBar ratingBar;
        Button btnVerify;

        VH(View item) {
            super(item);
            img = item.findViewById(R.id.l_imgRestaurant);
            txtName = item.findViewById(R.id.l_txtName);
            txtAddress = item.findViewById(R.id.l_txtAddress);
            ratingBar = item.findViewById(R.id.l_ratingBar);
            txtRatingCount = item.findViewById(R.id.l_txtRatingCount);
            btnVerify = item.findViewById(R.id.btnVerify);
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
        Restaurant res = data.get(pos);
        h.txtName.setText(res.name);
        h.txtAddress.setText(res.address);
        h.ratingBar.setRating((float) res.averageRating);

        if (res.reviewCount > 0) {
            h.txtRatingCount.setText("(" + res.reviewCount + ")");
            h.txtRatingCount.setVisibility(View.VISIBLE);
        } else {
            h.txtRatingCount.setVisibility(View.GONE);
        }

        if (res.images != null && !res.images.isEmpty()) {
            String url = res.images.values().iterator().next();
            Glide.with(context).load(url).into(h.img);
        }

        if (res.isVerified) {
            h.btnVerify.setVisibility(View.GONE);
        } else {
            h.btnVerify.setVisibility(View.VISIBLE);
            h.btnVerify.setEnabled(true);

            h.btnVerify.setOnClickListener(v -> {
                int adapterPosition = h.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;

                h.btnVerify.setEnabled(false);

                DatabaseReference resRef = FirebaseDatabase.getInstance().getReference("restaurants");

                resRef.child(res.getKey()).child("isVerified").setValue(true)
                        .addOnSuccessListener(task -> {
                            resRef.child(res.getKey()).child("isVisible").setValue(true)
                                    .addOnSuccessListener(visibleTask -> {
                                        Toast.makeText(context, "Đã xác minh: " + res.name, Toast.LENGTH_SHORT).show();

                                        res.isVerified = true;
                                        res.isVisible = true;

                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            QRCodeHelper.uploadQRCodeIfNeeded(context, res);
                                        });

                                        if (adapterPosition >= 0 && adapterPosition < data.size()) {
                                            data.remove(adapterPosition);
                                            notifyItemRemoved(adapterPosition);
                                        } else {
                                            notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(e -> h.btnVerify.setEnabled(true));
                        })
                        .addOnFailureListener(e -> h.btnVerify.setEnabled(true));
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
}
