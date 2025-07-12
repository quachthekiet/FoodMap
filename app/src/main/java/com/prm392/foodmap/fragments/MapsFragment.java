package com.prm392.foodmap.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prm392.foodmap.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.prm392.foodmap.activities.RestaurantActivity;
import com.prm392.foodmap.models.Restaurant;
import com.prm392.foodmap.utils.LocationUtil;

import java.util.HashMap;
import java.util.Map;


public class MapsFragment extends Fragment {

    private FloatingActionButton btnMyLocation;
    private boolean isGPSDialogShown = false;
    private boolean userDeniedGPS = false;
    private boolean shouldUpdateCameraFromGPS = false;
    private GoogleMap googleMap;
    private final Map<String, Marker> markerMap = new HashMap<>();

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CHECK_SETTINGS = 1002;

    private FusedLocationProviderClient fusedLocationClient;

    private final OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap gMap) {
            googleMap = gMap;

            googleMap.getUiSettings().setMyLocationButtonEnabled(false);

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
            LatLng cached = LocationUtil.getSavedLocation(requireContext());
            if(cached != null) {
                moveCamera(cached,13f);
            }
            loadRestaurantMarkers();
            requestLocationPermission();

        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
        btnMyLocation = view.findViewById(R.id.btnMyLocation);
        btnMyLocation.setOnClickListener(v -> {
            isGPSDialogShown = false; // ✅ Reset flag
            shouldUpdateCameraFromGPS = true;
            requestLocationPermission(); // Tiếp tục check GPS và quyền
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        // Nếu map đã sẵn sàng, kiểm tra lại quyền + GPS
        if (googleMap != null) {
            requestLocationPermission();
        }
    }

    private void loadRestaurantMarkers() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("restaurants");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot resSnap : snapshot.getChildren()) {
                    Restaurant restaurant = resSnap.getValue(Restaurant.class);
                    Log.d("kiet.debug", "Name: " + restaurant.name + ", Lat: " + restaurant.latitude + ", Lng: " + restaurant.longitude);


                    if (restaurant != null && restaurant.isVisible) {
                        restaurant.setKey(resSnap.getKey()); // Lưu id

                        LatLng position = new LatLng(restaurant.latitude, restaurant.longitude);
                        Marker marker = googleMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(restaurant.name));

                        marker.setTag(restaurant.getKey());
                        markerMap.put(restaurant.getKey(), marker);
                    }
                }

                // Sau khi load xong, xử lý sự kiện click vào marker
                googleMap.setOnMarkerClickListener(marker -> {
                    String resId = (String) marker.getTag();
                    if (resId != null) {
                        Intent intent = new Intent(getContext(), RestaurantActivity.class);
                        intent.putExtra("RESTAURANT_ID", resId);
                        startActivity(intent);
                    }
                    return false;
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi tải nhà hàng: " + error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    public void moveCamera(String restaurantId) {
        if(googleMap != null) {
            Marker marker = markerMap.get(restaurantId);
            if (marker != null) {
                moveCamera(marker.getPosition(), 16f);
                marker.showInfoWindow();
            }
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkGPSAndEnableLocation();
        }
    }

    private void enableUserLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
                getDeviceLocation();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Không thể bật hiển thị vị trí người dùng", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void getDeviceLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                LocationUtil.saveLocation(requireContext(), currentLatLng.latitude, currentLatLng.longitude);
                if (shouldUpdateCameraFromGPS) {
                    moveCamera(currentLatLng); // ✅ chỉ move khi có cờ
                    shouldUpdateCameraFromGPS = false; // reset
                }
            } else {
                // Nếu không lấy được last location, dùng current location
                fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener(newLocation -> {
                            if (newLocation != null) {
                                LatLng currentLatLng = new LatLng(newLocation.getLatitude(), newLocation.getLongitude());
                                if (shouldUpdateCameraFromGPS) {
                                    moveCamera(currentLatLng);
                                    shouldUpdateCameraFromGPS = false;
                                }
                            } else {
                                Toast.makeText(getContext(), "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void checkGPSAndEnableLocation() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        SettingsClient client = LocationServices.getSettingsClient(requireActivity());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> {
            isGPSDialogShown = false;
            userDeniedGPS = false; // ✅ nếu đã bật GPS → reset lại flag
            enableUserLocation();
        });

        task.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException && !isGPSDialogShown) {
                try {
                    isGPSDialogShown = true;
                    userDeniedGPS = false; // sẽ reset nếu đồng ý
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    sendEx.printStackTrace();
                }
            } else {
//                if (!userDeniedGPS) {
//                    Toast.makeText(getContext(), "Không thể bật GPS", Toast.LENGTH_SHORT).show();
//                }

            }
        });
    }

    public void moveCamera(LatLng latLng, float zoom) {
        if (googleMap != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        } else {
            Toast.makeText(getContext(), "Bản đồ chưa sẵn sàng", Toast.LENGTH_SHORT).show();
        }
    }



    public void moveCamera(LatLng latLng) {
        if (googleMap != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
        } else {
            Toast.makeText(getContext(), "Bản đồ chưa sẵn sàng", Toast.LENGTH_SHORT).show();
        }
    }

    // Khi người dùng bật/tắt GPS từ dialog
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            isGPSDialogShown = false; // ✅ Luôn reset sau khi dialog đóng
            if (resultCode == Activity.RESULT_OK) {
                enableUserLocation();
            } else {
                Toast.makeText(getContext(), "GPS chưa được bật", Toast.LENGTH_SHORT).show();
            }
        }
    }




    // Xử lý kết quả cấp quyền từ requestPermission()
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkGPSAndEnableLocation();
            } else {
                Toast.makeText(getContext(), "Bạn cần cấp quyền vị trí để sử dụng chức năng này", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
