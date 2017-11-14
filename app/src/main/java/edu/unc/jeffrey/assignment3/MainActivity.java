package edu.unc.jeffrey.assignment3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
//        db.execSQL("DROP TABLE IF EXISTS Photos");
//        db.execSQL("DROP TABLE IF EXISTS Tags");
        db.execSQL("CREATE TABLE IF NOT EXISTS Photos (ID INTEGER PRIMARY KEY, Location VARCHAR(255), Size INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS Tags (ID INTEGER, Tag VARCHAR(255), FOREIGN KEY (ID) REFERENCES Photos(ID))");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO && resultCode == RESULT_OK) {
            ImageView iv = findViewById(R.id.image);
            photo = (Bitmap) data.getExtras().get("data");
            EditText size = (EditText) findViewById(R.id.size);
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
        Log.v("photos", String.valueOf(count) + " " + directoryPath + count + ".png" + " " + photo.getByteCount() + "");
        db.execSQL("INSERT INTO Photos (ID, Location, Size) VALUES (?, ?, ?)", new String[] {String.valueOf(count), directoryPath + count + ".png", photo.getByteCount() + ""});
        for (String tag : tags) {
            Log.v("tagInsert", count + "");
            Log.v("tagInsert", tag + "");
            db.execSQL("INSERT INTO Tags (ID, Tag) VALUES (?, ?)", new String[]{String.valueOf(count), tag});
        }
        ImageView iv = findViewById(R.id.image);
        iv.setImageResource(android.R.color.transparent);
        EditText sizeBox = (EditText) findViewById(R.id.size);
        sizeBox.setText("");
        String tagText = "";
        EditText tagBox = (EditText) findViewById(R.id.tags);
        tagBox.setText("");
    }

    public void load(View view) {
        Log.v("here", "here");
        EditText stringSize = (EditText) findViewById(R.id.size);
        int size = 0;
        if (!(stringSize.getText().toString().equals(""))) {
            try {
                size = Integer.parseInt(stringSize.getText().toString());
            } catch (NumberFormatException e) {
                // dont care
            }
        }
        String[] tags = ((EditText) findViewById(R.id.tags)).getText().toString().split(";");
        Cursor c = null;
        if (((EditText) findViewById(R.id.tags)).getText().toString().length() != 0 && size != 0) {
            String query = "SELECT * FROM Photos, Tags WHERE Photos.ID = Tags.ID AND " + convertToString(tags) + " AND (Photos.Size > " + (size * 0.75) + " AND Photos.Size < " + (size * 1.25) + ")";
            Log.v("query", query + "");
            c = db.rawQuery(query + "", null);
        } else if (((EditText) findViewById(R.id.tags)).getText().toString().length() == 0) {
            Log.v("SIZE", size + "");
            String query = "SELECT * FROM Photos, Tags WHERE Photos.ID = Tags.ID AND (Photos.Size >= " + (size * 0.75) + " AND Photos.Size <= " + (size * 1.25) + ")";
            Log.v("query", query + "");
            c = db.rawQuery(query + "", null);

        } else if (size == 0) {
            String query = "SELECT * FROM Photos, Tags WHERE Photos.ID = Tags.ID AND " + convertToString(tags);
            Log.v("query", query + "");
            c = db.rawQuery(query + "", null);
        }
        // https://stackoverflow.com/questions/2810615/how-to-retrieve-data-from-cursor-class
        String output = "";
        String[] names = c.getColumnNames();
        for (String name : names) {
            Log.v("column name", name + "");
        }

        if (c.moveToFirst()){
            do{
                output += c.getInt(c.getColumnIndex("ID")) + " " + c.getString(c.getColumnIndex("Location")) + " " + c.getString(c.getColumnIndex("Tag")) + " " + c.getInt(c.getColumnIndex("Size")) + "\n";
            }while(c.moveToNext());
        }
        c.close();
        ImageView iv = findViewById(R.id.image);
        Log.v("QUERYRESULT", output + "a");
        iv.setImageBitmap(BitmapFactory.decodeFile(output.split(" ")[1] + ""));
        EditText sizeBox = (EditText) findViewById(R.id.size);
        sizeBox.setText(output.split(" ")[3].split("\n")[0]);
        String tagText = "";
        tagText += output.split(" ")[2] + ";";
        EditText tagBox = (EditText) findViewById(R.id.tags);
        tagBox.setText(tagText);
    }

    public void capture(View view) {
        count++;
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, PHOTO);
    }

    private String convertToString(String[] tags) {
        String tagText = "";
        int counter = 0;
        for (String tag : tags) {
            if (counter == 0) {
                tagText += "(Tags.Tag = \"" + tag + "\"";
            } else {
                tagText += " OR Tags.Tag = \"" + tag + "\" ";
            }
            counter++;
        }
        return tagText + ")";
    }
}
