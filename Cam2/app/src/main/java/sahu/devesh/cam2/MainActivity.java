package sahu.devesh.cam2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Button cam, browse, upload;
    private ImageView preview;
    private Uri fileUri = null;

    private Bitmap bitmap = null;

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int PICK_IMAGE = 2;

    // Tracks user action (Capture or Pick)
    public static int choice = 0;

    // Server Location
    public String server = "http://104.236.112.86/process_photo/";

    // Absolute path of the source Image
    public String sourceImagePath;
    public String destinationImagePath;

    // Create a file to store Captured Image
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

    // Convert the file into Grayscale image and save it again
    private void convertToGrayScale(String path){
        File image = new File(path);
        Bitmap original = BitmapFactory.decodeFile(image.getAbsolutePath());
        int width, height;
        height = original.getHeight();
        width = original.getWidth();

        Bitmap grayScale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(grayScale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(original, 0, 0, paint);

        try {
            OutputStream fOut = new FileOutputStream(image);

            grayScale.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(getContentResolver(), image.getAbsolutePath(), image.getName(), image.getName());

        }catch (Exception e) {
        }
    }

    // Resample to fit the image view
    private void resetImageforView(String path){

        File image = new File(path);

        if (image == null){
            Log.d("preview", "image is empty!");
            return;
        }
        else {
            Log.d("preview", "Displaying image at " + path);
        }
        Bitmap temp = BitmapFactory.decodeFile(image.getAbsolutePath());
        temp = Bitmap.createScaledBitmap(temp,1024, 768,true);
        preview.setImageBitmap(temp);
    }

   // Copy the image from Uri to a new location
   // Save the absolute path of this file in sourceImagePath
   private void saveUriToFile(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor imageSource = parcelFileDescriptor.getFileDescriptor();

            bitmap = BitmapFactory.decodeFileDescriptor(imageSource);

            String path = Environment.getExternalStorageDirectory().toString();
            OutputStream fOut = null;
            File file = new File (path, "imageUploaded" + ".jpg");
            sourceImagePath = file.getAbsolutePath();
            fOut = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(),file.getName(),file.getName());

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

    // Upload raw image and download/save colorized image in a separate thread
    private class talkToServer extends AsyncTask<ImageView, Void, String>{

        ImageView imageView;

        protected String doInBackground(ImageView... params){

            this.imageView = params[0];

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        + "/CameraApp/" + "Col_IMG_" + timeStamp + ".jpg" ;

            Bitmap bitmap = BitmapFactory.decodeFile(sourceImagePath);

            String attachmentName = "bwPhoto";
            String attachmentFileName = "bwPhoto.bmp";
            String crlf = "\r\n";
            String twoHyphens = "--";
            String boundary =  "*****";


            try {
                File sourceFile = new File(sourceImagePath);
                FileInputStream fileInputStream = new FileInputStream(sourceFile);

                HttpURLConnection httpURLConnection = null;
                URL url = new URL(server);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);

                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
                httpURLConnection.setRequestProperty("Cache-Control", "no-cache");
                httpURLConnection.setRequestProperty(
                        "Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(httpURLConnection.getOutputStream());
                request.writeBytes(twoHyphens + boundary + crlf);
                request.writeBytes("Content-Disposition: form-data; name=\"" +
                        attachmentName + "\";filename=\"" +
                        attachmentFileName + "\"" + crlf);
                request.writeBytes(crlf);

                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                bytesRead  = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0 ){
                    request.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                request.writeBytes(crlf);
                request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

                request.flush();
                request.close();

                if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK){

                    InputStream responseStream = new BufferedInputStream(httpURLConnection.getInputStream());

                    BufferedInputStream bis = new BufferedInputStream(responseStream, 8190);

                    ByteArrayOutputStream nbuffer = new ByteArrayOutputStream();

                    byte[] imageData = new byte[50];
                    int current = 0;

                    while((current = bis.read(imageData,0,imageData.length)) != -1){
                        nbuffer.write(imageData,0,current);
                    }

                    /*String path2 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            + "/CameraApp/" + "Col_IMG_temp"+ ".jpg" ;*/
                    File newfile = new File(path);

                    FileOutputStream fos = new FileOutputStream(newfile);
                    fos.write(nbuffer.toByteArray());
                    fos.close();

                   /*Bitmap outputBitmap = BitmapFactory.decodeFile();

                    OutputStream fOut = null;
                    File outFile = new File (path);
                    fOut = new FileOutputStream(outFile);

                    outputBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
                    fOut.flush();
                    fOut.close();

                    MediaStore.Images.Media.insertImage(getContentResolver(), outFile.getAbsolutePath(),outFile.getName(),outFile.getName());
                    */
                    destinationImagePath = path;

                }

                else {
                    String serverResponseMessage = httpURLConnection.getResponseMessage();



                    Log.i("uploadFile", "HTTP Response is : "+ serverResponseMessage);

                    Log.d("HttpURLconnection","connection not OK");
                }

                httpURLConnection.disconnect();


            }catch (Exception e){

                Log.d("HttpURLconnection", "Connection failure");

            }
            return "True";
        }

        protected void onPostExecute(String result){
            File image = new File(destinationImagePath);

            if (image == null){
                Log.d("preview", "image is empty!");
                return;
            }
            else {
                Log.d("preview", "Displaying image at " + destinationImagePath);
            }
            Bitmap temp = BitmapFactory.decodeFile(image.getAbsolutePath());
            temp = Bitmap.createScaledBitmap(temp,1024, 768,true);
            imageView.setImageBitmap(temp);
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

        // Open Camera Intent to take picture
        cam.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                sourceImagePath = fileUri.getPath();

                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });

        // Open Gallary to choose picture
        browse.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });

        // Upload picture to the server and download colorized one
        upload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                Log.d("upload", sourceImagePath);

                new talkToServer().execute(preview);

                //resetImageforView(destinationImagePath);

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // Call back for captured image
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                convertToGrayScale(sourceImagePath);

                resetImageforView(sourceImagePath);

            }

            else if (resultCode == RESULT_CANCELED) {
                // Print cancelled
            }

            else {
                // Do nothing
            }
        }

        // Call back for picked image
        else if (requestCode == PICK_IMAGE){
            if (data != null) {
                fileUri = data.getData();

                Log.i("Browse ", "Uri " + fileUri.toString());

                saveUriToFile(fileUri);

                convertToGrayScale(sourceImagePath);

                resetImageforView(sourceImagePath);

            }
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