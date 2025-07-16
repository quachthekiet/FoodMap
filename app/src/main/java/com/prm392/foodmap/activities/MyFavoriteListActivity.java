package com.prm392.foodmap.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prm392.foodmap.R;
import com.prm392.foodmap.adapters.MyFavoriteAdapter;
import com.prm392.foodmap.fragments.MapsFragment;
import com.prm392.foodmap.models.Restaurant;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MyFavoriteListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MyFavoriteAdapter adapter;
    private List<Restaurant> favoriteRestaurants = new ArrayList<>();
    private String userId;

    private MapsFragment mapsFragment;
    private DatabaseReference favoritesRef, restaurantsRef;
    private LinearLayout layoutEmpty;
    public void bindingView(){
        recyclerView = findViewById(R.id.recyclerFavorites);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        favoritesRef = FirebaseDatabase.getInstance().getReference("favorites").child(userId);
        restaurantsRef = FirebaseDatabase.getInstance().getReference("restaurants");
        mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentById(R.id.favoriteMapFragment);
        layoutEmpty = findViewById(R.id.myf_layoutEmpty);
    }
    public void bindingAction(){

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_favorite_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainFavoriteLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        bindingView();
        bindingAction();
    }
    private void loadFavoriteRestaurants() {
        favoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoriteRestaurants.clear();
                if (!snapshot.hasChildren()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    return;
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                List<Restaurant> tempList = new ArrayList<>();
                AtomicInteger counter = new AtomicInteger(0);
                int totalFavorites = (int) snapshot.getChildrenCount();
                for (DataSnapshot favSnap : snapshot.getChildren()) {
                    String resId = favSnap.getKey();
                    restaurantsRef.child(resId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot resSnap) {
                            Restaurant res = resSnap.getValue(Restaurant.class);
                            if (res != null && res.isVisible && res.isVerified()) {
                                res.setKey(resSnap.getKey());
                                tempList.add(res);
                            }
                            if (counter.incrementAndGet() == totalFavorites) {
                                favoriteRestaurants.clear();
                                favoriteRestaurants.addAll(tempList);
                                adapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }

                adapter = new MyFavoriteAdapter(
                        MyFavoriteListActivity.this,
                        favoriteRestaurants,
                        userId,
                        restaurant -> {
                            if (mapsFragment != null) {
                                mapsFragment.moveCamera(new LatLng(restaurant.latitude, restaurant.longitude), 16f);
                            }
                        }
                );
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadFavoriteRestaurants();
    }
}