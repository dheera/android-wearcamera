package net.dheera.wearcamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.zip.Inflater;

public class MainActivity extends Activity {

    private static final String TAG = "WearCamera";

    private Timer myTimer;
    private TextView mTextView;
    GoogleApiClient mGoogleApiClient = null;
    private Node mPhoneNode = null;
    private boolean connected = false;
    ImageView imageView;
    // ImageView imageView = (ImageView) View.inflate(this, R.id.imageView, null);

    private boolean previewRunning = true;

    Handler mHandler = new Handler();
    Thread timer = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(10000);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(mPhoneNode!=null) { sendAndForget("/request", null); }
                        }
                    });
                } catch (Exception e) {
                }
            }
        }
    });


    void findWearableNode() {
        Log.d(TAG,"foo");
        PendingResult<NodeApi.GetConnectedNodesResult> pending = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        pending.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                Log.d(TAG,"foo2");
                if(result.getNodes().size()>0) {Log.d(TAG,"foo3");
                    mPhoneNode = result.getNodes().get(0);
                    Log.d(TAG, "Found wearable: name=" + mPhoneNode.getDisplayName() + ", id=" + mPhoneNode.getId());
                    sendAndForget("/start", null);
                    sendAndForget("/request", null);
                    //timer.start();
                } else {
                    mPhoneNode = null;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        previewRunning = false;
        PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mPhoneNode.getId(), "/stop", null);
        pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult result) {
                if (!result.getStatus().isSuccess()) { Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus()); }
                finish();
            }
        });
    }
    @Override
    protected void onStop() {
        super.onStop();
        previewRunning = false;
        PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mPhoneNode.getId(), "/stop", null);
        pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult result) {
                if (!result.getStatus().isSuccess()) { Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus()); }
                finish();
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        previewRunning = false;
        PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mPhoneNode.getId(), "/stop", null);
        pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult result) {
                if (!result.getStatus().isSuccess()) { Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus()); }
                finish();
            }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                imageView = (ImageView) stub.findViewById(R.id.imageView);
            }
        });
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        findWearableNode();
                        Wearable.MessageApi.addListener(mGoogleApiClient, new MessageApi.MessageListener() {
                            @Override
                            public void onMessageReceived (MessageEvent messageEvent){
                                if (mPhoneNode!=null && previewRunning && messageEvent.getPath().equals("/show")) {
                                    sendAndForget("/request", null);
                                    ByteArrayInputStream bais = new ByteArrayInputStream(messageEvent.getData());
                                    Bitmap bmpSmall = BitmapFactory.decodeStream(bais);
                                    setBitmap(bmpSmall);
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
    public void setBitmap(final Bitmap bmp) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageBitmap(bmp);
                }
            });
    }
    public void buttonSnap_onClick(View view) {
        if(mPhoneNode!=null) { sendAndForget("/snap", null); }
    }
    private void sendAndForget(String path, byte[] data) {
        PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mPhoneNode.getId(), path, data);
        pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                }
            }
        });
    }
}
