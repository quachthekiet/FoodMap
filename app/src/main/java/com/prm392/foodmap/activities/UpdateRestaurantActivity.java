package com.prm392.foodmap.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.prm392.foodmap.R;
import com.prm392.foodmap.adapters.ImageGalleryAdapter;
import com.prm392.foodmap.adapters.MenuImageAdapter;
import com.prm392.foodmap.config.CloudinaryConfig;
import com.prm392.foodmap.interfaces.DataCallback;
import com.prm392.foodmap.models.Restaurant;
import com.prm392.foodmap.utils.FirebaseHelper;
import com.prm392.foodmap.utils.ValidationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateRestaurantActivity extends AppCompatActivity implements OnMapReadyCallback,
        ImageGalleryAdapter.OnImageRemoveListener, MenuImageAdapter.OnImageRemoveListener {
    private TextInputEditText etName, etAddress, etPhone;
    private Button btnSelectLocation, btnAddImage, btnAddMenuImage, btnUpdate;
    private RecyclerView rvImages, rvMenuImages;
    private GoogleMap mapPreview;
    private LatLng selectedLatLng;

    private ImageGalleryAdapter imageGalleryAdapter;
    private MenuImageAdapter menuImageAdapter;

    private final List<String> imageUrls = new ArrayList<>();
    private final List<String> menuImageUrls = new ArrayList<>();

    private String restaurantKey;
    private Restaurant currentRestaurant;

    private static final int IMAGE_PICKER_REQUEST_CODE = 1001;
    private static final int LOCATION_PICKER_REQUEST_CODE = 1002;
    private boolean pickingMenuImage = false;
    private void bindingView() {
        etName = findViewById(R.id.updateRes_etName);
        etAddress = findViewById(R.id.updateRes_etAddress);
        etPhone = findViewById(R.id.updateRes_etPhone);
        btnSelectLocation = findViewById(R.id.updateRes_btnSelectLocation);
        btnAddImage = findViewById(R.id.updateRes_btnAddImage);
        btnAddMenuImage = findViewById(R.id.updateRes_btnAddMenuImage);
        btnUpdate = findViewById(R.id.updateRes_btnSubmit);
        rvImages = findViewById(R.id.updateRes_rvImages);
        rvMenuImages = findViewById(R.id.updateRes_rvMenuImages);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.updateRes_mapPreview);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        restaurantKey = getIntent().getStringExtra("restaurantKey");
        if (restaurantKey == null) {
            Toast.makeText(this, "Không tìm thấy quán ăn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }
    private void bindingAction() {
        btnSelectLocation.setOnClickListener(this::onSelectLocationClicked);
        btnAddImage.setOnClickListener(this::onAddImageClicked);
        btnAddMenuImage.setOnClickListener(this::onAddMenuImageClicked);
        btnUpdate.setOnClickListener(this::onUpdateClicked);
    }

    // Callback methods
    private void onSelectLocationClicked(View v) {
        Intent intent = new Intent(this, LocationPickerActivity.class);
        if (selectedLatLng != null) {
            intent.putExtra("latitude", selectedLatLng.latitude);
            intent.putExtra("longitude", selectedLatLng.longitude);
        }
        startActivityForResult(intent, LOCATION_PICKER_REQUEST_CODE);
    }

    private void onAddImageClicked(View v) {
        pickingMenuImage = false;
        openImagePicker();
    }

    private void onAddMenuImageClicked(View v) {
        pickingMenuImage = true;
        openImagePicker();
    }

    private void onUpdateClicked(View v) {
        updateRestaurant();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_restaurant);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.updateRes_mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        CloudinaryConfig.setupCloudinary(this);
        bindingView();
        bindingAction();
        setupAdapters();
        loadRestaurantData();
    }

    private void setupAdapters() {
        imageGalleryAdapter = new ImageGalleryAdapter(imageUrls, this);
        rvImages.setLayoutManager( new GridLayoutManager(this, 10, RecyclerView.VERTICAL, false));
        rvImages.setAdapter(imageGalleryAdapter);

        menuImageAdapter = new MenuImageAdapter(menuImageUrls, this);
        rvMenuImages.setLayoutManager( new GridLayoutManager(this, 10, RecyclerView.VERTICAL, false));
        rvMenuImages.setAdapter(menuImageAdapter);
    }

    private void loadRestaurantData() {
        FirebaseHelper.getRestaurantByKey(restaurantKey, new DataCallback<Restaurant>() {
            @Override
            public void onSuccess(Restaurant restaurant) {
                currentRestaurant = restaurant;
                etName.setText(restaurant.name);
                etAddress.setText(restaurant.address);
                etPhone.setText(restaurant.phone);
                selectedLatLng = new LatLng(restaurant.latitude, restaurant.longitude);

                if (mapPreview != null) {
                    mapPreview.clear();
                    mapPreview.addMarker(new MarkerOptions().position(selectedLatLng));
                    mapPreview.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
                }

                imageUrls.clear();
                if (restaurant.images != null) imageUrls.addAll(restaurant.images.values());
                imageGalleryAdapter.notifyDataSetChanged();

                menuImageUrls.clear();
                if (restaurant.menuImages != null) menuImageUrls.addAll(restaurant.menuImages.values());
                menuImageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(UpdateRestaurantActivity.this, "Lỗi tải dữ liệu: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), IMAGE_PICKER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("latitude", 0);
            double lng = data.getDoubleExtra("longitude", 0);
            selectedLatLng = new LatLng(lat, lng);
            etAddress.setText(data.getStringExtra("address"));

            if (mapPreview != null) {
                mapPreview.clear();
                mapPreview.addMarker(new MarkerOptions().position(selectedLatLng));
                mapPreview.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
            }
        }

        if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    uploadImage(imageUri);
                }
            } else if (data.getData() != null) {
                uploadImage(data.getData());
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
                        String imageUrl = (String) resultData.get("secure_url");
                        runOnUiThread(() -> {
                            if (pickingMenuImage) {
                                menuImageUrls.add(imageUrl);
                                menuImageAdapter.notifyItemInserted(menuImageUrls.size() - 1);
                            } else {
                                imageUrls.add(imageUrl);
                                imageGalleryAdapter.notifyItemInserted(imageUrls.size() - 1);
                            }
                        });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> Toast.makeText(UpdateRestaurantActivity.this,
                                "Upload ảnh thất bại: " + error.getDescription(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    @Override
    public void onImageRemove(int position) {
        if (pickingMenuImage && position < menuImageUrls.size()) {
            menuImageUrls.remove(position);
            menuImageAdapter.notifyItemRemoved(position);
        } else if (!pickingMenuImage && position < imageUrls.size()) {
            imageUrls.remove(position);
            imageGalleryAdapter.notifyItemRemoved(position);
        }
    }

    private void updateRestaurant() {
        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (!ValidationHelper.isValidForm(name, phone,
                selectedLatLng != null ? selectedLatLng.latitude : null,
                selectedLatLng != null ? selectedLatLng.longitude : null)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ và đúng thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> imagesMap = new HashMap<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            imagesMap.put("img" + i, imageUrls.get(i));
        }

        Map<String, String> menuImagesMap = new HashMap<>();
        for (int i = 0; i < menuImageUrls.size(); i++) {
            menuImagesMap.put("menu" + i, menuImageUrls.get(i));
        }

        currentRestaurant.name = name;
        currentRestaurant.address = address;
        currentRestaurant.phone = phone;
        currentRestaurant.latitude = selectedLatLng.latitude;
        currentRestaurant.longitude = selectedLatLng.longitude;
        currentRestaurant.images = imagesMap;
        currentRestaurant.menuImages = menuImagesMap;
        currentRestaurant.updateTimestamp();

        FirebaseHelper.updateRestaurant(restaurantKey, currentRestaurant,  new DataCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                runOnUiThread(() -> {
                    Toast.makeText(UpdateRestaurantActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated", true);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(UpdateRestaurantActivity.this, "Lỗi cập nhật: " + errorMessage, Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapPreview = googleMap;
        if (selectedLatLng != null) {
            mapPreview.addMarker(new MarkerOptions().position(selectedLatLng));
            mapPreview.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
        } else {
            LatLng defaultLocation = new LatLng(21.0278, 105.8342); // Hà Nội
            mapPreview.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
        }
    }
}