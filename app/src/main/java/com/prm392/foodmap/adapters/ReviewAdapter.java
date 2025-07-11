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
import com.prm392.foodmap.models.Review;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private Context context;
    private List<Review> reviewList;

    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviewList.get(position);

        holder.ratingBar.setRating(review.getRating());
        holder.tvComment.setText(review.getComment());
        holder.tvTimestamp.setText(review.getFormattedTimestamp());
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RatingBar ratingBar;
        TextView tvComment, tvTimestamp;

        ViewHolder(View itemView) {
            super(itemView);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
