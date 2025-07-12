package com.prm392.foodmap.adapters;

import android.content.Context;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import com.prm392.foodmap.R;
import com.prm392.foodmap.models.Restaurant;

import java.util.List;

public class AdminRestaurantAdapter extends RecyclerView.Adapter<AdminRestaurantAdapter.VH> {

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    private final Context context;
    private List<Restaurant> data;
    private final int layoutResId;
    private final OnRestaurantClickListener clickListener;

    public AdminRestaurantAdapter(Context ctx,
                                  List<Restaurant> list,
                                  int layoutResId,
                                  OnRestaurantClickListener listener) {
        this.context = ctx;
        this.data = list;
        this.layoutResId = layoutResId;
        this.clickListener = listener;
    }

    public void setRestaurantList(List<Restaurant> newList) {
        data = newList;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtName, txtAddress;
        ImageView img;
        Switch swVisible;
        Button btnVerify;  // ✅ Button thay vì Switch

        VH(View item) {
            super(item);
            txtName    = item.findViewById(R.id.l_txtName);
            txtAddress = item.findViewById(R.id.l_txtAddress);
            img        = item.findViewById(R.id.l_imgRestaurant);
            swVisible  = item.findViewById(R.id.switchVisible);
            btnVerify  = item.findViewById(R.id.btnVerify);
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

        // Load ảnh
        if (res.images != null && !res.images.isEmpty()) {
            String url = res.images.values().iterator().next();
            Glide.with(context).load(url).into(h.img);
        }

        // Xử lý visible (nếu có)
        if (h.swVisible != null) {
            h.swVisible.setOnCheckedChangeListener(null);
            h.swVisible.setChecked(res.isVisible);
            h.swVisible.setOnCheckedChangeListener((b, checked) -> {
                res.isVisible = checked;
                FirebaseDatabase.getInstance()
                        .getReference("restaurants")
                        .child(res.getKey())
                        .child("isVisible")
                        .setValue(checked);
            });
        }

        // ✅ Xác minh nhà hàng
        if (h.btnVerify != null) {
            // Ẩn nút nếu đã xác minh
            if (res.isVerified) {
                h.btnVerify.setVisibility(View.GONE);
            } else {
                h.btnVerify.setVisibility(View.VISIBLE);
                h.btnVerify.setOnClickListener(v -> {
                    FirebaseDatabase.getInstance()
                            .getReference("restaurants")
                            .child(res.getKey())
                            .child("isVerified")
                            .setValue(true)
                            .addOnSuccessListener(task -> {
                                Toast.makeText(context, "Đã xác minh " + res.name, Toast.LENGTH_SHORT).show();
                                res.isVerified = true;
                                notifyItemChanged(h.getAdapterPosition()); // cập nhật giao diện
                            });
                });
            }
        }

        // Click item → zoom map
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onRestaurantClick(res);
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }
}
