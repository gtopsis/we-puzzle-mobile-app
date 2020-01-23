package com.example.gt0p.ciu196project;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.skyfishjy.library.RippleBackground;

import mehdi.sakout.fancybuttons.FancyButton;

public class ServerConnActivity extends AppCompatActivity {
    private static final String TAG = ServerConnActivity.class.getName();

    // Own variables
    int connectedPlayers = 0;

    // Service
    Messenger mService = null;
    boolean mIsBound = false;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    // Message codes
    static final int MSG_UPDATE_PLAYER_COUNTER = 1;

    // UI elements
    TextView playerCounter;

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("BluetoothService", "Activity received: " + msg.what);

            switch (msg.what) {
                case MSG_UPDATE_PLAYER_COUNTER:
                    playerCounter.setText("Player connected: " + msg.arg1);
                    connectedPlayers = msg.arg1;
                    updateUI();
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

            // Debug
            requestUiUpdate();
            //playerCounter.setText("Players connected: " + mService.getBinder().getPlayerConnected());
            Log.d("BluetoothService", "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
            Log.d("BluetoothService", "Service disconnected");
        }
    };

    // Send a message to the service to send a message back to this
    // which then triggers an update through the IncomingHandler
    private void requestUiUpdate() {
        if(mIsBound) {
            if(mService != null) {

                try {
                    Message msg = Message.obtain(null, BluetoothService.MSG_REQUEST_UI_UPDATE);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateUI() {
        playerCounter.setText("Players connected: " + connectedPlayers);

        // Activate start game button when at least one player has connected
        FancyButton newGameButton = (FancyButton) findViewById(R.id.start_game_button);
        if(connectedPlayers > 0) {
            newGameButton.setEnabled(true);
            newGameButton.setIconResource("\uf04b");
            newGameButton.setText("Start Game");
            newGameButton.setClickable(true);
            newGameButton.setAlpha(1f);
        } else {
            newGameButton.setEnabled(false);
            newGameButton.setIconResource("\uf029");
            newGameButton.setText("Wait for other players");
            newGameButton.setClickable(false);
            newGameButton.setAlpha(.5f);
        }
        Game game = Game.getInstance();
        game.enableUserConnectivityIcon(connectedPlayers,ServerConnActivity.this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This has to go first
        String orientation = Utils.getRotation(this);
        switch (orientation){
            case "portrait":
            case "reverse portrait":
                setContentView(R.layout.activity_server_conn_port);
                break;
            case "landscape":
            case "reverse landscape":
                setContentView(R.layout.activity_server_conn_land);

        }

        // Set UI elements
        playerCounter = (TextView) findViewById(R.id.PlayerCounter);

        // Start the Bluetooth service to manage all the bluetooth connections
        Intent i = new Intent(this, BluetoothService.class);

        bindService(i, mConnection, Context.BIND_AUTO_CREATE);

        RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.content);
        rippleBackground.startRippleAnimation();
        ImageView imageView=(ImageView)findViewById(R.id.qrcode);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // create thread to avoid ANR Exception
        Thread t = new Thread(new QRCodeRunnable(this));
        t.start();



        FancyButton newGameButton = (FancyButton) findViewById(R.id.start_game_button);
        // Disable at the beginning
        newGameButton.setEnabled(false);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                startGame();

                Intent intent = new Intent(v.getContext(), LocalPuzzleActivity.class);
                intent.putExtra("playerId", 0);
                startActivity(intent);
            }
        });
    }

    private void startGame() {
        // Send message to service to start game
        try {
            Log.d("ServerActivity", "Activity start game");

            Message msg = Message.obtain(null, BluetoothService.MSG_START_GAME);
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private class QRCodeRunnable implements Runnable {
        final Activity mActivity;
        final static int WIDTH = 800;

        final static int HEIGHT = 800;

        final ImageView qrCodeImageView;

        public QRCodeRunnable(Activity activity) {
            mActivity = activity;

            qrCodeImageView = (ImageView) activity.findViewById(R.id.qrcode);
        }

        Bitmap encodeAsBitmap(String str) {
            BitMatrix result;

            if (str == null)
                return null;

            try {
                result = new MultiFormatWriter().encode(str,
                        BarcodeFormat.QR_CODE, WIDTH, HEIGHT, null);
            } catch (IllegalArgumentException iae) {
                // Unsupported format
                return null;
            } catch (WriterException we) {
                we.printStackTrace();
                return null;
            }

            int w = result.getWidth();
            int h = result.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                int offset = y * w;
                for (int x = 0; x < w; x++) {
                    pixels[offset + x] = result.get(x, y) ? 0xff000000 : 0xffffffff;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, WIDTH, 0, 0, w, h);
            return bitmap;
        }

        public void run() {
            // Use MAC-address of the device as QR-code:
            String macAddress = android.provider.Settings.Secure.getString(mActivity.getContentResolver(), "bluetooth_address");
            if (macAddress == null) {
                // Older android versions
                macAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
            }

            final Bitmap bitmap = encodeAsBitmap(macAddress);

            synchronized (this) {
                // runOnUiThread method used to do UI task in main thread.
                mActivity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        qrCodeImageView.setImageBitmap(bitmap);
                    }
                });
            }
        }
    }
}