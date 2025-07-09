package com.prm392.foodmap.activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.prm392.foodmap.R;
import com.prm392.foodmap.utils.ImageHelper;

public class ImagePreviewActivity extends AppCompatActivity {
    private ImageView imageView;
    private String imageUrl;

    public void bindingView(){
        imageView = findViewById(R.id.preview_imageView);
        imageUrl = getIntent().getStringExtra("image_url");
    }
    public void bindingAction(){
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_preview);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        bindingView();
        bindingAction();
        if (imageUrl != null) {
            ImageHelper.loadImage(this, imageUrl, imageView);
        }
    }
}