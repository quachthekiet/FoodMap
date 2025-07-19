package com.prm392.foodmap.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.prm392.foodmap.interfaces.DataCallback;
import com.prm392.foodmap.models.Review;
import com.prm392.foodmap.utils.FirebaseHelper;
import com.prm392.foodmap.utils.LocationHelper;

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
    private Button btnReview,btnUpdate, btnCheckIn;
    private TextView edtReview;
    private RatingBar ratingBarInput;
    private ImageButton btnFavorite;

    private LinearLayout layoutRatingForm;
    private boolean isFavorite = false;
    private String userId;

    private double restaurantLat;

    private double restaurantLng;
    private FirebaseUser user;
    private FirebaseAuth mAuth;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "CHECKIN_PREFS";

    private static final int REQUEST_CODE_QR = 999;

    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private void openQRScanner() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "Thiết bị không có camera để quét mã QR", Toast.LENGTH_LONG).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            startQRScanner();
        }
    }

    private void startQRScanner() {
        Intent intent = new Intent(this, QRScanActivity.class);
        startActivityForResult(intent, REQUEST_CODE_QR);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_QR && resultCode == RESULT_OK && data != null) {
            String scannedId = data.getStringExtra("restaurantId");
            checkInWithQRIfValid(scannedId);
        }
    }

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
        btnUpdate = findViewById(R.id.detail_btnUpdate);
        btnUpdate.setVisibility(View.GONE);
        btnCheckIn = findViewById(R.id.btnCheckIn);
        layoutRatingForm = findViewById(R.id.layoutRatingForm);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            String key = getCheckInKey(userId, restaurantId);
            boolean isCheckedIn = sharedPreferences.getBoolean(key, false);
            if (isCheckedIn) {
                showRatingForm();
            } else {
                checkCheckInOnFirebase(userId, restaurantId);
            }
        } else {
            userId = null;
            showCheckInPrompt();
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
        btnCheckIn.setOnClickListener(v -> {
            if (userId == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để check-in và đánh giá!", Toast.LENGTH_SHORT).show();
            } else {
                openQRScanner();
            }
        });
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
        ratingBarInput.setRating(5);
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
                        Toast.makeText(this, "Không thể review!", Toast.LENGTH_SHORT).show();
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
                String ownerUid = snapshot.child("ownerUid").getValue(String.class);
                restaurantLat = snapshot.child("latitude").getValue(Double.class);
                restaurantLng = snapshot.child("longitude").getValue(Double.class);
                if (ownerUid != null && userId != null && ownerUid.equals(userId)) {
                    btnUpdate.setVisibility(View.VISIBLE);
                    btnUpdate.setOnClickListener(v -> {
                        Intent intent = new Intent(RestaurantActivity.this, UpdateRestaurantActivity.class);
                        intent.putExtra("restaurantKey", restaurantId);
                        startActivity(intent);
                    });
                } else {
                    btnUpdate.setVisibility(View.GONE);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RestaurantActivity.this, "Failed to load details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadImagesFromFirebase() {
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(restaurantId);

        // Clear the existing imageUrls list
        imageUrls.clear();

        // Fetch images from the 'images' node
        restaurantRef.child("images").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String url = child.getValue(String.class);
                    if (url != null) {
                        imageUrls.add(url);
                    }
                }
                // Notify adapter after both images and menuImages are fetched
                fetchMenuImages();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RestaurantActivity.this, "Failed to load images", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMenuImages() {
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(restaurantId);

        // Fetch images from the 'menuImages' node
        restaurantRef.child("menuImages").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String url = child.getValue(String.class);
                    if (url != null) {
                        imageUrls.add(url);
                    }
                }
                // Notify adapter after all images are fetched
                imageSliderAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RestaurantActivity.this, "Failed to load menu images", Toast.LENGTH_SHORT).show();
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
    private void showCheckInPrompt() {
        layoutRatingForm.setVisibility(View.GONE);
        btnCheckIn.setVisibility(View.VISIBLE);
    }

    private void showRatingForm() {
        layoutRatingForm.setVisibility(View.VISIBLE);
        btnCheckIn.setVisibility(View.GONE);
    }
    private void checkInWithQR(String restaurantId) {
        FirebaseHelper.checkInUser(restaurantId, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        String key = getCheckInKey(userId, restaurantId);
                        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                        prefs.edit().putBoolean(key, true).apply();
                    }

                    Toast.makeText(RestaurantActivity.this, "Check-in thành công!", Toast.LENGTH_SHORT).show();
                    showRatingForm();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(RestaurantActivity.this, "Check-in thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void checkCheckInOnFirebase(String userId, String restaurantId) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("checkins")
                .child(userId)
                .child(restaurantId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isCheckedInRemote = snapshot.getValue(Boolean.class);

                if (Boolean.TRUE.equals(isCheckedInRemote)) {
                    String key = getCheckInKey(userId, restaurantId);
                    sharedPreferences.edit().putBoolean(key, true).apply();
                    showRatingForm();
                } else {
                    showCheckInPrompt();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CHECKIN", "Firebase check failed: " + error.getMessage());
                showCheckInPrompt();
            }
        });
    }
    private String getCheckInKey(String userId, String restaurantId) {
        return "checkin_" + userId + "_" + restaurantId;
    }
    private void checkInWithQRIfValid(String scannedId) {
        if (scannedId == null || !scannedId.equals(restaurantId)) {
            Toast.makeText(this, "Sai mã QR. Vui lòng quét đúng mã của quán này!", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationHelper.checkGPSDistance(this, restaurantLat, restaurantLng, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isNearby) {
                if (isNearby) {
                    checkInWithQR(restaurantId);
                } else {
                    Toast.makeText(RestaurantActivity.this, "Bạn chưa ở gần nhà hàng. Đến gần hơn để check-in!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(RestaurantActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
