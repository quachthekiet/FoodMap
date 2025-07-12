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

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prm392.foodmap.R;
import com.prm392.foodmap.adapters.NearbyAdapter;
import com.prm392.foodmap.adapters.RestaurantAdapter;
import com.prm392.foodmap.fragments.MapsFragment;
import com.prm392.foodmap.models.Restaurant;
import com.prm392.foodmap.utils.LocationUtil;

import java.util.ArrayList;
import java.util.List;

public class NearbyListActivity extends AppCompatActivity {

    private RecyclerView recyclerNearby;
    private Spinner spinnerRadius;
    private NearbyAdapter adapter;

    private List<Restaurant> allRestaurants = new ArrayList<>();
    private List<Restaurant> filteredList = new ArrayList<>();

    private DatabaseReference resRef = FirebaseDatabase.getInstance().getReference("restaurants");
    private DatabaseReference reviewRef = FirebaseDatabase.getInstance().getReference("reviews");

    private MapsFragment mapsFragment;

    private int totalRestaurants = 0;
    private int loadedRestaurants = 0;

    private Location currentUserLocation;

    private void bindViews() {
        recyclerNearby = findViewById(R.id.l_recyclerNearby);
        spinnerRadius = findViewById(R.id.l_spinnerRadius);

        // Gắn adapter với danh sách được lọc
        adapter = new NearbyAdapter(this, filteredList, restaurant ->{
            moveMapToRestaurant(restaurant);
        } );
        recyclerNearby.setLayoutManager(new LinearLayoutManager(this));
        recyclerNearby.setAdapter(adapter);
    }

    private void moveMapToRestaurant(Restaurant restaurant) {
        if (mapsFragment != null) {
            mapsFragment.moveCamera(restaurant.getKey());
        }
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
        mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);

        bindViews();
        loadCurrentLocation();
        setupRadiusFilter(); // ✅ bật lại filter
        fetchRestaurantsFromFirebase();
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
                filterNearbyRestaurants(); // ✅ lọc mỗi khi chọn bán kính mới
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
                                        filterNearbyRestaurants(); // ✅ lọc khi đủ dữ liệu
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
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(NearbyListActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("NearbyListActivity.Debug", e.getMessage());
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
                res.distance = distanceMeters / 1000.0f;
                filteredList.add(res);
            }
        }
        filteredList.sort((r1, r2) -> Float.compare(r2.averageRating, r1.averageRating));

        adapter.notifyDataSetChanged();
    }
}
