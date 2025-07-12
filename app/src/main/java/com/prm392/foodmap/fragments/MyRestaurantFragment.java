package com.prm392.foodmap.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prm392.foodmap.R;
/*import com.prm392.foodmap.activities.UpdateRestaurantActivity;*/
import com.prm392.foodmap.activities.UpdateRestaurantActivity;
import com.prm392.foodmap.adapters.MyRestaurantAdapter;
import com.prm392.foodmap.interfaces.DataCallback;
import com.prm392.foodmap.models.Restaurant;
import com.prm392.foodmap.models.RestaurantWithKey;
import com.prm392.foodmap.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class MyRestaurantFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private RecyclerView rvRestaurants;
    private MyRestaurantAdapter adapter;
    private final List<RestaurantWithKey> restaurantList = new ArrayList<>();

    public MyRestaurantFragment() {
        // Required empty public constructor
    }

    public static MyRestaurantFragment newInstance(String param1, String param2) {
        MyRestaurantFragment fragment = new MyRestaurantFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_restaurant, container, false);
        bindingView(view);
        bindingAction();
        loadMyRestaurants();
        return view;
    }

    private void bindingView(View view) {
        rvRestaurants = view.findViewById(R.id.myRes_rvRestaurants);
        rvRestaurants.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void bindingAction() {
        adapter = new MyRestaurantAdapter(requireContext(),restaurantList, rwk -> {
            Intent intent = new Intent(getContext(), UpdateRestaurantActivity.class);
            intent.putExtra("restaurantKey", rwk.getKey());
            updateLauncher.launch(intent);
        });
        rvRestaurants.setAdapter(adapter);
    }

    private void loadMyRestaurants() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseHelper.loadRestaurantsByOwner(currentUser.getUid(), new DataCallback<List<RestaurantWithKey>>() {
            @Override
            public void onSuccess(List<RestaurantWithKey> restaurants) {
                restaurantList.clear();
                restaurantList.addAll(restaurants);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private final ActivityResultLauncher<Intent> updateLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                    boolean updated = result.getData().getBooleanExtra("updated", false);
                    if (updated) {
                        loadMyRestaurants(); // Reload danh sách sau khi cập nhật
                    }
                }
            });

}
