package edu.unc.jeffrey.assignment3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private int PHOTO = 0;
    private String directoryPath;
    private Bitmap photo;
    SQLiteDatabase db;
    private static int count = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Here, we are making a folder named picFolder to store
        // pics taken by the camera using this application.
        directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Assignment5/";
        File dir = new File(directoryPath);
        dir.mkdirs();

        //set count to highest so far
        for (File f : dir.listFiles()) {
            if (Integer.parseInt(f.getName().split("\\.")[0]) > count) {
                count = Integer.parseInt(f.getName().split("\\.")[0]);
            }
        }

        db = this.openOrCreateDatabase("Assignment5", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Photos (ID INTEGER PRIMARY KEY AUTOINCREMENT, Location VARCHAR(255), Size INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS Tags (ID INTEGER, Tag VARCHAR(255), FOREIGN KEY (ID) REFERENCES Photos(ID))");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO && resultCode == RESULT_OK) {
            ImageView iv = findViewById(R.id.image);
            photo = (Bitmap) data.getExtras().get("data");
            final EditText size = (EditText) findViewById(R.id.size);
            size.setText(photo.getByteCount() + "", TextView.BufferType.EDITABLE);
            // save the image
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(directoryPath + count + ".png");
                photo.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            iv.setImageBitmap(photo);

            Log.v("CameraDemo", "Pic saved");
        }
    }

    public void save(View view) {
        String[] tags = ((EditText) findViewById(R.id.tags)).getText().toString().split(";");
        db.execSQL("INSERT INTO Photos (Location, Size) VALUES (?, ?)", new String[] {directoryPath + count + ".png", photo.getByteCount() + ""});
        for (String tag : tags) {
            db.execSQL("INSERT INTO Tags (ID, Tag) VALUES (?, ?)", new String[]{String.valueOf(count), tag});
        }
    }

    public void load(View view) {
    }

    public void capture(View view) {
        count++;
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, PHOTO);
    }
}
