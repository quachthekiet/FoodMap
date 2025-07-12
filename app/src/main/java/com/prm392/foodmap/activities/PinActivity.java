package com.prm392.foodmap.activities;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prm392.foodmap.R;
import com.prm392.foodmap.adapters.AdminAdapter;
import com.prm392.foodmap.models.Restaurant;
import com.prm392.foodmap.utils.LocationUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PinActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "NearbyListActivity";
    private RecyclerView recyclerNearby;
    private Spinner spinnerRadius;
    private AdminAdapter adapter;
    private GoogleMap mMap;
    private Map<String, Marker> markerMap;

    private List<Restaurant> allRestaurants = new ArrayList<>();
    private List<Restaurant> filteredList = new ArrayList<>();

    private DatabaseReference resRef = FirebaseDatabase.getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("restaurants");
    private DatabaseReference reviewRef = FirebaseDatabase.getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("reviews");
    private DatabaseReference usersRef = FirebaseDatabase.getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");

    private int totalRestaurants = 0;
    private int loadedRestaurants = 0;

    private Location currentUserLocation;
    private boolean isAdmin = false;

    private void bindViews() {
        recyclerNearby = findViewById(R.id.l_recyclerNearby);
        spinnerRadius = findViewById(R.id.l_spinnerRadius);

        // Kiểm tra vai trò admin
        checkUserRole();

        // Gắn adapter với danh sách được lọc
        adapter = new AdminAdapter(
                this,
                filteredList,
                R.layout.item_restaurant,
                this::moveToRestaurantLocation,
                this::updateMapMarkers // thêm callback cập nhật map
        );
        recyclerNearby.setLayoutManager(new LinearLayoutManager(this));
        recyclerNearby.setAdapter(adapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nearby_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        bindViews();
        loadCurrentLocation();
        setupRadiusFilter();
        fetchRestaurantsFromFirebase();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        markerMap = new HashMap<>();
        // Di chuyển camera đến vị trí mặc định (ví dụ: trung tâm Hà Nội)
        LatLng defaultLocation = new LatLng(21.0285, 105.8542);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
        // Cập nhật marker sau khi bản đồ sẵn sàng
        updateMapMarkers();
    }

    private void checkUserRole() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef.child(uid).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);
                isAdmin = "admin".equals(role);
                adapter.notifyDataSetChanged(); // Cập nhật adapter để hiển thị/ẩn Switch
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "checkUserRole: Lỗi kiểm tra vai trò: " + error.getMessage());
                Toast.makeText(PinActivity.this, "Lỗi kiểm tra vai trò", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCurrentLocation() {
        LatLng latLng = LocationUtil.getSavedLocation(this);
        if (latLng != null) {
            currentUserLocation = new Location("custom");
            currentUserLocation.setLatitude(latLng.latitude);
            currentUserLocation.setLongitude(latLng.longitude);
        }
    }

    private void setupRadiusFilter() {
        spinnerRadius.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterNearbyRestaurants();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void fetchRestaurantsFromFirebase() {
        allRestaurants.clear();
        try {
            resRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    totalRestaurants = 0;
                    loadedRestaurants = 0;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Restaurant res = child.getValue(Restaurant.class);
                        if (res != null && res.isVisible) {
                            res.setKey(child.getKey());
                            totalRestaurants++;

                            reviewRef.child(res.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot reviewSnap) {
                                    float totalRating = 0;
                                    int count = 0;

                                    for (DataSnapshot review : reviewSnap.getChildren()) {
                                        Long rating = review.child("rating").getValue(Long.class);
                                        if (rating != null) {
                                            totalRating += rating;
                                            count++;
                                        }
                                    }

                                    res.averageRating = (count > 0) ? totalRating / count : 0;
                                    res.reviewCount = count;

                                    allRestaurants.add(res);
                                    loadedRestaurants++;

                                    if (loadedRestaurants == totalRestaurants) {
                                        filterNearbyRestaurants();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    loadedRestaurants++;
                                    if (loadedRestaurants == totalRestaurants) {
                                        filterNearbyRestaurants();
                                    }
                                }
                            });
                        }
                    }

                    if (totalRestaurants == 0) {
                        filteredList.clear();
                        adapter.notifyDataSetChanged();
                        updateMapMarkers();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(PinActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void filterNearbyRestaurants() {
        if (currentUserLocation == null) return;

        String radiusText = spinnerRadius.getSelectedItem().toString();
        int radiusKm;
        try {
            radiusKm = Integer.parseInt(radiusText);
        } catch (NumberFormatException e) {
            radiusKm = 5; // fallback
        }

        filteredList.clear();

        for (Restaurant res : allRestaurants) {
            Location resLoc = new Location("firebase");
            resLoc.setLatitude(res.latitude);
            resLoc.setLongitude(res.longitude);

            float distanceMeters = currentUserLocation.distanceTo(resLoc);
            if (distanceMeters <= radiusKm * 1000) {
                filteredList.add(res);
            }
        }

        adapter.notifyDataSetChanged();
        updateMapMarkers();
    }

    private void updateMapMarkers() {
        if (mMap == null) return;
        mMap.clear();
        markerMap.clear();
        for (Restaurant res : filteredList) {
            if (res.isVisible) {
                LatLng location = new LatLng(res.latitude, res.longitude);
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(location)
                        .title(res.name)
                        .snippet(res.address));
                markerMap.put(res.getKey(), marker);
            }
        }
    }

    private void moveToRestaurantLocation(Restaurant restaurant) {
        LatLng location = new LatLng(restaurant.latitude, restaurant.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        Marker marker = markerMap.get(restaurant.getKey());
        if (marker != null) {
            marker.showInfoWindow();
        }
        Log.d(TAG, "moveToRestaurantLocation: Di chuyển camera đến " + restaurant.name);
    }

    public boolean isAdmin() {
        return isAdmin;
    }
}
