package net.dheera.wearcamera;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "WearCamera";

    SurfaceHolder mSurfaceHolder;
    SurfaceView mSurfaceView;
    public Camera mCamera;
    boolean mPreviewRunning=false;
    GoogleApiClient mGoogleApiClient;
    private Node mWearableNode = null;
    public boolean image_requested = true;

    void findWearableNode() {
        PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                if(result.getNodes().size()>0) {
                    mWearableNode = result.getNodes().get(0);
                    Log.d(TAG, "Found wearable: name=" + mWearableNode.getDisplayName() + ", id=" + mWearableNode.getId());
                } else {
                    mWearableNode = null;
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView_camera);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        findWearableNode();
                        Wearable.MessageApi.addListener(mGoogleApiClient, new MessageApi.MessageListener() {
                            @Override
                            public void onMessageReceived (MessageEvent messageEvent){
                                if (messageEvent.getPath().equals("/snap")) {
                                    doSnap();
                                } else if(messageEvent.getPath().equals("/request")) {
                                    image_requested = true;
                                } else if(messageEvent.getPath().equals("/stop")) {
                                    moveTaskToBack(true);
                                }
                            }
                        });
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();
    }
    public void setCameraDisplayOrientation() {
            Camera.CameraInfo info = new Camera.CameraInfo();
            mCamera.getCameraInfo(0, info);
            int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }

            int result = (info.orientation - degrees + 360) % 360;
            mCamera.setDisplayOrientation(result);
    }
     public void doSnap(){
        Log.d(TAG, "doSnap()");
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        Camera.Size size = sizes.get(0);
        for (int i = 0; i < sizes.size(); i++) {
            if (sizes.get(i).width > size.width)
                size = sizes.get(i);
        }
        params.setPictureSize(size.width, size.height);
        mCamera.setParameters(params);
        Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d(TAG, "onPictureTaken()");
                FileOutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(String.format("/sdcard/DCIM/Camera/IMG_%d.jpg", System.currentTimeMillis()));
                    outStream.write(data);
                    outStream.close();
                    mCamera.startPreview();
                    Log.d(TAG, "wrote bytes: " + data.length);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        mCamera.takePicture(null, null, jpegCallback);
    }

    private void sendAndForget(String path, byte[] data) {
        PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mWearableNode.getId(), path, data);
        pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                }
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        if (mPreviewRunning) {
            mCamera.stopPreview();
        }
        if (mSurfaceHolder.getSurface() == null){
            return;
        }
        Camera.Parameters p = mCamera.getParameters();
        mCamera.setParameters(p);
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(arg0);
                setCameraDisplayOrientation();
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    public void onPreviewFrame(byte[] data, Camera arg1) {
                        if (mWearableNode != null && image_requested && mPreviewRunning) {
                            image_requested = false;
                            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                            int[] rgb = decodeYUV420SP(data, previewSize.width, previewSize.height);
                            Bitmap bmp = Bitmap.createBitmap(rgb, previewSize.width, previewSize.height, Bitmap.Config.ARGB_8888);
                            int smallWidth, smallHeight;
                            if(previewSize.width > previewSize.height) {
                              smallWidth = 280;
                              smallHeight = 280*previewSize.height/previewSize.width;
                            } else {
                              smallHeight = 280;
                              smallWidth = 280*previewSize.width/previewSize.height;
                            }
                            Bitmap bmpSmall = Bitmap.createScaledBitmap(bmp, smallWidth, smallHeight, false);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.JPEG, 30, baos);
                            sendAndForget("/show", baos.toByteArray());
                        }
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        mPreviewRunning = true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mPreviewRunning = false;
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
        }
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] imageData, Camera c) { }
 	};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public int[] decodeYUV420SP( byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        int rgb[]=new int[width*height];
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);
                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;
                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
        return rgb;   }

}
