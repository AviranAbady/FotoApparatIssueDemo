package org.aviran.fotoapparatissuedemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.error.CameraErrorCallback;
import io.fotoapparat.hardware.CameraException;
import io.fotoapparat.hardware.provider.CameraProviders;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.photo.Photo;
import io.fotoapparat.result.PendingResult;
import io.fotoapparat.view.CameraView;

/**
 * A placeholder fragment containing a simple view.
 */
public class CameraFragment extends Fragment implements CameraErrorCallback {

    private static final int PERMISSION_CAMERA_REQUEST = 1000;
    private Fotoapparat fotoApparat;
    private boolean isCameraOpen;
    private OrientationEventListener orientationListener;
    private Button button;

    public CameraFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        CameraView cameraView = view.findViewById(R.id.cameraView);
        button = view.findViewById(R.id.button);
        fotoApparat = Fotoapparat
                .with(getContext())
                .cameraErrorCallback(this)
                .cameraProvider(CameraProviders.defaultProvider(getContext()))
                .into(cameraView)
                .previewScaleType(ScaleType.CENTER_CROP)
                .build();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });

        orientationListener = new OrientationEventListener(getContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                final int margin = 20;
                if ((orientation > (90 - margin) && orientation < (90 + margin))
                        || (orientation > (270 - margin) && orientation < (270 + margin))) {

                    button.setVisibility(View.VISIBLE);

                } else {
                    button.setVisibility(View.INVISIBLE);
                }
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        orientationListener.enable();
        startCamera();
    }

    @Override
    public void onPause() {
        if(isCameraOpen) {
            fotoApparat.stop();
        }
        orientationListener.disable();
        super.onPause();
    }

    private void takePicture() {
        fotoApparat.takePicture().toPendingResult().whenAvailable(new PendingResult.Callback<Photo>() {
            @Override
            public void onResult(Photo photo) {
                extractExifData(photo.encodedImage);
            }
        });
    }

    private ExifInterface extractExifData(byte[] data) {
        InputStream in = null;
        ExifInterface exifInterface = null;
        try {
            in = new ByteArrayInputStream(data);
            exifInterface= new ExifInterface(in);
        }
        catch (IOException e) {}
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
        String orientationCode = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
        Log.i("orientation", "code: " + orientationCode);
        return exifInterface;
    }



    private void startCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            isCameraOpen = true;
            fotoApparat.start();
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CAMERA_REQUEST);

        }
    }

    @Override
    public void onError(CameraException e) {

    }
}
