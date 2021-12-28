package com.example.test;

import android.content.DialogInterface;
import android.os.Environment;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileActivity {

    public EditText editText;
    public File image;

    private File txtFile;
    private String txtFileName;
    private String currentPhotoPath;

    /*
     * Create an image file
     */
    public File createImageFile(File storageDir) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        image = File.createTempFile(imageFileName, ".jpg", storageDir);
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /*
     * Run the text recognizer model
     */
    public void runTextRecog(FirebaseVisionImage image) {
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        // Start text recognizer
        Task<FirebaseVisionText> result =
                detector.processImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        // Task completed successfully
                        processExtractedText(firebaseVisionText);
                    }
                }).addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                            }
                        });
        // End detector
    }

    /*
     * Display recognized text in the Text Editor
     */
    public void processExtractedText(FirebaseVisionText firebaseVisionText) {
        editText.setText(null);
        if (firebaseVisionText.getTextBlocks().size() == 0) {
            editText.setText(R.string.blank);
            return;
        }
        for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
            editText.append(block.getText());

        }
    }

    /*
     * Save current text in Text Editor to file
     */
    public File saveText(EditText editText, File storageDir) throws IOException {
        // Create a .txt file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        txtFileName = "TXT_" + timeStamp + "_";
        txtFile = File.createTempFile(txtFileName, ".txt", storageDir);
        // Write onto the created .txt file to save the editText text
        FileWriter writer = new FileWriter(txtFile);
        writer.append(editText.getText().toString());
        writer.flush();
        writer.close();
        return txtFile;
    }

    /*
     * Load all files and display in the Text Editor
     */
    public void loadFile(File storageDir){
        StringBuilder text = new StringBuilder();
        // Retrieve all .txt files in "Documents" folder of the app's storage
        File file[] = storageDir.listFiles();
        assert file != null;
        // Print all files onto the editText with a separator
        for (int i = 0; i < file.length; i++){
            File fileEvents = new File(String.valueOf(file[i]));
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileEvents));
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                // Separator
                text.append("━◦○◦━◦○◦━◦○◦━◦○◦━◦○◦━◦○◦━◦○◦━");
                text.append('\n');
                br.close();
            } catch (IOException e) {
            }
        }
        String result = text.toString();
        editText.setText(result);
    }


    /*
     * Clears all saved data in files from the app
     */
    public void clear(File docDir, File picDir) {
        // Clears all .txt files
        File doc[] = docDir.listFiles();
        assert doc != null;
        for (int i = 0; i < doc.length; i++) {
            doc[i].delete();
        }
        // Clears all photos saved in storage
        File pic[] = picDir.listFiles();
        assert doc != null;
        for (int i = 0; i < pic.length; i++) {
            pic[i].delete();
        }
    }
}
