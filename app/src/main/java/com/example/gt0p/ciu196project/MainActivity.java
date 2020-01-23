package com.example.gt0p.ciu196project;


import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import mehdi.sakout.fancybuttons.FancyButton;


public class MainActivity extends AppCompatActivity {

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    //    For logging purpose
    private static final String TAG = MainActivity.class.getName();
    //    code for activity callback
    private static final int REQUEST_ENABLE_BT = 666;
    static boolean gameStarted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String orientation = Utils.getRotation(this);
        switch (orientation) {
            case "portrait":
            case "reverse portrait":
                setContentView(R.layout.activity_main_port);
                break;
            case "landscape":
            case "reverse landscape":
                setContentView(R.layout.activity_main_land);
        }

        FancyButton newGameButton = (FancyButton) findViewById(R.id.new_game_button);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // transition to screen QR-code server

                //Game game = Game.getInstance();
                // I am the first player in the team so I create myself
                //game.addPlayer();

                Intent intent = new Intent(v.getContext(), ServerConnActivity.class);
                startActivity(intent);
            }
        });

        FancyButton joinGameButton = (FancyButton) findViewById(R.id.join_game_button);
        joinGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Game game = Game.getInstance();

                // transition to screen QR-code client
                Intent intent = new Intent(v.getContext(), ClientConnActivity.class);
                startActivity(intent);
            }
        });


        // Setup debug player buttons
//        findViewById(R.id.debug_player0).setOnClickListener(createDebugClickListener(0));
//        findViewById(R.id.debug_player1).setOnClickListener(createDebugClickListener(1));
//        findViewById(R.id.debug_player2).setOnClickListener(createDebugClickListener(2));
//        findViewById(R.id.debug_player3).setOnClickListener(createDebugClickListener(3));

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        Game.resetTheInstance();
    }

    private View.OnClickListener createDebugClickListener(int playerId) {
        final int id = playerId;
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Check if game has been started
                if (MainActivity.gameStarted) {
                    Intent intent = new Intent(MainActivity.this, LocalPuzzleActivity.class);
                    intent.putExtra("playerId", id);
                    startActivity(intent);
                } else {

                    // As player0 start new game
                    if (id == 0) {
                        // Start the game
                        int numPlayers = 4, rows = 3, cols = 3;
                        int numTiles = rows * cols;
                        Game game = Game.getInstance();

                        Game.getInstance().addPlayer();
                        Game.getInstance().addPlayer();
                        Game.getInstance().addPlayer();
                        Game.getInstance().addPlayer();
                        Game.getInstance().startGame(v.getContext(), numTiles, true);

                        MainActivity.gameStarted = true;

                        Intent intent = new Intent(MainActivity.this, LocalPuzzleActivity.class);
                        intent.putExtra("playerId", id);
                        startActivity(intent);
                    } else {
                        // Notify if player 0 has not created a game yet
                        Toast.makeText(MainActivity.this, "No game started", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
    }


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        checkBluetooth();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            // if resultCode == RESULT_OK - do nothing
            if (resultCode == RESULT_CANCELED) {
                //show message playing not possible
                Log.e(TAG, "onActivityResult: result cancelled. No playing possible ");
                Intent intent = new Intent(this, NoGameActivity.class);
                startActivity(intent);
            }

        }
    }

    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(getBaseContext(), BluetoothService.class));
    }


    /**
     * Check if Bluetooth is available
     * Check if Bluetooth is enabled
     */
    private void checkBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // if device does not support Bluetooth
        if (mBluetoothAdapter == null) {
            // Todo: implement exception handling
            Log.e(TAG, "device does not support bluetooth");
            Intent intent = new Intent(this, NoGameActivity.class);
            startActivity(intent);

        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }
}
