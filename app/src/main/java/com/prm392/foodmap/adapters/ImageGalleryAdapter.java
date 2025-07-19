package com.prm392.foodmap.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prm392.foodmap.R;
import com.prm392.foodmap.activities.ImagePreviewActivity;
import com.prm392.foodmap.utils.ImageHelper;

import java.util.List;

public class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.ImageViewHolder> {

    private final List<String> imageUrls;
    private final OnImageRemoveListener removeListener;

    public interface OnImageRemoveListener {
        void onImageRemove(int position,boolean isMenuImage);
    }

    public ImageGalleryAdapter(List<String> imageUrls, OnImageRemoveListener listener) {
        this.imageUrls = imageUrls;
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant_request_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String url = imageUrls.get(position);
        // Dùng ImageHelper để load ảnh
        ImageHelper.loadImage(holder.imageView.getContext(), url, holder.imageView);

        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onImageRemove(position,false);
            }
        });
        holder.imageView.setOnClickListener(v -> {
            Context context = holder.imageView.getContext();
            Intent intent = new Intent(context, ImagePreviewActivity.class);
            intent.putExtra("image_url", url);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton btnRemove;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.itemReqImg_imageView);
            btnRemove = itemView.findViewById(R.id.itemReqImg_btnRemove);
        }
    }
}

