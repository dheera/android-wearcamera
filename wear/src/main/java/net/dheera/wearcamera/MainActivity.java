package net.dheera.wearcamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
    private static final boolean D = true;



    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private GoogleApiClient mGoogleApiClient = null;
    private Node mPhoneNode = null;
    private ImageView mImageView;
    private ImageView mImageView2;
    private TextView textViewCountDown;

    private boolean mPreviewRunning = true;

    int selfTimerSeconds;

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
        mPreviewRunning = false;
        if(mPhoneNode != null && mPreviewRunning) {
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
        mPreviewRunning = false;
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
                mImageView = (ImageView) stub.findViewById(R.id.imageView);
                mImageView2 = (ImageView) stub.findViewById(R.id.imageView2);
                textViewCountDown = (TextView) stub.findViewById(R.id.textViewCountDown);
                mImageView.setOnLongClickListener(new View.OnLongClickListener() {
                   @Override
                    public boolean onLongClick(View v) {
                       mImageView_onLongClick(v);
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
                                    mPreviewRunning = false;
                                    moveTaskToBack(true);
                                } else if (m.getPath().equals("/start")) {
                                    mPreviewRunning = true;
                                } else if (mPhoneNode!=null && m.getPath().equals("/show")) {
                                    // sendToPhone("/received", null, null);
                                    byte[] data = m.getData();
                                    Bitmap bmpSmall = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    setBitmap(bmpSmall);
                                } else if(m.getPath().equals("/result")) {
                                    if(D) Log.d(TAG, "result");
                                    onMessageResult(m.getData());
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
                    if(mImageView != null) {
                        BitmapDrawable drawable = ((BitmapDrawable) mImageView.getDrawable());
                        if (drawable instanceof BitmapDrawable) {
                            drawable.getBitmap().recycle();
                        }
                        mImageView.setImageBitmap(bmp);
                    }
                }
            });
    }

    public void mImageView_onClick(View view) {
        if(mPhoneNode!=null) { sendToPhone("/snap", null, null); }
        /*mImageView.animate().setDuration(500).translationX(mImageView.getWidth()).rotation(40).withEndAction(new Runnable() {
            public void run() {
                mImageView.setX(0);
                mImageView.setRotation(0);
            }
        });*/
    }


    public void mImageView_onLongClick(final View view) {
        textViewCountDown.setVisibility(View.VISIBLE);
        Timer mTimer = new Timer();
        selfTimerSeconds = 6;
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(selfTimerSeconds-->0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewCountDown.setText(String.valueOf(selfTimerSeconds));
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewCountDown.setVisibility(View.GONE);
                            mImageView_onClick(view);
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
    
    private void onMessageResult(final byte[] data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                mImageView2.setImageBitmap(bmp);
                mImageView2.setX(0);
                mImageView2.setRotation(0);
                mImageView2.setVisibility(View.VISIBLE);
                mImageView2.animate().setDuration(500).translationX(mImageView2.getWidth()).rotation(40).withEndAction(new Runnable() {
                    public void run() {
                        mImageView2.setVisibility(View.GONE);
                    }
                });
            }
        });
    }
}
