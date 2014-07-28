package net.dheera.wearcamera;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Dheera Venkatraman
 * http://dheera.net
 */
public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "WearCameraListenerService";
    private static final boolean D = false;

    @Override
    public void onCreate() {
        if(D) Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if(D) Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
    @Override
    public void onPeerConnected(Node peer) {
        if(D) Log.d(TAG, "onPeerConnected");
        super.onPeerConnected(peer);
        if(D) Log.d(TAG, "Connected: name=" + peer.getDisplayName() + ", id=" + peer.getId());
    }
    @Override
    public void onMessageReceived(MessageEvent m) {
        if(D) Log.d(TAG, "onMessageReceived: " + m.getPath());
        if(m.getPath().equals("start")) {
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }

    public boolean isForeground(String PackageName){
        // Get the Activity Manager
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        // Get a list of running tasks, we are only interested in the last one,
        // the top most so we give a 1 as parameter so we only get the topmost.
        List< ActivityManager.RunningTaskInfo > task = manager.getRunningTasks(1);

        // Get the info we need for comparison.
        ComponentName componentInfo = task.get(0).topActivity;

        // Check if it matches our package name.
        if(componentInfo.getPackageName().equals(PackageName)) return true;

        // If not then our app is not on the foreground.
        return false;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        // i don't care
    }
}