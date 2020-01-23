package com.example.gt0p.ciu196project;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.util.ArrayList;


public class LoadingGameActivity extends AppCompatActivity {
    // Service
    Messenger mService = null;
    boolean mIsBound = false;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    // Message codes
    static final int MSG_START_GAME = 1;


    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("LoadingActivity", "Activity received: " + msg.what);

            switch (msg.what) {
                case MSG_START_GAME:
                    startGame(msg.arg1);
                    break;

                default:
                    super.handleMessage(msg);
            }

        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            mIsBound = true;

            // Register activity at service
            try {
                Message msg = Message.obtain(null, BluetoothService.MSG_REGISTER_LISTENER);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
            Log.d("BluetoothService", "Service disconnected");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_game);

        // parse MAC address
        Intent intent = getIntent();
        String macAddress = "";

        if(intent.hasExtra("macaddress")) {
            macAddress = intent.getStringExtra("macaddress");
        }

        Intent serviceIntent = new Intent(this, BluetoothService.class);
        serviceIntent.putExtra("macaddress", macAddress);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);

        CircularProgressView progressView = (CircularProgressView) findViewById(R.id.progress_view);
        progressView.startAnimation();
    }

    private void startGame(int playerId) {
        Log.d("LoadingGameActivity", "Join game");
        // Start puzzle activity

        Intent i = new Intent(this, LocalPuzzleActivity.class);
        i.putExtra("playerId", playerId);
        startActivity(i);
    }
}
