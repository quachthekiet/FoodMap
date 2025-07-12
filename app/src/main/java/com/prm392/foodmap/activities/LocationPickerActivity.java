package com.prm392.foodmap.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.prm392.foodmap.R;
import com.prm392.foodmap.utils.LocationHelper;
import com.prm392.foodmap.utils.PermissionHelper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView pickLoc_tvAddress;
    private Button pickLoc_btnConfirm;
    private LatLng selectedLatLng;

    private LatLng initialLatLng = null;

    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location_picker);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindingView();
        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lng = getIntent().getDoubleExtra("longitude", 0);
        if (lat != 0 && lng != 0) {
            initialLatLng = new LatLng(lat, lng);
        }

        bindingAction();

        executorService = Executors.newSingleThreadExecutor();
        initMap();
    }

    private void bindingView() {
        pickLoc_tvAddress = findViewById(R.id.pickLoc_tvAddress);
        pickLoc_btnConfirm = findViewById(R.id.pickLoc_btnConfirm);
        pickLoc_btnConfirm.setEnabled(false);
    }

    private void bindingAction() {
        pickLoc_btnConfirm.setOnClickListener(this::onBtnConfirmClick);
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.pickLoc_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void onBtnConfirmClick(View view) {
        if (selectedLatLng == null) return;

        Intent result = new Intent();
        result.putExtra("latitude", selectedLatLng.latitude);
        result.putExtra("longitude", selectedLatLng.longitude);
        result.putExtra("address", pickLoc_tvAddress.getText().toString());
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (PermissionHelper.hasFineLocationPermission(this)) {
            enableMyLocationAndMoveCamera();
        } else {
            PermissionHelper.requestFineLocationPermission(this);
        }

        mMap.setOnCameraIdleListener(() -> {
            selectedLatLng = mMap.getCameraPosition().target;
            updateAddress(selectedLatLng);
        });
    }
    @SuppressLint("MissingPermission")
    private void enableMyLocationAndMoveCamera() {
        if (!PermissionHelper.hasFineLocationPermission(this)) return;

        mMap.setMyLocationEnabled(true);
        if (initialLatLng != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, 15));
            selectedLatLng = initialLatLng;
            updateAddress(initialLatLng);
        } else {
            LocationHelper.getLastKnownLocation(this, location -> {
                LatLng target = (location != null)
                        ? new LatLng(location.getLatitude(), location.getLongitude())
                        : new LatLng(21.0278, 105.8342); // default Hanoi
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 15));
                selectedLatLng = target;
                updateAddress(target);
            });
        }
    }

    private void updateAddress(LatLng latLng) {
        pickLoc_btnConfirm.setEnabled(false);
        pickLoc_tvAddress.setText("Đang tải địa chỉ...");

        executorService.execute(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        pickLoc_tvAddress.setText(addresses.get(0).getAddressLine(0));
                        pickLoc_btnConfirm.setEnabled(true);
                    } else {
                        pickLoc_tvAddress.setText("Không xác định được địa chỉ");
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> pickLoc_tvAddress.setText("Lỗi lấy địa chỉ"));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionHelper.handleLocationPermissionResult( requestCode, grantResults)) {
            enableMyLocationAndMoveCamera();
        } else {
            Toast.makeText(this, "Quyền vị trí bị từ chối", Toast.LENGTH_SHORT).show();
            LatLng defaultLocation = new LatLng(21.0278, 105.8342);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
            updateAddress(defaultLocation);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}
