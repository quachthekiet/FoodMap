package com.prm392.foodmap.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.prm392.foodmap.utils.ValidationHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddRestaurantActivity extends AppCompatActivity implements OnMapReadyCallback {
    private TextInputEditText etName, etAddress, etPhone;
    private Button btnSelectLocation, btnSubmit;
    private GoogleMap mapPreview;
    private LatLng selectedLatLng;

    private static final int LOCATION_PICKER_REQUEST = 1001;
    public void bindingView(){
        etName = findViewById(R.id.addRes_etName);
        etAddress = findViewById(R.id.addRes_etAddress);
        etPhone = findViewById(R.id.addRes_etPhone);
        btnSelectLocation = findViewById(R.id.addRes_btnSelectLocation);
        btnSubmit = findViewById(R.id.addRes_btnSubmit);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.addRes_mapPreview);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    public void bindingAction(){
        btnSelectLocation.setOnClickListener(this::onBtnSelectLocationClick);
        btnSubmit.setOnClickListener(this::onBtnSubmitClick);

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { validateForm(); }
        };

        etName.addTextChangedListener(watcher);
        etPhone.addTextChangedListener(watcher);
    }


    private void validateForm() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        boolean valid = ValidationHelper.isValidForm(
                name,
                phone,
                selectedLatLng != null ? selectedLatLng.latitude : null,
                selectedLatLng != null ? selectedLatLng.longitude : null
        );

        btnSubmit.setEnabled(valid);
    }

    private void onBtnSubmitClick(View view) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        /*if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để gửi yêu cầu", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return;
        }*/
        String uid = currentUser.getUid();

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        Map<String, String> images = new HashMap<>();
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
        restaurantData.put("isVisible", true);

        long now = System.currentTimeMillis();
        restaurantData.put("createdAt", now);
        restaurantData.put("updatedAt", now);

        DatabaseReference restaurantsRef = FirebaseDatabase.getInstance()
                .getReference("restaurants");
        String newRestaurantId = restaurantsRef.push().getKey();

        if (newRestaurantId != null) {
            restaurantsRef.child(newRestaurantId)
                    .setValue(restaurantData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Gửi yêu cầu thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi gửi yêu cầu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            Toast.makeText(this, "Lỗi tạo ID quán ăn", Toast.LENGTH_LONG).show();
        }
    }


    private void onBtnSelectLocationClick(View view) {
        Intent intent = new Intent(this, LocationPickerActivity.class);
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
    }
}