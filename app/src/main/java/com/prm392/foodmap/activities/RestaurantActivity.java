package com.prm392.foodmap.activities;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prm392.foodmap.R;
import com.prm392.foodmap.adapters.ImageSliderAdapter;
import com.prm392.foodmap.adapters.ReviewAdapter;
import com.prm392.foodmap.models.Review;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestaurantActivity extends AppCompatActivity {
    private TextView tvName, tvAddress, tvPhone;
    private ViewPager2 viewPagerImages;
    private RecyclerView recyclerReviews;
    private List<String> imageUrls;
    private ImageSliderAdapter imageSliderAdapter;
    private List<Review> reviewList;
    private ReviewAdapter reviewAdapter;
    private String restaurantId;
    private Button btnReview;
    private TextView edtReview;
    private RatingBar ratingBarInput;
    private ImageButton btnFavorite;
    private boolean isFavorite = false;
    private String userId;
    private FirebaseUser user;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant);
        mAuth = FirebaseAuth.getInstance();
        bindingView();
        bindingAction();
    }

    private void bindingView() {
        restaurantId = getIntent().getStringExtra("RESTAURANT_ID");

        tvName = findViewById(R.id.tvName);
        tvAddress = findViewById(R.id.tvAddress);
        tvPhone = findViewById(R.id.tvPhone);
        btnReview = findViewById(R.id.btnReview);
        edtReview = findViewById(R.id.edtReview);
        ratingBarInput = findViewById(R.id.ratingBarInput);

        viewPagerImages = findViewById(R.id.viewPagerImages);
        imageUrls = new ArrayList<>();
        imageSliderAdapter = new ImageSliderAdapter(this, imageUrls);
        viewPagerImages.setAdapter(imageSliderAdapter);

        recyclerReviews = findViewById(R.id.recyclerReviews);
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        recyclerReviews.setLayoutManager(new LinearLayoutManager(this));
        recyclerReviews.setAdapter(reviewAdapter);

        btnFavorite = findViewById(R.id.btnFavorite);

        user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            userId = null;
        }


    }

    private void bindingAction() {
        loadRestaurantDetail();
        if (userId != null) {
            checkFavoriteState();
            btnFavorite.setOnClickListener(v -> toggleFavorite());
        } else {
            btnFavorite.setOnClickListener(v -> {
                Toast.makeText(this, "Vui lòng đăng nhập để sử dụng chức năng yêu thích.", Toast.LENGTH_SHORT).show();
            });
        }


        loadImagesFromFirebase();
        loadReviews();
        btnReview.setOnClickListener(this::onReviewClick);
    }

    private void checkFavoriteState() {
        DatabaseReference favRef = FirebaseDatabase.getInstance()
                .getReference("favorites")
                .child(userId)
                .child(restaurantId);

        favRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavorite = snapshot.exists();
                updateFavoriteIcon();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RestaurantActivity.this, "Lỗi khi kiểm tra yêu thích", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateFavoriteIcon() {
        if (isFavorite) {
            btnFavorite.setImageResource(R.drawable.ic_heart_filled);
        } else {
            btnFavorite.setImageResource(R.drawable.ic_heart_outline);
        }
    }
    private void toggleFavorite() {
        DatabaseReference favRef = FirebaseDatabase.getInstance()
                .getReference("favorites")
                .child(userId)
                .child(restaurantId);

        if (isFavorite) {
            // Bỏ yêu thích
            favRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    isFavorite = false;
                    updateFavoriteIcon();
                    Toast.makeText(this, "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Thêm yêu thích
            favRef.setValue(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    isFavorite = true;
                    updateFavoriteIcon();
                    Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void onReviewClick(View view) {
        int rating = (int) ratingBarInput.getRating();
        String comment = edtReview.getText().toString().trim();

        if (rating < 1 || comment.isEmpty()) {
            Toast.makeText(this, "Hãy nhập đủ rating và bình luận!", Toast.LENGTH_SHORT).show();
            return;
        }

        saveReviewToFirebase(rating, comment);

        edtReview.setText("");
        ratingBarInput.setRating(0);
    }

    private void saveReviewToFirebase(int rating, String comment) {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        DatabaseReference reviewRef = FirebaseDatabase.getInstance()
                .getReference("reviews")
                .child(restaurantId)
                .child(deviceId);

        long timestamp = System.currentTimeMillis();

        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("rating", rating);
        reviewData.put("comment", comment);
        reviewData.put("timestamp", timestamp);

        reviewRef.setValue(reviewData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Review đã được gửi!", Toast.LENGTH_SHORT).show();
                        loadReviewsAndScrollToBottom();

                    } else {
                        Toast.makeText(this, "Lỗi khi gửi review", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadReviewsAndScrollToBottom() {
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance()
                .getReference("reviews")
                .child(restaurantId);

        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();

                for (DataSnapshot deviceSnapshot : snapshot.getChildren()) {
                    Integer rating = deviceSnapshot.child("rating").getValue(Integer.class);
                    String comment = deviceSnapshot.child("comment").getValue(String.class);
                    Long timestamp = deviceSnapshot.child("timestamp").getValue(Long.class);

                    if (rating != null && comment != null && timestamp != null) {
                        reviewList.add(new Review(rating, comment, timestamp));
                    }
                }

                reviewAdapter.notifyDataSetChanged();

                if (!reviewList.isEmpty()) {
                    recyclerReviews.scrollToPosition(reviewList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RestaurantActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void loadRestaurantDetail() {
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(restaurantId);

        restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String address = snapshot.child("address").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);

                tvName.setText(name != null ? name : "N/A");
                tvAddress.setText(address != null ? address : "N/A");
                tvPhone.setText(phone != null ? phone : "N/A");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RestaurantActivity.this, "Failed to load details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadImagesFromFirebase() {
        DatabaseReference imagesRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(restaurantId)
                .child("images");

        imagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                imageUrls.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String url = child.getValue(String.class);
                    if (url != null) {
                        imageUrls.add(url);
                    }
                }
                imageSliderAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RestaurantActivity.this, "Failed to load images", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReviews() {
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance()
                .getReference("reviews")
                .child(restaurantId);

        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();
                for (DataSnapshot deviceSnapshot : snapshot.getChildren()) {
                    Integer rating = deviceSnapshot.child("rating").getValue(Integer.class);
                    String comment = deviceSnapshot.child("comment").getValue(String.class);
                    Long timestamp = deviceSnapshot.child("timestamp").getValue(Long.class);

                    if (rating != null && comment != null && timestamp != null) {
                        reviewList.add(new Review(rating, comment, timestamp));
                    } else {
                        Log.e("REVIEW_LOAD", "Review missing field at deviceId: " + deviceSnapshot.getKey());
                    }
                }
                reviewAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RestaurantActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
