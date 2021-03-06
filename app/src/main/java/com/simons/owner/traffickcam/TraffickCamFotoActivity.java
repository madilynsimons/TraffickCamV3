package com.simons.owner.traffickcam;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.parameter.LensPosition;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.photo.BitmapPhoto;
import io.fotoapparat.result.PendingResult;
import io.fotoapparat.result.PhotoResult;
import io.fotoapparat.view.CameraView;

import static io.fotoapparat.parameter.selector.FlashSelectors.autoFlash;
import static io.fotoapparat.parameter.selector.FlashSelectors.autoRedEye;
import static io.fotoapparat.parameter.selector.FlashSelectors.off;
import static io.fotoapparat.parameter.selector.FlashSelectors.torch;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.autoFocus;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.continuousFocus;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.fixed;
import static io.fotoapparat.parameter.selector.LensPositionSelectors.lensPosition;
import static io.fotoapparat.parameter.selector.Selectors.firstAvailable;
import static io.fotoapparat.parameter.selector.SizeSelectors.biggestSize;

/**
 * FotoApparat repo:
 * https://github.com/Fotoapparat/Fotoapparat
 */

public class TraffickCamFotoActivity extends AppCompatActivity {

    private final int CAMERA_PERMISSION_CODE = 10;

    private boolean hasCameraPermission; // whether or not user has granted permissions
    private CameraView cameraView; // camera view -- how the user sees what the camera sees
    private Fotoapparat fotoapparat; // open source camera

    /*
     *  List<String> s contains a list of things the user will be told to take pictures of.
     *  If you wish to change the list of things the user should be taking pictures of,
     *  feel free to change the list here.
     */
    protected List<String> s
            = Arrays.asList("window", "bed", "TV", "bathroom");

    // iterates List s -- use this to go through subjects
    protected ListIterator<String> subjects
            = s.listIterator();

    // holds a copy of the current subject
    protected String currentSubject;

    protected ImageView stencilView;

    protected Bitmap bitmaps[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.simons.owner.traffickcam.R.layout.activity_traffick_cam_foto);

        stencilView = (ImageView) findViewById(com.simons.owner.traffickcam.R.id.imageView);

        // TODO -- Camera permission is absolutely necessary to proceed
        // make sure that the app actually has permission to use the camera
        // and make granting permission more user friendly
        getCameraPermission();

        cameraView = (CameraView) findViewById(com.simons.owner.traffickcam.R.id.camera_view);
        cameraView.setVisibility(View.VISIBLE);

        fotoapparat = initFotoapparat();

        makeBitmaps();
        printNextInstruction();
        makeStencil();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasCameraPermission) {
            fotoapparat.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (hasCameraPermission) {
            fotoapparat.stop();
        }
    }

    /*
     *  Requests permission to use the phone camera from the user
     *  Whether or not the app has permission to the camera is stored in bool hasCameraPermission
     */
    private void getCameraPermission()
    {
        // request permission to use camera
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE
        );

        // check to see whether or not camera permission has been granted
        int permissionCheck = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        );

        // if TraffickCam has permission to use the user's camera, hasCameraPermission is true
        hasCameraPermission = (permissionCheck == PackageManager.PERMISSION_GRANTED);
    }

    // Sets up fotoapparat
    // see FotoApparat docs to further customize
    private Fotoapparat initFotoapparat() {
        return Fotoapparat
                .with(this)
                .into(cameraView)
                .previewScaleType(ScaleType.CENTER_CROP)
                .photoSize(biggestSize())
                .lensPosition(lensPosition(LensPosition.BACK))
                .focusMode(firstAvailable(
                        continuousFocus(),
                        autoFocus(),
                        fixed()
                ))
                .flash(firstAvailable(
                        autoRedEye(),
                        autoFlash(),
                        torch(),
                        off()
                ))
                .build();
    }

    // Uses Toast to tell the user what to take a picture of
    // TODO -- find an alternative to Toast
    protected void printNextInstruction()
    {
        currentSubject = subjects.next();
        Toast.makeText(this, getString(com.simons.owner.traffickcam.R.string.take_pic_message) + " " + currentSubject + ".", Toast.LENGTH_LONG).show();
    }

    /**
     *  takePictureOnClick will take a picture whenever the user touches the screen
     *  As of right now, the photo taken saves to a generic file path
     *  TODO -- save photo to correct file path
     *  If there are more subjects left to be iterated through, the next instruction is printed
     */
    public void takePictureOnClick(View view)
    {
        PhotoResult photoResult = fotoapparat.takePicture(); // takes photo
        savePicture(photoResult); // saves photo

        // When the photo is available,
        // if there are more subjects to take pictures of, the app will tell the user to do so
        // else, the next activity is triggered
        photoResult
                .toBitmap()
                .whenAvailable(new PendingResult.Callback<BitmapPhoto>() {
                    @Override
                    public void onResult(BitmapPhoto result) {
                        if(subjects.hasNext()){
                            printNextInstruction();
                            makeStencil();
                        }
                    }
                });
        if(subjects.hasNext() == false) exit();

    }

    // Saves PhotoResult photoResult to a generic file
    // TODO -- get proper final directory
    private void savePicture(PhotoResult photoResult)
    {
        String path = getExternalFilesDir("photos") + "/" + currentSubject + ".jpg";
        File photoFile = new File(path);
        photoResult.saveToFile(photoFile);
    }

    protected void makeBitmaps()
    {
        int length = s.size();
        // get .bmp files
        bitmaps = new Bitmap[length];
        bitmaps[0] = BitmapFactory.decodeResource(getResources(), R.drawable.pikachu);
        bitmaps[1] = BitmapFactory.decodeResource(getResources(), R.drawable.charmander);
        bitmaps[2] = BitmapFactory.decodeResource(getResources(), R.drawable.squirtle);
        bitmaps[3] = BitmapFactory.decodeResource(getResources(), R.drawable.charmander);

        for(int i = 0; i < length; i++)
        {
            // make the bitmap mutable
            bitmaps[i] = bitmaps[i].copy(Bitmap.Config.ARGB_8888, true);

            int width = bitmaps[i].getWidth();
            int height = bitmaps[i].getHeight();

            int [] allpixels = new int [height * width];

            bitmaps[i].getPixels(allpixels, 0, width, 0, 0, width, height);

            for(int j = 0; j < allpixels.length; j++)
            {
                if(allpixels[j] == Color.WHITE)
                {
                    allpixels[j] = Color.TRANSPARENT;
                }
            }

            bitmaps[i].setPixels(allpixels,0,width,0, 0, width, height);
            bitmaps[i].setHasAlpha(true);
        }

    }

    // makes and displays stencil
    protected void makeStencil()
    {
        Bitmap bm = null;
        switch(currentSubject)
        {
            case "window":
                bm = bitmaps[0];
                break;
            case "bed":
                bm = bitmaps[1];
                break;
            case "TV":
                bm = bitmaps[2];
                break;
            case "bathroom":
                bm = bitmaps[3];
                break;
        }
        stencilView.setImageBitmap(bm);
    }

    // Changes to next activity
    protected void exit()
    {
        Intent exit = new Intent(this, HotelListActivity.class);
        startActivity(exit);
    }


}
