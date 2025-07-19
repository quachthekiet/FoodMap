package com.prm392.foodmap.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prm392.foodmap.R;
import com.prm392.foodmap.models.Restaurant;

import java.util.List;

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.SearchSuggestionViewHolder> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(Restaurant res);
    }

    private Context context;
    private List<Restaurant> restaurantList;

    private OnSuggestionClickListener listener;

    public SearchSuggestionAdapter(Context context, List<Restaurant> restaurantList, OnSuggestionClickListener listener) {
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
    public SearchSuggestionAdapter.SearchSuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_restaurant_suggestion, parent, false);
        return new SearchSuggestionAdapter.SearchSuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchSuggestionAdapter.SearchSuggestionViewHolder holder, int position) {

        Restaurant res = restaurantList.get(position);
        holder.txtName.setText(res.name);
        holder.txtAddress.setText(res.address);
        holder.ratingBar.setRating((float) res.averageRating);
        holder.txtRatingCount.setText(String.format("%d", res.reviewCount));
        holder.tvDistance.setText(String.format("(%.1f km)", res.distance));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSuggestionClick(res);
            }
        });
    }

    @Override
    public int getItemCount() {
        return restaurantList == null ? 0 : restaurantList.size();
    }

    public class SearchSuggestionViewHolder extends RecyclerView.ViewHolder {
        public TextView txtName;
        public TextView txtAddress;
        public TextView txtRatingCount;
        public TextView tvDistance;
        public RatingBar ratingBar;
        public SearchSuggestionViewHolder(@NonNull View view) {
            super(view);
            txtName = view.findViewById(R.id.s_txtName);
            txtAddress = view.findViewById(R.id.s_txtAddress);
            txtRatingCount = view.findViewById(R.id.s_txtRatingCount);
            tvDistance = view.findViewById(R.id.s_tvDistance);
            ratingBar = view.findViewById(R.id.s_ratingBar);
        }
    }
}
