package com.prm392.foodmap.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;

import com.prm392.foodmap.R;
import com.prm392.foodmap.adapters.AdminAdapter;
import com.prm392.foodmap.adapters.AdminRestaurantAdapter;
import com.prm392.foodmap.models.Restaurant;

import java.util.*;

public class AdminActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "AdminActivity";

    // Map -------------------------------------------------------------
    private GoogleMap mMap;
    private final Map<String, Marker> markerMap = new HashMap<>();

    // RecyclerView ----------------------------------------------------
    private RecyclerView recycler;
    private final List<Restaurant> listAll = new ArrayList<>();
    private final List<Restaurant> listUnverified = new ArrayList<>();
    private AdminAdapter viewAdapter;           // Layout: quản lý chung
    private AdminRestaurantAdapter verifyAdapter; // Layout: xác minh pin

    // Firebase --------------------------------------------------------
    private final DatabaseReference resRef = FirebaseDatabase
            .getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("restaurants");

    // -----------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // AppBar back
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý nhà hàng");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Map
        SupportMapFragment mapFr = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Objects.requireNonNull(mapFr).getMapAsync(this);

        // Recycler
        recycler = findViewById(R.id.recyclerViewRestaurants);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // Adapter: quản lý chung
        viewAdapter = new AdminAdapter(
                this, listAll,
                R.layout.item_pin_restaurant_admin,
                this::moveCameraTo,
                this::drawVisibleRestaurantsOnMap
        );

        // Adapter: xác minh pin
        verifyAdapter = new AdminRestaurantAdapter(
                this, listUnverified,
                R.layout.item_verify_pin_restaurant_admin,
                this::moveCameraTo
        );

        // Mặc định vào “quản lý chung”
        recycler.setAdapter(viewAdapter);
        loadAllRestaurants();
    }

    // AppBar Back
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // MENU ⋮
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.mn_all) {
            switchToMode(true);  // vào chế độ “tất cả”
        } else if (item.getItemId() == R.id.mn_verify) {
            switchToMode(false); // vào chế độ “chờ xác minh”
        }
        return super.onOptionsItemSelected(item);
    }


    // Firebase: load danh sách
    private void loadAllRestaurants() {
        resRef.addListenerForSingleValueEvent(buildListener(false));
    }

    private void loadUnverifiedRestaurants() {
        resRef.orderByChild("isVerified")
                .equalTo(false)
                .addListenerForSingleValueEvent(buildListener(true));
    }

    private void switchToMode(boolean showVerified) {
        if (showVerified) {
            recycler.setAdapter(viewAdapter);
            loadAllRestaurants();
        } else {
            recycler.setAdapter(verifyAdapter);
            loadUnverifiedRestaurants();
        }
    }

    private ValueEventListener buildListener(boolean onlyUnverified) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                clearMap();
                if (onlyUnverified) {
                    listUnverified.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Restaurant r = child.getValue(Restaurant.class);
                        if (r == null) continue;
                        r.setKey(child.getKey());
                        listUnverified.add(r);
                        addMarkerIfVisible(r);
                    }
                    verifyAdapter.notifyDataSetChanged();
                } else {
                    listAll.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Restaurant r = child.getValue(Restaurant.class);
                        if (r == null) continue;
                        r.setKey(child.getKey());
                        listAll.add(r);
                        addMarkerIfVisible(r);
                    }
                    viewAdapter.notifyDataSetChanged();
                }
                Log.d(TAG, "Loaded: " + snapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Lỗi tải: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
    }

    // Map
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(21.0285, 105.8542), 10));
    }

    private void addMarkerIfVisible(Restaurant r) {
        if (!r.isVisible || mMap == null) return;
        LatLng loc = new LatLng(r.latitude, r.longitude);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(loc).title(r.name).snippet(r.address));
        markerMap.put(r.getKey(), marker);
    }

    private void clearMap() {
        if (mMap != null) mMap.clear();
        markerMap.clear();
    }

    private void moveCameraTo(Restaurant r) {
        if (mMap == null) return;
        LatLng loc = new LatLng(r.latitude, r.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));
        Marker marker = markerMap.get(r.getKey());
        if (marker != null) marker.showInfoWindow();
    }

    private void drawVisibleRestaurantsOnMap() {
        if (mMap != null) {
            mMap.clear();
            for (Restaurant r : listAll) {
                if (r.isVisible) {
                    LatLng loc = new LatLng(r.latitude, r.longitude);
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(loc).title(r.name).snippet(r.address));
                    markerMap.put(r.getKey(), m);
                }
            }
        }
    }
}
