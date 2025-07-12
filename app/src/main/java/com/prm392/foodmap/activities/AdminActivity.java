package com.prm392.foodmap.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;

import com.prm392.foodmap.R;
import com.prm392.foodmap.adapters.AdminRestaurantAdapter;
import com.prm392.foodmap.adapters.RestaurantAdapter;
import com.prm392.foodmap.models.Restaurant;

import java.util.*;

public class AdminActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "AdminActivity";
    private GoogleMap mMap;
    private RecyclerView recyclerView;
    private AdminRestaurantAdapter restaurantAdapter;
    private List<Restaurant> restaurantList;
    private DatabaseReference restaurantsRef;
    private Map<String, Marker> markerMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Hiện nút back trên AppBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý nhà hàng");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // RecyclerView
        recyclerView = findViewById(R.id.recyclerViewRestaurants);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        restaurantList = new ArrayList<>();
        markerMap = new HashMap<>();

        restaurantAdapter = new AdminRestaurantAdapter(
                this,
                restaurantList,
                R.layout.item_pin_restaurant_admin,
                this::moveToRestaurantLocation
        );
        recyclerView.setAdapter(restaurantAdapter);

        // Firebase
        restaurantsRef = FirebaseDatabase.getInstance(
                "https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference("restaurants");

        loadRestaurants();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng defaultLocation = new LatLng(21.0285, 105.8542); // Hà Nội
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
    }

    private void loadRestaurants() {
        restaurantsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                restaurantList.clear();
                if (mMap != null) {
                    mMap.clear();
                    markerMap.clear();
                }

                for (DataSnapshot data : snapshot.getChildren()) {
                    Restaurant res = data.getValue(Restaurant.class);
                    if (res != null) {
                        res.setKey(data.getKey());
                        restaurantList.add(res);

                        if (res.isVisible && mMap != null) {
                            LatLng location = new LatLng(res.latitude, res.longitude);
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(location)
                                    .title(res.name)
                                    .snippet(res.address));
                            markerMap.put(res.getKey(), marker);
                        }
                    }
                }

                restaurantAdapter.setRestaurantList(restaurantList);
                Log.d(TAG, "Đã tải " + restaurantList.size() + " nhà hàng");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lỗi tải dữ liệu: " + error.getMessage(), error.toException());
                Toast.makeText(AdminActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void moveToRestaurantLocation(Restaurant res) {
        if (mMap != null) {
            LatLng location = new LatLng(res.latitude, res.longitude);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));

            Marker marker = markerMap.get(res.getKey());
            if (marker != null) marker.showInfoWindow();

            Log.d(TAG, "Đã chuyển tới: " + res.name);
        }
    }
}
