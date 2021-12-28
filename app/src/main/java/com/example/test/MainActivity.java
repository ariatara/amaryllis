package com.example.test;

import android.Manifest;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView imageView;
    private Uri photoURI;
    private FirebaseVisionImage FBVimage;
    private Bitmap bitmap;

    private FileActivity fileActivity = new FileActivity();

    /*
     * Entry point to the app
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        // When user clicks on camera button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePictureAction(view);
            }
        });
    }

    /*
     * Load Action Bar at the top
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_main, menu);
        fileActivity.editText = findViewById(R.id.edit_text);
        // When user clicks on save button
        // Saves text from the current state of the editText
        findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                    fileActivity.saveText(fileActivity.editText, storageDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Confirmation that all text has been saved
                Toast.makeText(MainActivity.this, "Saved text.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // When user clicks on load/view button (eye icon)
        // Loads all *.txt files from the "Documents" folder of app's storage

        findViewById(R.id.load_button).setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view)   {
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                fileActivity.loadFile(storageDir);
                // Confirmation that all files in "Documents" was printed
                Toast.makeText(MainActivity.this, "Loaded text. ",
                        Toast.LENGTH_SHORT).show();
            }
        });
        return true;
    }

    /*
     * Handle when an item is selected from the Action Bar menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // When action bar options is selected
        switch (item.getItemId()) {
            // When "Clear data" is selected
            case R.id.action_clearData:
                // Prompts the user to confirm file deletion
                decision();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Prepare to take an image and create an image file
     */
    private void takePictureAction(View view)    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {

                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                photoFile = fileActivity.createImageFile(storageDir);

            } catch (IOException ex) {
                // Error occurred while creating the file
                Toast.makeText(this, "Failed to create image file.",
                        Toast.LENGTH_SHORT).show();
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.test.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /*
     * Handle take picture action
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageView = findViewById(R.id.image_view);
            // Displays image in an imageView
            displayThumbnail(imageView);
        }
    }

    /*
     * Display image thumbnail in Image View
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void displayThumbnail(ImageView imageView) {
        this.getContentResolver().notifyChange(photoURI, null);
        ContentResolver contentResolver = this.getContentResolver();
        try {
            bitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, photoURI);
            ExifInterface exif = new ExifInterface(fileActivity.image);
            // Make sure orientation of image is correctly displayed
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            if (orientation == 6) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);
           } else if (orientation == 3) {
                Matrix matrix = new Matrix();
                matrix.postRotate(180);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);
            } else if (orientation == 8) {
                Matrix matrix = new Matrix();
                matrix.postRotate(270);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);
            }
            // Set bitmap in imageView
            imageView.setImageBitmap(bitmap);
            // Create FirebaseVisionImage for recognizer to use
            FBVimage = FirebaseVisionImage.fromBitmap(bitmap);
            fileActivity.runTextRecog(FBVimage);
        }
        catch (Exception e) {
            // If image could not be loaded on imageView
            Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * User is prompted to choose whether or not to clear all saved data
     */
    public void decision() {
        // User can choose whether or not to delete all .txt files and photos stored
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Are you sure you want to clear all files? This action cannot be undone!");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        File docDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                        File picDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        // Clears all data
                        fileActivity.clear(docDir, picDir);
                        // Reset editText
                        fileActivity.editText.setText(getResources().getString(R.string.hello_first_fragment));
                        // Confirmation that all tasks have run
                        Toast.makeText(MainActivity.this, "All files deleted.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
}
