package com.prm392.foodmap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
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
import com.prm392.foodmap.fragments.MapsFragment;
import com.prm392.foodmap.fragments.ProfileFragment;
import com.prm392.foodmap.models.Constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements ProfileFragment.OnAuthButtonClickListener {

    private static final String TAG = "HomeActivity";
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int RC_SIGN_IN = 1001;

    private EditText edtSearch;
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth mAuth;

    // ──────────────────────────────────────────────────────────────────────────

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
        mAuth = FirebaseAuth.getInstance();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.api_key));
        }

        googleSignInClient = GoogleSignIn.getClient(
                this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
        );

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.h_fragmentContainerView, new MapsFragment())
                .commit();

        edtSearch.setFocusable(false);
        edtSearch.setOnClickListener(v -> openAutocompleteActivity());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadMainFragment(new MapsFragment());
                closeProfileDrawer();
                return true;
            } else if (id == R.id.nav_profile) {
                openProfileDrawer();
                return true;
            } else if (id == R.id.nav_list) {
                startActivity(new Intent(this, NearbyListActivity.class));
                return true;
            }
            else if (id == R.id.nav_favorites) {
                startActivity(new Intent(this, MyFavoriteListActivity.class));
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

        if (req == AUTOCOMPLETE_REQUEST_CODE) {
            if (res == RESULT_OK && data != null) {
                Place p = Autocomplete.getPlaceFromIntent(data);
                edtSearch.setText(p.getName());
            } else if (res == AutocompleteActivity.RESULT_ERROR) {
                Toast.makeText(this, "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show();
            }
            return;
        }

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
            if (user == null) return;
            saveUserIfFirstLoginThenRedirect(user);
        });
    }

    // region SAVE USER & REDIRECT

    private void saveUserIfFirstLoginThenRedirect(FirebaseUser user) {
        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users").child(user.getUid());

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
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

            @Override public void onCancelled(@NonNull DatabaseError e) {
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
    private void bindViews() {
        edtSearch = findViewById(R.id.h_edtSearch);
        drawerLayout = findViewById(R.id.drawerLayout);
        bottomNavigationView = findViewById(R.id.h_bottomNavigationView);
    }

    private void loadMainFragment(androidx.fragment.app.Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.h_fragmentContainerView, f)
                .commit();
    }

    private void openAutocompleteActivity() {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME,
                Place.Field.ADDRESS, Place.Field.LAT_LNG);
        startActivityForResult(
                new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this),
                AUTOCOMPLETE_REQUEST_CODE);
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
        if (pf != null) pf.updateUI(user);
        closeProfileDrawer();
    }

    @Override public void onAuthButtonClicked() { startGoogleSignIn(); }
    @Override public void onLogout()            { closeProfileDrawer(); }
    @Override public void onLogoutSuccess()     { Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show(); }
}
