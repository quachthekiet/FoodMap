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
import com.prm392.foodmap.adapters.RestaurantAdapter;
import com.prm392.foodmap.models.Restaurant;

import java.util.*;

public class AdminActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "AdminActivity";

    // Map -------------------------------------------------------------
    private GoogleMap mMap;
    private Map<String, Marker> markerMap = new HashMap<>();

    // RecyclerView ----------------------------------------------------
    private RecyclerView recycler;
    private final List<Restaurant> data = new ArrayList<>();
    private AdminAdapter viewAdapter;   // quản lý chung
    private AdminRestaurantAdapter   verifyAdapter; // xác minh pin

    // Firebase --------------------------------------------------------
    private final DatabaseReference resRef = FirebaseDatabase
            .getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("restaurants");

    // -----------------------------------------------------------------
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
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

        viewAdapter = new AdminAdapter(
                this, data, R.layout.item_pin_restaurant_admin, this::moveCameraTo
        );

        verifyAdapter = new AdminRestaurantAdapter(
                this, data, R.layout.item_verify_pin_restaurant_admin, this::moveCameraTo
        );

        // mặc định vào “quản lý chung”
        recycler.setAdapter(viewAdapter);
        loadAllRestaurants();
    }

    // Back trên AppBar
    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    // -----------------------------------------------------------------
    /*  MENU:  ⋮ -> “Tất cả”  hoặc  “Chờ xác minh”  */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.mn_all)          loadAllRestaurants();
        else if (item.getItemId() == R.id.mn_verify)  loadUnverifiedRestaurants();
        return super.onOptionsItemSelected(item);
    }

    // -----------------------------------------------------------------
    /** TẢI TẤT CẢ nhà hàng, dùng adapter thường */
    private void loadAllRestaurants() {
        recycler.setAdapter(viewAdapter);
        resRef.addListenerForSingleValueEvent(buildListener(false));
    }

    /** TẢI CHỈ nhà hàng chưa isVerified, dùng adapter verify */
    private void loadUnverifiedRestaurants() {
        recycler.setAdapter(verifyAdapter);
        resRef.orderByChild("isVerified").equalTo(false)
                .addListenerForSingleValueEvent(buildListener(true));
    }

    // -----------------------------------------------------------------
    /** Lắng nghe Firebase, nạp data & vẽ marker */
    private ValueEventListener buildListener(boolean onlyUnverified) {
        return new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                data.clear();
                clearMap();

                for (DataSnapshot child : snap.getChildren()) {
                    Restaurant r = child.getValue(Restaurant.class);
                    if (r == null) continue;
                    r.setKey(child.getKey());

                    // Nếu đang ở chế độ unverified thì bỏ qua đã verified
                    if (onlyUnverified && r.isVerified) continue;

                    data.add(r);
                    addMarkerIfVisible(r);
                }
                // refresh
                Objects.requireNonNull(recycler.getAdapter()).notifyDataSetChanged();
                Log.d(TAG, "Loaded: " + data.size());
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(AdminActivity.this, "Lỗi tải: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
    }

    // -----------------------------------------------------------------
    /** Thêm marker khi restaurant đang visible */
    private void addMarkerIfVisible(Restaurant r) {
        if (!r.isVisible || mMap == null) return;
        LatLng loc = new LatLng(r.latitude, r.longitude);
        Marker m = mMap.addMarker(new MarkerOptions()
                .position(loc).title(r.name).snippet(r.address));
        markerMap.put(r.getKey(), m);
    }
    /** Xóa hết marker khỏi map */
    private void clearMap() {
        if (mMap != null) mMap.clear();
        markerMap.clear();
    }

    // -----------------------------------------------------------------
    @Override public void onMapReady(@NonNull GoogleMap g) {
        mMap = g;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(21.0285, 105.8542), 10));
    }

    private void moveCameraTo(Restaurant r) {
        if (mMap == null) return;
        LatLng loc = new LatLng(r.latitude, r.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));
        Marker m = markerMap.get(r.getKey());
        if (m != null) m.showInfoWindow();
    }
}
