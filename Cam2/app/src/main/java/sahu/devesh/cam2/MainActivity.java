package sahu.devesh.cam2;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Button cam, browse, upload;
    private ImageView preview;
    private Uri fileUri = null;

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int PICK_IMAGE = 2;
    public static final int UPLOAD_IMAGE = 3;

    public static String server = "http://104.236.112.86/";

    private static Uri getOutputMediaFileUri (int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private static File getOutputMediaFile(int type){

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CameraApp");

        if (! mediaStorageDir.exists()) {
            if(! mediaStorageDir.mkdirs()){
                Log.d("Cam2", "failed to create directory");
                if(! mediaStorageDir.canWrite()){
                    Log.d("Cam2", "no writing permission");
                }
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        }
        else{
            return null;
        }

        return mediaFile;
    }

    private void getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor imageSource = parcelFileDescriptor.getFileDescriptor();

            Bitmap image = BitmapFactory.decodeFileDescriptor(imageSource);

            preview.setImageBitmap(image);

        }catch (FileNotFoundException e) {
        }catch (IOException e){
        }finally {
            if (parcelFileDescriptor != null)
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cam = (Button)findViewById(R.id.camera);
        preview = (ImageView)findViewById(R.id.imageView);
        browse = (Button) findViewById(R.id.browse);
        upload = (Button) findViewById(R.id.upload);

        cam.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });

        browse.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });

        upload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){


            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Image saved to: " + data.getData(), Toast.LENGTH_LONG).show();
                getBitmapFromUri(fileUri);
            }

            else if (resultCode == RESULT_CANCELED) {
            }

            else {
            }
        }

        else if (requestCode == PICK_IMAGE){
            if (data != null) {
                fileUri = data.getData();
                Log.i("CamApp", "Uri: " + fileUri.toString());
                getBitmapFromUri(fileUri);
            }
        }

        else if (requestCode == UPLOAD_IMAGE){

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
