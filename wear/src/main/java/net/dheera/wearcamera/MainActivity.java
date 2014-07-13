package net.dheera.wearcamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private static final String TAG = "WearCamera";
    private static final boolean D = false;

    private GoogleApiClient mGoogleApiClient = null;
    private Node mPhoneNode = null;
    private ImageView imageView;
    private TextView textViewCountDown;

    private boolean previewRunning = true;

    void findPhoneNode() {
        PendingResult<NodeApi.GetConnectedNodesResult> pending = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        pending.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                if(result.getNodes().size()>0) {
                    mPhoneNode = result.getNodes().get(0);
                    if(D) Log.d(TAG, "Found wearable: name=" + mPhoneNode.getDisplayName() + ", id=" + mPhoneNode.getId());
                    sendToPhone("/start", null, null);
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
        if(mPhoneNode != null && previewRunning) {
            sendToPhone("/stop", null, new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                    moveTaskToBack(true);
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        previewRunning = false;
        if(mPhoneNode != null) {
            sendToPhone("/stop", null, new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                    moveTaskToBack(true);
                }
            });
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mPhoneNode != null) {
            sendToPhone("/stop", null, new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                    moveTaskToBack(true);
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                imageView = (ImageView) stub.findViewById(R.id.imageView);
                textViewCountDown = (TextView) stub.findViewById(R.id.textViewCountDown);
                imageView.setOnLongClickListener(new View.OnLongClickListener() {
                   @Override
                    public boolean onLongClick(View v) {
                       imageView_onLongClick(v);
                       return true;
                    }
                });
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        findPhoneNode();
                        Wearable.MessageApi.addListener(mGoogleApiClient, new MessageApi.MessageListener() {
                            @Override
                            public void onMessageReceived (MessageEvent m){
                                if (m.getPath().equals("/stop")) {
                                    previewRunning = false;
                                    moveTaskToBack(true);
                                } else if (m.getPath().equals("/start")) {
                                    previewRunning = true;
                                } else if (mPhoneNode!=null && m.getPath().equals("/show")) {
                                    sendToPhone("/received", null, null);
                                    byte[] data = m.getData();
                                    Bitmap bmpSmall = BitmapFactory.decodeByteArray(data, 0, data.length);
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

    public void imageView_onClick(View view) {
        if(mPhoneNode!=null) { sendToPhone("/snap", null, null); }
        imageView.animate().setDuration(500).translationX(imageView.getWidth()).rotation(40).withEndAction(new Runnable() {
            public void run() {
                imageView.setX(0);
                imageView.setRotation(0);
            }
        });
    }

    int selfTimer;

    public void imageView_onLongClick(final View view) {
        textViewCountDown.setVisibility(View.VISIBLE);
        Timer mTimer = new Timer();
        selfTimer = 6;
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(selfTimer-->0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewCountDown.setText(String.valueOf(selfTimer));
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewCountDown.setVisibility(View.GONE);
                            imageView_onClick(view);
                        }
                    });
                    this.cancel();
                }
            }
        }, 0, 1000);
    }

    private void sendToPhone(String path, byte[] data, final ResultCallback<MessageApi.SendMessageResult> callback) {
        if (mPhoneNode != null) {
            PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mPhoneNode.getId(), path, data);
            pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    if (callback != null) {
                        callback.onResult(result);
                    }
                    if (!result.getStatus().isSuccess()) {
                        if(D) Log.d(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                }
            });
        } else {
            if(D) Log.d(TAG, "ERROR: tried to send message before device was found");
        }
    }
}
