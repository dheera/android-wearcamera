package net.dheera.wearcamera;

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
 * Created by dheera on 7/11/14.
 */
public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "WearCameraListenerService";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
        /*
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
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

        mGoogleApiClient.connect();*/
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }
    @Override
    public void onPeerConnected(Node peer) {
        Log.d(TAG, "onPeerConnected()");
        super.onPeerConnected(peer);
        Log.d(TAG, "Connected: name=" + peer.getDisplayName() + ", id=" + peer.getId());
    }
    @Override
    public void onMessageReceived(MessageEvent m) {
        Log.d(TAG, "onMessageReceived()");
        Log.d(TAG, m.getPath());
        if(m.getPath().equals("/start")) {
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        /*Log.d(TAG, "onDataChanged");

        final List<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);*/

        /*
        for (DataEvent event : events) {
            Uri uri = event.getDataItem().getUri();

            // Get the node id from the host value of the URI
            String nodeId = uri.getHost();
            // Set the data of the message to be the bytes of the URI.
            byte[] payload = uri.toString().getBytes();

            // Send the RPC
            Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                    DATA_ITEM_RECEIVED_PATH, payload);
        }
        */
    }
}