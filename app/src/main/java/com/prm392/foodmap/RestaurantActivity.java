package com.prm392.foodmap;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prm392.foodmap.adapters.ImageSliderAdapter;
import com.prm392.foodmap.adapters.ReviewAdapter;
import com.prm392.foodmap.models.Review;

import java.util.ArrayList;
import java.util.List;

public class RestaurantActivity extends AppCompatActivity {

    private TextView tvName, tvAddress, tvPhone;
    private ViewPager2 viewPagerImages;
    private RecyclerView recyclerReviews;

    private List<String> imageUrls;
    private ImageSliderAdapter imageSliderAdapter;

    private List<Review> reviewList;
    private ReviewAdapter reviewAdapter;

    private String restaurantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant);

        bindingView();
        bindingAction();
    }

    private void bindingView() {
        restaurantId = getIntent().getStringExtra("RESTAURANT_ID");

        tvName = findViewById(R.id.tvName);
        tvAddress = findViewById(R.id.tvAddress);
        tvPhone = findViewById(R.id.tvPhone);

        viewPagerImages = findViewById(R.id.viewPagerImages);
        imageUrls = new ArrayList<>();
        imageSliderAdapter = new ImageSliderAdapter(this, imageUrls);
        viewPagerImages.setAdapter(imageSliderAdapter);

        recyclerReviews = findViewById(R.id.recyclerReviews);
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        recyclerReviews.setLayoutManager(new LinearLayoutManager(this));
        recyclerReviews.setAdapter(reviewAdapter);
    }

    private void bindingAction() {
        loadRestaurantDetail();
        loadImagesFromFirebase();
        loadReviews();
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
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");

        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String resId = child.child("restaurant_id").getValue(String.class);
                    if (restaurantId.equals(resId)) {
                        Integer rating = child.child("rating").getValue(Integer.class);
                        String comment = child.child("comment").getValue(String.class);
                        Long timestamp = child.child("timestamp").getValue(Long.class);

                        if (rating != null && comment != null && timestamp != null) {
                            reviewList.add(new Review(rating, comment, timestamp));
                        }
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
