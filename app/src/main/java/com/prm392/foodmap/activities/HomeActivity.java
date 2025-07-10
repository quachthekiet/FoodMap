package com.prm392.foodmap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.prm392.foodmap.R;
import com.prm392.foodmap.fragments.MapsFragment;
import com.prm392.foodmap.fragments.ProfileFragment;

import java.util.Arrays;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements ProfileFragment.OnAuthButtonClickListener {

    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int RC_SIGN_IN = 1001;
    private EditText edtSearch;
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private void bindViews() {
        edtSearch = findViewById(R.id.h_edtSearch);
        drawerLayout = findViewById(R.id.drawerLayout);
        bottomNavigationView = findViewById(R.id.h_bottomNavigationView);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();

        mAuth = FirebaseAuth.getInstance();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.api_key));
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

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
            }
            return false;
        });
    }

    public void startGoogleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void openAutocompleteActivity() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                edtSearch.setText(place.getName());
                Log.i("HomeActivity", "Place: " + place.getName() + ", " + place.getId());
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR && data != null) {
                Toast.makeText(this, "Lỗi tìm kiếm: " + Autocomplete.getStatusFromIntent(data).getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                        // Cập nhật UI hoặc xử lý sau đăng nhập
                        ProfileFragment profileFragment = (ProfileFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.profileDrawer);
                        if (profileFragment != null) {
                            profileFragment.updateUI(mAuth.getCurrentUser());
                        }
                        closeProfileDrawer();
                    } else {
                        Toast.makeText(this, "Đăng nhập thất bại.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMainFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.h_fragmentContainerView, fragment)
                .commit();
    }

    private void openProfileDrawer() {
        ProfileFragment profileFragment = new ProfileFragment();
        profileFragment.setGoogleSignInClient(mGoogleSignInClient);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.profileDrawer, profileFragment)
                .commit();
        drawerLayout.openDrawer(Gravity.RIGHT);
    }

    private void closeProfileDrawer() {
        if (drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            drawerLayout.closeDrawer(Gravity.RIGHT);
        }
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
