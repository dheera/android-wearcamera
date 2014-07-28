package net.dheera.wearcamera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private static final String TAG = "WearCamera";
    private static final boolean D = true;
    private final MainActivity self = this;

    private GoogleApiClient mGoogleApiClient = null;
    private Node mPhoneNode = null;

    private MenuAdapter mMenuAdapter;

    private Vibrator mVibrator;

    GridViewPager mGridViewPager;

    private boolean mPreviewRunning = true;

    private static int currentTimer = 0;
    private int frameNumber = 0;
    private boolean timerIsRunning = false;

    private MessageApi.MessageListener mMessageListener = new MessageApi.MessageListener() {
        @Override
        public void onMessageReceived (MessageEvent m){
            Scanner s = new Scanner(m.getPath());
            String command = s.next();
            if (command.equals("stop")) {
                mPreviewRunning = false;
                moveTaskToBack(true);
            } else if (command.equals("start")) {
                mPreviewRunning = true;
            } else if (command.equals("show")) {
                byte[] data = m.getData();
                Bitmap bmpSmall = BitmapFactory.decodeByteArray(data, 0, data.length);
                setBitmap(bmpSmall);
                if(mPhoneNode != null && s.hasNextLong() && (frameNumber++%8) == 0) {
                    sendToPhone(String.format("received %d", s.nextLong()), null, null);
                }
            } else if(command.equals("result")) {
                if(D) Log.d(TAG, "result");
                onMessageResult(m.getData());
            }

        }
    };

    private Timer mTimer;

    int selfTimerSeconds;

    void findPhoneNode() {
        PendingResult<NodeApi.GetConnectedNodesResult> pending = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        pending.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                if(result.getNodes().size()>0) {
                    mPhoneNode = result.getNodes().get(0);
                    if(D) Log.d(TAG, "Found wearable: name=" + mPhoneNode.getDisplayName() + ", id=" + mPhoneNode.getId());
                    sendToPhone("start", null, null);
                    doSwitch(currentCamera);
                    doFlash(currentFlash);
                } else {
                    mPhoneNode = null;
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPreviewRunning = false;
        if(mPhoneNode != null) {
            sendToPhone("stop", null, new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                    moveTaskToBack(true);
                }
            });
        } else {
            findPhoneNode();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mPhoneNode != null) {
            sendToPhone("stop", null, new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                    moveTaskToBack(true);
                }
            });
        }
        Wearable.MessageApi.removeListener(mGoogleApiClient, mMessageListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mMenuAdapter = new MenuAdapter(self, getFragmentManager(), mHandler);
                mGridViewPager = (GridViewPager) findViewById(R.id.pager);
                mGridViewPager.setAdapter(mMenuAdapter);
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        findPhoneNode();
                        Wearable.MessageApi.addListener(mGoogleApiClient, mMessageListener);
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

    @Override
    protected void onPause() {
        super.onPause();
        if(mPhoneNode != null) {
            sendToPhone("stop", null, null);
        } else {
            findPhoneNode();
        }
        mPreviewRunning = false;
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        if(mPhoneNode != null) {
            sendToPhone("start", null, null);
            doSwitch(currentCamera);
            doFlash(currentFlash);
        } else {
            findPhoneNode();
        }
        mPreviewRunning = true;
        super.onResume();
    }

    public void setBitmap(final Bitmap bmp) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mMenuAdapter.mCameraFragment.cameraPreview != null) {
                        BitmapDrawable drawable = ((BitmapDrawable) mMenuAdapter.mCameraFragment.cameraPreview.getDrawable());
                        if (drawable instanceof BitmapDrawable) {
                            drawable.getBitmap().recycle();
                        }
                        mMenuAdapter.mCameraFragment.cameraPreview.setImageBitmap(bmp);
                    }
                }
            });
    }

    private static int currentFlash = 0;
    private void doFlash(int arg0) {
        currentFlash = arg0;
        sendToPhone("flash " + String.valueOf(arg0), null, null);
    }

    private static int currentCamera = 0;

    private void doSwitch(int arg0) {
        currentCamera = arg0;
        sendToPhone("switch " + String.valueOf(arg0), null, null);
    }

    private void doTimer(int arg0) {
        currentTimer = arg0;
    }

    private void takePicture() {
        if(mPhoneNode!=null) { sendToPhone("snap", null, null); }
        mMenuAdapter.mCameraFragment.cameraResult.animate().setDuration(500).translationX(mMenuAdapter.mCameraFragment.cameraResult.getWidth()).rotation(40).withEndAction(new Runnable() {
            public void run() {
                mMenuAdapter.mCameraFragment.cameraResult.setX(0);
                mMenuAdapter.mCameraFragment.cameraResult.setRotation(0);
            }
        });
    }

    private void startTimer(int seconds) {
        mMenuAdapter.mCameraFragment.cameraTime.setVisibility(View.VISIBLE);
        mTimer = new Timer();
        timerIsRunning = true;
        selfTimerSeconds = seconds + 1;
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(selfTimerSeconds-->0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMenuAdapter.mCameraFragment.cameraTime.setText(String.valueOf(selfTimerSeconds));
                            mVibrator.vibrate(10);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMenuAdapter.mCameraFragment.cameraTime.setVisibility(View.GONE);
                            takePicture();
                            mVibrator.vibrate(200);
                        }
                    });
                    this.cancel();
                    timerIsRunning = false;
                }
            }
        }, 0, 1000);
    }

    private void cancelTimer() {
        if(mTimer != null) {
            mTimer.cancel();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMenuAdapter.mCameraFragment.cameraTime.setVisibility(View.GONE);
            }
        });
        timerIsRunning = false;
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
                mMenuAdapter.mCameraFragment.cameraResult.setImageBitmap(bmp);
                mMenuAdapter.mCameraFragment.cameraResult.setTranslationX(0);
                mMenuAdapter.mCameraFragment.cameraResult.setRotation(0);
                mMenuAdapter.mCameraFragment.cameraResult.setVisibility(View.VISIBLE);
                mMenuAdapter.mCameraFragment.cameraResult.animate().setDuration(500).translationX(mMenuAdapter.mCameraFragment.cameraPreview.getWidth()).rotation(40).withEndAction(new Runnable() {
                    public void run() {
                        mMenuAdapter.mCameraFragment.cameraResult.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MenuAdapter.MESSAGE_SNAP:
                    Log.d(TAG, "MESSAGE_SNAP");
                    mGridViewPager.scrollTo(0, 0);
                    if(currentTimer == 0) {
                        takePicture();
                    } else if(currentTimer == 1) {
                        if(timerIsRunning)
                            cancelTimer();
                        else
                            startTimer(5);
                    } else if(currentTimer == 2) {
                        if(timerIsRunning)
                            cancelTimer();
                        else
                            startTimer(10);
                    }
                    break;
                case MenuAdapter.MESSAGE_SWITCH:
                    Log.d(TAG, "MESSAGE_SWITCH");
                    doSwitch(msg.arg1);
                    break;
                case MenuAdapter.MESSAGE_TIMER:
                    Log.d(TAG, "MESSAGE_TIMER");
                    doTimer(msg.arg1);
                    break;
                case MenuAdapter.MESSAGE_FLASH:
                    Log.d(TAG, "MESSAGE_FLASH");
                    doFlash(msg.arg1);
                    break;
            }
        }
    };
}
