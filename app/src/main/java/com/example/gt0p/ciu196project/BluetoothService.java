package com.example.gt0p.ciu196project;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothService extends Service {
    private static final String TAG = BluetoothService.class.getName();
    //private final IBinder mBinder = new LocalBinder();
    ArrayList<BluetoothSocket> sockets = new ArrayList<>();

    // Connection establishing stuff
    Thread mConnectionThread;
    private String macAddress;
    boolean isServer = false;
    boolean gameIsStarted = false;

    // Device communication stuff
    CommunicationManager mCommunicationManager;

    // Message handler stuff
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    Messenger mListener;

    public int playerId = 0;

    // Define Message codes
    static final int MSG_REQUEST_UI_UPDATE = 1;
    static final int MSG_REGISTER_LISTENER = 2;
    static final int MSG_START_GAME = 3;

    static final int MSG_SEND_OFFER = 4;
    static final int MSG_RECEIVE_OFFER = 5;
    static final int MSG_ACCEPT_OFFER = 6;

    public BluetoothService() {
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("BluetoothService", "Service received message: " + msg.what);

            switch (msg.what) {
                case MSG_REGISTER_LISTENER:
                    mListener = msg.replyTo;
                    Log.d("BluetoothService", "Service registered listener");
                    break;

                case MSG_REQUEST_UI_UPDATE:
                    sendUiUpdate();
                    break;

                case MSG_START_GAME:
                    startGame();
                    break;

                case MSG_SEND_OFFER:
                    sendOffer(msg);
                    break;

                case MSG_ACCEPT_OFFER:
                    sendAcceptOffer(msg);
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void sendOffer(Message msg) {
        TradeAction trade = TradeAction.fromBundle(msg.getData());

        Log.d("BluetoothService", "Service received a offer message");
        Log.d("BluetoothService", "Trade: (" + trade.getPlayerAId() + "," + trade.getPlayerBId() +
                "," + trade.getTileAId() + "," + trade.getTileBId() + ")");

        int sendTo;

        if(playerId == 0) {
            sendTo = trade.getPlayerBId() - 1;
        } else {
            sendTo = 0;
        }

        mCommunicationManager.write(sendTo, trade.toOfferString().getBytes());
    }

    private void receiveOffer(String tradeString) {
        // Split the string at ";"
        String[] elements = tradeString.split(";");
        TradeAction offer = new TradeAction();

        if(elements.length == 4 && elements[0].equals("offer")) {
            offer.setPlayerAId(Integer.valueOf(elements[1]));
            offer.setPlayerBId(Integer.valueOf(elements[2]));
            offer.setTileAId(Integer.valueOf(elements[3]));
        }

        // Send the offer to the local puzzle
        try {
            Message msg = Message.obtain(null, LocalPuzzleActivity.MSG_RECEIVE_TRADE);
            msg.setData(offer.toBundle());
            mListener.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void acceptOffer(String socketString) {
        String[] elements = socketString.split(";");
        TradeAction offer = new TradeAction();

        if(elements.length == 5 && elements[0].equals("accept")) {
            offer.setPlayerAId(Integer.valueOf(elements[1]));
            offer.setPlayerBId(Integer.valueOf(elements[2]));
            offer.setTileAId(Integer.valueOf(elements[3]));
            offer.setTileBId(Integer.valueOf(elements[4]));
        }

        // Send the offer to the local puzzle
        try {
            Message msg = Message.obtain(null, LocalPuzzleActivity.MSG_ACCEPT_TRADE);
            msg.setData(offer.toBundle());
            mListener.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sendAcceptOffer(Message msg) {
        TradeAction trade = TradeAction.fromBundle(msg.getData());

        int sendTo;

        if(playerId == 0) {
            sendTo = trade.getPlayerBId() - 1;
        } else {
            sendTo = 0;
        }

        mCommunicationManager.write(sendTo, trade.toAcceptString().getBytes());
    }


    private void sendUiUpdate() {
        if(mListener != null) {
            try {
                Message reply = Message.obtain(null, ServerConnActivity.MSG_UPDATE_PLAYER_COUNTER, sockets.size(), 0);
                mListener.send(reply);
                Log.d("BluetoothService", "Service send UI update: " + sockets.size());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * evoked after all clients connected --> player0 pressed startGame
     * -create local Game Object
     * -send Game Info to all bluetooth devices via m CommunicationManager
     *      -not via broadcast due to player_id
     * -notify serverConnActivity to start puzzle for player0
     */
    private void startGame() {
        Log.d("BluetoothService", "Server started the game");

        // Stop server connection thread by setting gameIsStarted
        gameIsStarted = true;

        // Start communication thread
        mCommunicationManager = new CommunicationManager();
        mCommunicationManager.start();

        // init Game object
        int numPlayers = sockets.size()+1;
        Log.d(TAG, "startGame: sockets size: "+sockets.size());
        Game game = Game.getInstance();
        game.initGame(getBaseContext(),numPlayers,9);
        int imageID = game.getPictureForPuzzleID();
        Log.d(TAG, "startGame: game getplayers" +game.getPlayers());
        //skip player0 because this is the server
        for(Player p : game.getPlayers().subList(1,game.getPlayers().size())) {
            int pID = p.getId();
            Grid<Tile> grid = game.getPlayerTiles(pID);
            String playerTileList = "";
            Log.d(TAG, "startGame: player that will receive a msg" +game.getPlayers().subList(1,game.getPlayers().size()));
            for(Tile t : grid.getElements()){
                playerTileList = playerTileList.concat(","+t.getId());
            }
            //remove first ,
            playerTileList = playerTileList.substring(1);

            // concat string message
            String msg = "start_game;" +numPlayers +";"+pID +";" +imageID +";" +playerTileList;
            Log.d(TAG, "startGame: message to send: " +msg);

            // send individual messages to p1-p3
            // TODO to match sockets (index in socket_list) and players (player_id)
            //pID-1 to match from player_id to index in socket (server is p0 -> has no socket)
            mCommunicationManager.write(pID-1,msg.getBytes());
        }
        // inform serverConnActivity

    }

    /**
     * The Client has now a socket to transfer data.
     * This method is called after the server (p0) has notified this player with the start_game message
     */
    private void clientConnectionEstablished() {
        // Start communication thread
        mCommunicationManager = new CommunicationManager();
        mCommunicationManager.start();

        try {
            Message msg = Message.obtain(null, LoadingGameActivity.MSG_START_GAME, playerId, 0);
            mListener.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

//    /**
//     * Do not call directly
//     */
    @Override
    public void onCreate(){

        // Debug: Enable button in UI
        sendUiUpdate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startBluetoothService(intent);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        startBluetoothService(intent);

        return mMessenger.getBinder();
    }

    public void startBluetoothService(Intent intent) {
        // Get the MAC address from the intent if it is inside

        macAddress = "";
        if (intent.hasExtra("macaddress")) {
            macAddress = intent.getStringExtra("macaddress");
            Log.d("BluetoothService", "MAC address provided");
        } else {
            Log.d("BluetoothService", "MAC address not provided");
        }
        
        setConnectionType(); // Distinguish between server and client

        Log.d(TAG, "startBluetoothService: gameIsStarted: " +gameIsStarted);
        if(!gameIsStarted) {
            startConnectionThread(); // Start threads to establish connection
        }

        Log.d("BluetoothService", "Service bound");
    }

    // Assume that we are a server if there is not MAC address provided
    private void setConnectionType() {
        if(macAddress.isEmpty()) {
            isServer = true;
            Log.d("BluetoothService", "Service is server");
        } else {
            isServer = false;
            Log.d("BluetoothService", "Service is client");
        }
    }

    private void startConnectionThread() {
        Log.d(TAG, "startConnectionThread: isserver "+isServer);
        if(isServer) {
            Log.d(TAG, "startConnectionThread: Start Accept Thread");
            mConnectionThread = new AcceptBluetoothThread();
        } else {
            mConnectionThread = new ConnectBluetoothThread(macAddress);
        }

        mConnectionThread.start();
    }
//
//    /** Called when The service is no longer used and is being destroyed */
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
//        mThread.stopThread();
//    }

    /**
     * This thread opens a server socket and waits for clients to connect.
     * Clients who know the MAC-Address of this device
     * can initiate a connection by binding against this socket
     *
     * implemented as a thread because accept() is blocking.
     *
     * TODO: stop listening after 4 connected players
     */
    private class AcceptBluetoothThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private BluetoothAdapter mBluetoothAdapter;
        //private ArrayList<BluetoothSocket> socketList;
        InputStream tmpIn = null;
        BluetoothService service;


        public AcceptBluetoothThread() {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //socketList = new ArrayList<>();
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                String name = getResources().getString(R.string.app_name);
                UUID myUUid = UUID.fromString(getResources().getString(R.string.uuid_string));
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(name, myUUid);
                Log.d(TAG, "AcceptBluetoothThread: "+tmp);

            } catch (IOException e) {e.printStackTrace(); e.toString();}
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d("ServerThread", "Server thread started");

            BluetoothSocket socket = null;
            // Keep listening until game is started
            while (!gameIsStarted) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted, add it to the list of accepted sockets
                if (socket != null) {
                    //TODO Quit this when 4 players have connected (sockets.size()==4)
                    sockets.add(socket);
                    sendUiUpdate();
                    Log.d(TAG, "Added socket");
               }
            }


            Log.d("ServerThread", "Server thread finished");
        }
    }

    // Client thread

    /**
     * Connection Thread for the client to initiate a bluetooth connection.
     */
    private class ConnectBluetoothThread extends Thread {

        public ConnectBluetoothThread(String mac_address) {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac_address);
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                UUID myUUid = UUID.fromString(getResources().getString(R.string.uuid_string));
                tmp = device.createRfcommSocketToServiceRecord(myUUid);
                Log.d(TAG, "ConnectBluetoothThread: "+tmp);

            } catch (IOException e) {e.printStackTrace(); }
            sockets.add(tmp);
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

            try {
                while(!sockets.get(0).isConnected()) {
                    // Connect the device through the socket. This will block
                    // until it succeeds or throws an exception
                    sockets.get(0).connect();
                }
                Log.d(TAG, "Connection established, socket:" + sockets.get(0).toString());
            } catch (IOException connectException) {
                connectException.printStackTrace();
                // Unable to connect; close the socket and get out
                try {
                    sockets.get(0).close();
                } catch (IOException closeException) { }
            }

            // Wait until game is started by the server
            Log.d("ConnectionThread", "Waiting for game to start");
            /*waiting for game to start in loadingGameActivity
             *want to leave this after game was created and we received the message start_game via bluetooth
             *start ConnectionManager, leave loading activity
             */
            while(!gameIsStarted) {
                byte[] buffer = new byte[1024];  // buffer store for the stream
                int bytes; // bytes returned from read()

                try {
                    // Read from the InputStream
                    if(!sockets.get(0).isConnected()) continue;


                    bytes = sockets.get(0).getInputStream().read(buffer);
                    // Send the obtained bytes to the UI activity
                    final String message = new String(buffer, 0, bytes);
                    Log.d(TAG, "Incoming bytes: " + Integer.toString(bytes));
                    Log.d(TAG, "Incoming message: " + message);


                    /*if(message.equals("start_game")) {
                    }*/

                    //parse info delivered. Initialize local Game
                    String[] elements = message.split(";");
                    int numPlayers;
                    int playerId;
                    int imageId;
                    ArrayList<Integer> tileIds = new ArrayList<>();

                    // Check if first element is "start_game"
                    if(elements.length >= 5 && elements[0].equals("start_game")) {
                        numPlayers = Integer.parseInt(elements[1]);
                        playerId = Integer.parseInt(elements[2]);
                        BluetoothService.this.playerId = playerId;
                        imageId = Integer.parseInt(elements[3]);

                        String[] tiles = elements[4].split(",");
                        for(String s : tiles) {
                            tileIds.add(Integer.parseInt(s));
                        }

                        Log.d("BluetoothService", "Game Info: " + numPlayers + "," + playerId +
                        "," + imageId + "," + tileIds);
                    } else {
                        continue;
                    }

                    Log.d("BluetoothService", "Start game instance");
                    // Create game object
                    Game.getInstance().startGame(getBaseContext(),numPlayers, playerId,
                            imageId, tileIds);
                    //game.startGame(getBaseContext(), )

                    gameIsStarted = true;
                    clientConnectionEstablished();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Using this to send and receive messages
     * server deals with multiple sockets.
     */
    private class CommunicationManager extends Thread {
        private ArrayList<InputStream> inStreamList;
        private ArrayList<OutputStream> outStreamList;

        public CommunicationManager() {
//            inStreamList = new ArrayList<>();
            outStreamList = new ArrayList<>();

//            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            for(BluetoothSocket socket : sockets) {
                try {
//                    tmpIn = socket.getInputStream();
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                }

//                inStreamList.add(tmpIn);
                outStreamList.add(tmpOut);
            }
        }

        public void run() {
            //Keep listening to -all- InputStreams until an exception occurs
            ExecutorService executor = Executors.newFixedThreadPool(sockets.size());
            Log.d("ConnectionManager: ", "startReaderThreads");
            for(BluetoothSocket socket : sockets) {
                InputReaderSocket newPlayer = new InputReaderSocket(socket);
                executor.execute(newPlayer);
            }
            Log.d("ConnectionManager: ", "reading threads started");
        }

        /**
         * TODO make this synchronized if many threads call it
         * Server is player 0 and doesn't have a socket. (no BT required)
         * @param index in the outStreamList (size = players-1 )
         * @param bytes the message to be sent in bytes
         */
        public synchronized void write(int index, byte[] bytes) {
            //get the client's socket and use it to send the message
            try {
                outStreamList.get(index).write(bytes);
            } catch (IOException e) {e.printStackTrace(); }
        }

        /* Call this from the main activity to send data to the remote device */
        public void broadcast(byte[] bytes) {
            try {
                for (OutputStream stream : outStreamList) {
                    stream.write(bytes);
                }
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                for(BluetoothSocket s : sockets){
                    s.close();
                }
            } catch (IOException e) { }
        }
    }

    /**
     * Thread that reads from one socket concurrently
     */
    private class InputReaderSocket implements Runnable {

        private BluetoothSocket socket;
        private InputStream in;

        public InputReaderSocket(BluetoothSocket socket) {
            Log.d("InputReaderThread: ", "init Thread");
            this.socket = socket;
            InputStream tmpIn = null;
            try {
                tmpIn  = socket.getInputStream();
            } catch(IOException ioe){
                ioe.printStackTrace();
            }
            this.in = tmpIn;
        }

        @Override
        public void run() {
            Log.d("InputReaderThread run: ", "InputReaderSocket run:");
            byte[] buffer = new byte[1024];  // buffer store for the stream
            while(this.socket.isConnected()) {
                try {
                    //Log.d("InputreaderThread ","readline");

                    int bytes; // bytes returned from read()
                    // Read from the InputStream
                    bytes = in.read(buffer);
                    // Send the obtained bytes to the UI activity
                    final String message = new String(buffer, 0, bytes);
                    Log.d("InputreaderThread: ", "Socket="+this.socket+" incomingMsg="+message);

                    // process the message
                    // Distinguish between message types
                    String[] elements = message.split(";");


                    // Route the message if its not mine
                    int other = Integer.valueOf(elements[2]);
                    if(other != playerId) {
                        mCommunicationManager.write(other-1, message.getBytes());
                        continue;
                    }

                    switch(elements[0]) {
                        case "offer":
                            // Create TradeAction object
                            receiveOffer(message);
                            break;

                        case "accept":
                            acceptOffer(message);
                            break;

                        default:
                            break;
                    }

                } catch (IOException e) {e.printStackTrace();}
            }
        }
    }
}