package com.prm392.foodmap.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.prm392.foodmap.R;
import com.prm392.foodmap.adapters.ImageGalleryAdapter;
import com.prm392.foodmap.config.CloudinaryConfig;
import com.prm392.foodmap.interfaces.DataCallback;
import com.prm392.foodmap.utils.FirebaseHelper;
import com.prm392.foodmap.utils.LocationHelper;
import com.prm392.foodmap.utils.ValidationHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddRestaurantActivity extends AppCompatActivity implements OnMapReadyCallback, ImageGalleryAdapter.OnImageRemoveListener  {
    private TextInputEditText etName, etAddress, etPhone;
    private Button btnSelectLocation, btnSubmit,btnAddImage;
    private RecyclerView rvImages;
    private GoogleMap mapPreview;
    private LatLng selectedLatLng;
    private ImageGalleryAdapter imageGalleryAdapter;
    private final List<String> imageUrls = new ArrayList<>();

    private static final int LOCATION_PICKER_REQUEST = 1001;
    private static final int IMAGE_PICKER_REQUEST_CODE = 1002;
    public void bindingView(){
        etName = findViewById(R.id.addRes_etName);
        etAddress = findViewById(R.id.addRes_etAddress);
        etPhone = findViewById(R.id.addRes_etPhone);
        btnSelectLocation = findViewById(R.id.addRes_btnSelectLocation);
        btnSubmit = findViewById(R.id.addRes_btnSubmit);
        btnAddImage = findViewById(R.id.addRes_btnAddImage);
        rvImages = findViewById(R.id.addRes_rvImages);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.addRes_mapPreview);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    public void bindingAction(){
        btnSelectLocation.setOnClickListener(this::onBtnSelectLocationClick);
        btnSubmit.setOnClickListener(this::onBtnSubmitClick);
        btnAddImage.setOnClickListener(v -> openImagePicker());

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { validateForm(); }
        };

        etName.addTextChangedListener(watcher);
        etPhone.addTextChangedListener(watcher);
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        imageGalleryAdapter = new ImageGalleryAdapter(imageUrls, this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 10, RecyclerView.VERTICAL, false);
        rvImages.setLayoutManager(gridLayoutManager);
        rvImages.setAdapter(imageGalleryAdapter);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), IMAGE_PICKER_REQUEST_CODE);
    }


    private void validateForm() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        boolean valid = ValidationHelper.isValidForm(
                name,
                phone,
                selectedLatLng != null ? selectedLatLng.latitude : null,
                selectedLatLng != null ? selectedLatLng.longitude : null
        ) && !imageUrls.isEmpty();

        btnSubmit.setEnabled(valid);
    }

    private void onBtnSubmitClick(View view) {
        if (!LocationHelper.canUserPinLocation()) {
            Toast.makeText(this, "Vui lòng đăng nhập để gửi yêu cầu", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return;
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();
        if (currentUser != null) {

            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(uid);

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        Log.d("USER_DATA", "User data: " + task.getResult().getValue());
                    } else {
                        Log.d("USER_DATA", "User not found in database.");
                    }
                } else {
                    Log.e("USER_DATA", "Error getting user data", task.getException());
                }
            });
        } else {
            Log.d("USER_DATA", "No user is logged in.");
        }

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        Map<String, String> images = new HashMap<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.put("img" + i, imageUrls.get(i));
        }
        Map<String, String> menuImages = new HashMap<>();

        Map<String, Object> restaurantData = new HashMap<>();
        restaurantData.put("name", name);
        restaurantData.put("phone", phone);
        restaurantData.put("address", address);
        restaurantData.put("latitude", selectedLatLng.latitude);
        restaurantData.put("longitude", selectedLatLng.longitude);
        restaurantData.put("ownerUid", uid);
        restaurantData.put("images", images);
        restaurantData.put("menuImages", menuImages);
        restaurantData.put("isVisible", false);

        long now = System.currentTimeMillis();
        restaurantData.put("createdAt", now);
        restaurantData.put("updatedAt", now);

        DatabaseReference restaurantsRef = FirebaseDatabase.getInstance()
                .getReference("restaurants");
        String newRestaurantId = restaurantsRef.push().getKey();

        if (newRestaurantId != null) {
            FirebaseHelper.addRestaurant(restaurantData, new DataCallback<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Toast.makeText(AddRestaurantActivity.this, "Gửi yêu cầu thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(AddRestaurantActivity.this, "Lỗi gửi yêu cầu: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });

        } else {
            Toast.makeText(this, "Lỗi tạo ID quán ăn", Toast.LENGTH_LONG).show();
        }
    }


    private void onBtnSelectLocationClick(View view) {
        Intent intent = new Intent(this, LocationPickerActivity.class);
        if (selectedLatLng != null) {
            intent.putExtra("latitude", selectedLatLng.latitude);
            intent.putExtra("longitude", selectedLatLng.longitude);
        }
        startActivityForResult(intent, LOCATION_PICKER_REQUEST);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_restaurant);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addRes_mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        CloudinaryConfig.setupCloudinary(this);
        bindingView();
        bindingAction();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapPreview = googleMap;
        LatLng defaultLocation = new LatLng(21.0278, 105.8342);
        mapPreview.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("latitude", 0);
            double lng = data.getDoubleExtra("longitude", 0);
            String address = data.getStringExtra("address");

            selectedLatLng = new LatLng(lat, lng);
            etAddress.setText(address);

            if (mapPreview != null) {
                mapPreview.clear();
                mapPreview.addMarker(new MarkerOptions().position(selectedLatLng));
                mapPreview.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
            }

            validateForm();
        }
        if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    uploadImage(imageUri);
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                uploadImage(imageUri);
            }
        }
    }

    private void uploadImage(Uri imageUri) {
        Map<String, Object> options = new HashMap<>();
        options.put("upload_preset", "foodmap_preset");

        MediaManager.get().upload(imageUri)
                .option("folder", "restaurant_images")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = resultData.get("secure_url").toString();
                        runOnUiThread(() -> {
                            imageUrls.add(imageUrl);
                            imageGalleryAdapter.notifyItemInserted(imageUrls.size() - 1);
                            validateForm();
                        });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() ->
                                Toast.makeText(AddRestaurantActivity.this,
                                        "Upload ảnh thất bại: " + error.getDescription(),
                                        Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    @Override
    public void onImageRemove(int position) {
        if (position >= 0 && position < imageUrls.size()) {
            imageUrls.remove(position);
            imageGalleryAdapter.notifyItemRemoved(position);
            validateForm();
        }
    }
}