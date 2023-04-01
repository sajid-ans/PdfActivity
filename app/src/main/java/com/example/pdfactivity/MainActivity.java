package com.example.pdfactivity;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Button save,upload;
    ImageView imageView;
    private ProgressBar mProgressBar;
    int PICK_IMAGES_REQUEST = 88;
    ArrayList<Bitmap> mSelectedImages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        save = findViewById(R.id.btnSave);
        upload = findViewById(R.id.btnUpload);
        imageView = findViewById(R.id.imageView);
        mProgressBar = findViewById(R.id.progressBar);

        upload.setOnClickListener(v -> openGallery());
        save.setOnClickListener(v -> {
            saveAsPdf(mSelectedImages);
        });
    }
    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            // User has selected one or more images
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                // User has selected multiple images
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri imageUri = clipData.getItemAt(i).getUri();
                    Bitmap bitmap = getBitmapFromUri(imageUri);
                    mSelectedImages.add(bitmap);
                }
            } else {
                // User has selected a single image
                Uri imageUri = data.getData();
                Bitmap bitmap = getBitmapFromUri(imageUri);
                mSelectedImages.add(bitmap);
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveAsPdf(ArrayList<Bitmap> bitmaps) {
        // Create a new document
        mProgressBar.setVisibility(View.VISIBLE);

        // Create a new thread to do the PDF conversion in the background
        new Thread(() -> {
            // Create a new document
            PdfDocument document = new PdfDocument();

            // Loop through the bitmaps and add each one as a page in the document
            for (Bitmap bitmap : bitmaps) {
                // Create a page
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);

                // Draw the compressed bitmap on the page
                Canvas canvas = page.getCanvas();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 25, stream);
                byte[] byteArray = stream.toByteArray();
                Bitmap compressedBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                canvas.drawBitmap(compressedBitmap, 0, 0, null);

                // Finish the page
                document.finishPage(page);
            }

            // Save the document
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
//            File file = new File(getExternalFilesDir(null), "image_" + timestamp + ".pdf");
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File file = new File(documentsDir,  timestamp + ".pdf");
            try {
                OutputStream outputStream = new FileOutputStream(file);
                document.writeTo(outputStream);
                document.close();
                outputStream.close();
                runOnUiThread(() -> {
                    // Hide the progress bar
                    mProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "PDF saved successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    // Hide the progress bar
                    mProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}