package com.prm392.foodmap.activities;

import android.content.Intent;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import com.prm392.foodmap.R;
import com.prm392.foodmap.adapters.SearchSuggestionAdapter;
import com.prm392.foodmap.fragments.MapsFragment;
import com.prm392.foodmap.fragments.ProfileFragment;
import com.prm392.foodmap.models.Constants;
import com.prm392.foodmap.models.Restaurant;
import com.prm392.foodmap.utils.LocationUtil;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class HomeActivity extends AppCompatActivity implements ProfileFragment.OnAuthButtonClickListener {

    private static final String TAG = "HomeActivity";
    private static final int RC_SIGN_IN = 1001;

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth mAuth;

    private MapsFragment mapsFragment;

    private EditText edtSearch;

    private RecyclerView suggestionList;
    private SearchSuggestionAdapter suggestionAdapter;

    private Location currentUserLocation;

    private List<Restaurant> allRestaurants = new ArrayList<>();

    private final android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;

    private void moveMapToRestaurant(Restaurant restaurant) {
        if (mapsFragment != null) {
            mapsFragment.moveCamera(restaurant.getKey());
        }
    }

    private static String removeVietnameseDiacritics(String str) {
        if (str == null)
            return "";
        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replaceAll("đ", "d").replaceAll("Đ", "D");
    }

    private void sortAndShowSuggestions(String query) {
        String normalizedQuery = removeVietnameseDiacritics(query.toLowerCase());
        List<Restaurant> filtered = new ArrayList<>();
        for (Restaurant r : allRestaurants) {
            if (query.isEmpty()) {
                filtered.add(r);
            } else if (r.name != null) {
                String normalizedName = removeVietnameseDiacritics(r.name.toLowerCase());
                if (normalizedName.contains(normalizedQuery)) {
                    filtered.add(r);
                }
            }
        }

        filtered.sort((r1, r2) -> {
            int cmp = Float.compare(r1.distance, r2.distance);
            if (cmp == 0) {
                return Float.compare(r2.averageRating, r1.averageRating);
            }
            return cmp;
        });

        if (filtered.isEmpty()) {
            suggestionList.setVisibility(View.GONE);
        } else {
            suggestionAdapter.setRestaurantList(filtered);
            suggestionList.setVisibility(View.VISIBLE);
        }
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        bottomNavigationView = findViewById(R.id.h_bottomNavigationView);
        edtSearch = findViewById(R.id.h_edtSearch);

        suggestionList = findViewById(R.id.h_suggestionList);
        suggestionList.setLayoutManager(new LinearLayoutManager(this));
        suggestionAdapter = new SearchSuggestionAdapter(this, new ArrayList<>(), res -> {
            edtSearch.clearFocus();
            suggestionList.setVisibility(View.GONE);
            moveMapToRestaurant(res);
        });
        suggestionList.setAdapter(suggestionAdapter);
    }

    private void bindActions() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Hủy runnable cũ nếu có
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                final String query = s.toString().trim();

                // Delay 300ms trước khi thực thi
                searchRunnable = () -> {
                    if (edtSearch.hasFocus()) {
                        loadRestaurantsWithQuery(query);
                    } else {
                        suggestionList.setVisibility(View.GONE);
                    }
                };

                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        edtSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String currentQuery = edtSearch.getText().toString();
                if (!currentQuery.isEmpty()) {
                    loadRestaurantsWithQuery(currentQuery);
                }
            } else {
                suggestionList.setVisibility(View.GONE);
            }
        });

    }

    private void loadRestaurantsWithQuery(String query) {
        if (query.trim().isEmpty()) {
            suggestionAdapter.setRestaurantList(new ArrayList<>());
            suggestionList.setVisibility(View.GONE);
            return;
        }
        LatLng latLng = LocationUtil.getSavedLocation(this);
        if (latLng != null) {
            currentUserLocation = new Location("custom");
            currentUserLocation.setLatitude(latLng.latitude);
            currentUserLocation.setLongitude(latLng.longitude);
        }

        DatabaseReference resRef = FirebaseDatabase
                .getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("restaurants");
        DatabaseReference reviewRef = FirebaseDatabase.getInstance().getReference("reviews");

        String normalizedQuery = removeVietnameseDiacritics(query.toLowerCase());

        resRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Restaurant> filteredRestaurants = new ArrayList<>();

                final int[] total = { 0 };
                int[] loaded = { 0 };

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Restaurant res = snap.getValue(Restaurant.class);
                    if (res == null || !res.isVisible() || !res.isVerified())
                        continue;
                    res.setKey(snap.getKey());
                    total[0]++;

                    reviewRef.child(res.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot reviewSnap) {
                            float totalRating = 0;
                            int count = 0;
                            for (DataSnapshot review : reviewSnap.getChildren()) {
                                Long r = review.child("rating").getValue(Long.class);
                                if (r != null) {
                                    totalRating += r;
                                    count++;
                                }
                            }

                            res.averageRating = (count > 0) ? totalRating / count : 0;
                            res.reviewCount = count;

                            if (currentUserLocation != null) {
                                Location resLoc = new Location("firebase");
                                resLoc.setLatitude(res.latitude);
                                resLoc.setLongitude(res.longitude);
                                res.distance = currentUserLocation.distanceTo(resLoc) / 1000.0f; // km
                            } else {
                                res.distance = Float.MAX_VALUE;
                            }

                            // Lọc theo query không dấu
                            if (res.name != null) {
                                String normalizedName = removeVietnameseDiacritics(res.name.toLowerCase());
                                if (normalizedName.contains(normalizedQuery)) {
                                    filteredRestaurants.add(res);
                                }
                            }

                            loaded[0]++;
                            if (loaded[0] == total[0]) {
                                filteredRestaurants.sort((r1, r2) -> {
                                    int cmp = Float.compare(r1.distance, r2.distance);
                                    if (cmp == 0) {
                                        return Float.compare(r2.averageRating, r1.averageRating);
                                    }
                                    return cmp;
                                });

                                if (filteredRestaurants.isEmpty()) {
                                    suggestionList.setVisibility(View.GONE);
                                } else {
                                    suggestionAdapter.setRestaurantList(filteredRestaurants);
                                    suggestionList.setVisibility(View.VISIBLE);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loaded[0]++;
                            if (loaded[0] == total[0]) {
                                suggestionAdapter.setRestaurantList(filteredRestaurants);
                                suggestionList.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }

                if (total[0] == 0) {
                    suggestionAdapter.setRestaurantList(new ArrayList<>());
                    suggestionList.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        bindViews();
        bindActions();

        mAuth = FirebaseAuth.getInstance();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.api_key));
        }
        loadMainFragment();

        googleSignInClient = GoogleSignIn.getClient(
                this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadMainFragment();
                closeProfileDrawer();
                return true;
            } else if (id == R.id.nav_profile) {
                openProfileDrawer();
                return true;
            } else if (id == R.id.nav_list) {
                startActivity(new Intent(this, NearbyListActivity.class));
                return true;
            } else if (id == R.id.nav_favorites) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(this, "Vui lòng đăng nhập để xem quán yêu thích", Toast.LENGTH_SHORT).show();
                    openProfileDrawer();
                } else {
                    startActivity(new Intent(this, MyFavoriteListActivity.class));
                }
                return true;
            }
            return false;
        });

        // Nếu đã đăng nhập → update UI thôi, không redirect
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateProfileUI(currentUser);
        }
    }

    // region GOOGLE SIGN-IN
    public void startGoogleSignIn() {
        startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);

        if (req == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount acc = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(acc.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential cred = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(cred).addOnCompleteListener(this, t -> {
            if (!t.isSuccessful()) {
                Toast.makeText(this, "Đăng nhập Firebase thất bại", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null)
                return;
            saveUserIfFirstLoginThenRedirect(user);
        });
    }

    // region SAVE USER & REDIRECT

    private void saveUserIfFirstLoginThenRedirect(FirebaseUser user) {
        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users").child(user.getUid());

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("email", user.getEmail());
                    data.put("name", user.getDisplayName());
                    data.put("role", Constants.ROLE_USER);
                    data.put("createdAt", System.currentTimeMillis());
                    ref.setValue(data);
                }
                checkUserRoleAndRedirect(user); // chỉ gọi sau đăng nhập
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(HomeActivity.this, "Lỗi DB", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserRoleAndRedirect(FirebaseUser user) {
        DatabaseReference roleRef = FirebaseDatabase
                .getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users").child(user.getUid()).child("role");

        roleRef.get().addOnSuccessListener(snap -> {
            String role = snap.getValue(String.class);
            Log.d(TAG, "role = " + role);
            Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
            updateProfileUI(user);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi xác định vai trò", Toast.LENGTH_LONG).show();
        });
    }

    // region UI

    private void loadMainFragment() {
        MapsFragment newMaps = new MapsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.h_fragmentContainerView, newMaps)
                .commit();
        mapsFragment = newMaps;
    }

    private void openProfileDrawer() {
        ProfileFragment pf = new ProfileFragment();
        pf.setGoogleSignInClient(googleSignInClient);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.profileDrawer, pf)
                .commit();
        drawerLayout.openDrawer(Gravity.RIGHT);
    }

    private void closeProfileDrawer() {
        if (drawerLayout.isDrawerOpen(Gravity.RIGHT))
            drawerLayout.closeDrawer(Gravity.RIGHT);
    }

    private void updateProfileUI(FirebaseUser user) {
        ProfileFragment pf = (ProfileFragment) getSupportFragmentManager()
                .findFragmentById(R.id.profileDrawer);
        if (pf != null)
            pf.updateUI(user);
        closeProfileDrawer();
    }

    @Override
    public void onAuthButtonClicked() {
        startGoogleSignIn();
    }

    @Override
    public void onLogout() {
        closeProfileDrawer();
    }

    @Override
    public void onLogoutSuccess() {
        Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
    }
}
