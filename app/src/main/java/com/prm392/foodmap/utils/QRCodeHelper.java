package com.prm392.foodmap.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.prm392.foodmap.interfaces.DataCallback;
import com.prm392.foodmap.models.Restaurant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class QRCodeHelper {

    // Tạo QR code từ content
    public static Bitmap generateQRCode(String content, int size) throws WriterException {
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
        BarcodeEncoder encoder = new BarcodeEncoder();
        return encoder.createBitmap(matrix);
    }

    // Nén ảnh bitmap → file JPEG tại cache
    public static File compressAndSaveBitmapToCache(Context context, Bitmap bitmap, String fileName) throws IOException {
        File cacheDir = new File(context.getCacheDir(), "qr_cache");
        if (!cacheDir.exists()) cacheDir.mkdirs();

        File file = new File(cacheDir, fileName + ".jpg");
        FileOutputStream fos = new FileOutputStream(file);

        // Nén ảnh, 70% chất lượng là hợp lý
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
        fos.flush();
        fos.close();

        return file;
    }

    // Chuyển file thành URI
    public static Uri getUriFromFile(Context context, File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
    }

    // Xoá toàn bộ cache ảnh QR
    public static void clearQRCodeCache(Context context) {
        File cacheDir = new File(context.getCacheDir(), "qr_cache");
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            for (File file : cacheDir.listFiles()) {
                file.delete();
            }
        }
    }

    public static void generateAndUploadQRCode(Context context, String restaurantId, DataCallback<String> callback) {
        try {
            Bitmap qrBitmap = QRCodeHelper.generateQRCode(restaurantId, 300);
            File qrFile = QRCodeHelper.compressAndSaveBitmapToCache(context, qrBitmap, restaurantId);
            Uri qrUri = QRCodeHelper.getUriFromFile(context, qrFile);

            MediaManager.get().upload(qrUri)
                    .option("folder", "restaurant_qr_codes")
                    .option("upload_preset", "foodmap_preset")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String qrUrl = (String) resultData.get("secure_url");
                            QRCodeHelper.clearQRCodeCache(context);
                            callback.onSuccess(qrUrl);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            callback.onError("Upload QR thất bại: " + error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();

        } catch (WriterException | IOException e) {
            e.printStackTrace();
            callback.onError("Lỗi tạo QR: " + e.getMessage());
        }
    }
    public static void uploadQRCodeIfNeeded(Context context, Restaurant res) {
        if (res.getQrCodeUrl() == null || res.getQrCodeUrl().isEmpty()) {
            generateAndUploadQRCode(context, res.getKey(), new DataCallback<String>() {
                @Override
                public void onSuccess(String qrUrl) {
                    FirebaseDatabase.getInstance().getReference("restaurants")
                            .child(res.getKey()).child("qrCodeUrl").setValue(qrUrl);
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}