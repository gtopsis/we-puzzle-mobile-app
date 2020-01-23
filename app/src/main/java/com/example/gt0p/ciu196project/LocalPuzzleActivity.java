package com.example.gt0p.ciu196project;

import android.content.ComponentName;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.github.clans.fab.FloatingActionButton;
import com.github.javiersantos.bottomdialogs.BottomDialog;

public class LocalPuzzleActivity extends AppCompatActivity {

    private Player player;

    private SwapAction swapAction = new SwapAction();
    private TradeAction tempTrade = new TradeAction();

    private MyGridAdapter gridAdapter;
    private GridView gridView;

    private RetainedFragment dataFragment;
    private boolean showActionBar = true;
    private boolean fitScreen = false;

    // Message codes
    static final int MSG_RECEIVE_TRADE = 1;
    static final int MSG_CANCEL_TRADE = 2;
    static final int MSG_ACCEPT_TRADE = 3;

    // Service
    Messenger mService = null;
    boolean mIsBound = false;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("LoadingActivity", "Activity received: " + msg.what);

            switch (msg.what) {
                case MSG_RECEIVE_TRADE:
                    Log.d("LocalPuzzle", "Receive trade offer");
                    receiveOffer(TradeAction.fromBundle(msg.getData()));
                    break;

                case MSG_CANCEL_TRADE:
                    Log.d("LocalPuzzle", "A trade offer was cancel");
                    break;

                case MSG_ACCEPT_TRADE:
                    receiveAcceptedOffer(TradeAction.fromBundle(msg.getData()));
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
        setContentView(R.layout.activity_local_puzzle);

        // Bind service
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        serviceIntent.putExtra("connectSocket", false);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);


        // Load saved data or create new data
        loadData();

        // Setup the toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        myToolbar.setTitle("");
        myToolbar.setLogo(player.getPlayerAvatar(this, player.getId()));
        setSupportActionBar(myToolbar);

        // Configure grid view and its adapter to have the puzzle and the right number of columns
        gridAdapter = new MyGridAdapter(this, player.getPuzzle());
        gridView = (GridView) findViewById(R.id.grid);
        gridView.setAdapter(gridAdapter);
        gridView.setNumColumns(player.getPuzzle().getColumns());

        setGridViewSize();

        // Define the action that happens if a grid item is clicked
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // If tile is not selected, select it if possible or swapAction tiles
                TileView tileView = (TileView) view.findViewById(R.id.tile_item);
                Tile tile = player.getPuzzle().get(position);

                if (tileView.isSelected()) {
                    deselectTileView(tileView);
                } else {
                    selectTileView(tileView);
                }

                // Deselect the tile if it is already selected
                if (swapAction.containsTile(tile)) {
                    swapAction.removeTile(tile);
                } else {
                    // Select it otherwise
                    swapAction.addTile(tile);

                    // Perform the swap action if possible
                    if (swapAction.isValid()) {
                        swap();
                        swapAction.reset();
                        if (player.isSubPuzzleCompleted()) {
                            new BottomDialog.Builder(LocalPuzzleActivity.this)
                                    .setTitle("Awesome!")
                                    .setContent("You solved your subpuzzle. Preview your puzzle and see the full picture by connecting all the subpuzzles.")
                                    .setPositiveText("Preview")
                                    .setPositiveBackgroundColorResource(R.color.primary)
                                    .setPositiveTextColorResource(android.R.color.white)
                                    .onPositive(new BottomDialog.ButtonCallback() {
                                        @Override
                                        public void onClick(BottomDialog dialog) {
                                            ActionBar actionBar = getSupportActionBar();
                                            actionBar.hide();
                                        }
                                    })
                                    .setNegativeText("Main Menu")
                                    .setNegativeTextColorResource(R.color.colorAccent)
                                    .onNegative(new BottomDialog.ButtonCallback() {
                                        @Override
                                        public void onClick(BottomDialog dialog) {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(LocalPuzzleActivity.this.getBaseContext());
                                            builder.setMessage("Are you sure you want to exit?")
                                                    .setCancelable(false)
                                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            Intent i = new Intent(LocalPuzzleActivity.this, MainActivity.class);
                                                            startActivity(i);
                                                        }
                                                    })
                                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            dialog.cancel();
                                                        }
                                                    });
                                            AlertDialog alert = builder.create();
                                            alert.show();
                                        }
                                    }).show();
                        }
                    }
                }

                // Check if the tile is part of a trade
                tempTrade.setTileAId(tile.getId());
            }

        });

        final FloatingActionButton ratio_button = (FloatingActionButton) findViewById(R.id.menu_item_ratio);
        final FloatingActionButton show_bar_button = (FloatingActionButton) findViewById(R.id.menu_item_show_bar);
        FloatingActionButton exit_game_button = (FloatingActionButton) findViewById(R.id.menu_item_exit);

        show_bar_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionBar bar = getSupportActionBar();

                if (!showActionBar) {
                    bar.show();
                    show_bar_button.setLabelText("Hide bar");
                    show_bar_button.setImageDrawable(getResources().getDrawable(R.drawable.ic_visibility_off));
                    showActionBar = true; // new state
                } else {
                    bar.hide();
                    show_bar_button.setLabelText("Show bar");
                    show_bar_button.setImageDrawable(getResources().getDrawable(R.drawable.ic_visibility));
                    showActionBar = false; // new state
                }

                setGridViewPosition();
                setGridViewSize();
                gridView.invalidate();
            }
        });

        ratio_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fitScreen = !fitScreen;
                setGridViewSize();
                gridView.invalidate();

                if (fitScreen) {
                    ratio_button.setLabelText("Keep ratio");
                } else {
                    ratio_button.setLabelText("Fit screen");

                }
            }
        });

        exit_game_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("Are you sure you want to exit?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i = new Intent(LocalPuzzleActivity.this, MainActivity.class);
                                startActivity(i);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    private void setGridViewPosition() {
        // Set the top margin of gridview parent layout

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.gridViewParent);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);


        if (showActionBar) {
            int actionBarHeight = 0;
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            }

            params.setMargins(0, actionBarHeight, 0, 0);
        } else {
            params.setMargins(0, 0, 0, 0);
        }

        layout.setLayoutParams(params);
    }

    private void setGridViewSize() {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (showActionBar && getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }

        int statusbarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusbarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        int topHeight = actionBarHeight + statusbarHeight;

        int navigationWidth = 0;
        resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            navigationWidth = getResources().getDimensionPixelSize(resourceId);
        }

        DisplayMetrics display = getResources().getDisplayMetrics();
        int hspacing = gridView.getHorizontalSpacing() * (player.getPuzzle().getColumns() - 1);
        int vspacing = gridView.getVerticalSpacing() * (player.getPuzzle().getRows() - 1);

        int iw = player.getPuzzle().get(0).getImage().getWidth() * player.getPuzzle().getRows() + hspacing;
        int ih = player.getPuzzle().get(0).getImage().getHeight() * player.getPuzzle().getColumns() + vspacing;

        int marginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, topHeight, display);
        int sw = display.widthPixels - gridView.getPaddingLeft() - gridView.getPaddingRight(); // - navigationWidth;
        int sh = display.heightPixels - marginTop - gridView.getPaddingTop() - gridView.getPaddingBottom();


        // Find maximum extend
        int w = sw;
        int h = sh;
        if (!fitScreen) {
            //if(sw > iw && sh > ih) {
            // Scale image
            float widthRatio = (float) sw / (float) iw;
            float heightRatio = (float) sh / (float) ih;

            float ratio = Math.min(widthRatio, heightRatio);
            w = (int) (iw * ratio);
            h = (int) (ih * ratio);
            //}
        }
        gridAdapter.tileWidth = w / player.getPuzzle().getColumns() - gridView.getHorizontalSpacing() - 10;
        gridAdapter.tileHeight = h / player.getPuzzle().getRows() - gridView.getVerticalSpacing() - 10;

        ViewGroup.LayoutParams gridParams = gridView.getLayoutParams();
        gridParams.width = w;
        gridParams.height = h;
        gridView.setLayoutParams(gridParams);
    }

    private void loadData() {
        Game game = Game.getInstance();
        // Find retained fragment
        FragmentManager fm = getSupportFragmentManager();
        dataFragment = (RetainedFragment) fm.findFragmentByTag("data");

        // Load puzzle data or create it if it is the first time
        if (dataFragment == null) {
            dataFragment = new RetainedFragment();

            // Parse intent content
            Intent intent = getIntent();
            int playerId = intent.getIntExtra("playerId", -1);
            player = game.getPlayerById(playerId);

            dataFragment.setPlayer(player);
            fm.beginTransaction().add(dataFragment, "data").commit();
        } else {
            player = dataFragment.getPlayer();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Get playerId
        int playerId = item.getItemId();
        String action = "";

        // Get action
        switch (item.getTitle().toString()) {
            case "send":
                action = "send";
                break;

            case "wait":
                action = "wait";
                break;

            case "accept":
                action = "accept";
                break;

            default:
                break;
        }

        // Distinguish between different states
        switch (action) {
            case "send":
                // Try to send selected tile to other player
                sendOffer(playerId);
                break;
            case "wait":
                // Cancel the offer with the other player
                //cancelOffer(playerId);
                break;
            case "accept":
                acceptOffer(playerId);
                break;
        }

        /*if(playerId != -1 && tileIndex != -1) {
            if(sendOffer(playerId, tileIndex)) {
                Toast.makeText(this, "Tile offered", Toast.LENGTH_SHORT).show();
                swapAction.reset();
            } else {
                Toast.makeText(this, "Tile not offered", Toast.LENGTH_SHORT).show();
            }
        }

        // Trade
        if(tradeId != -1) {
            acceptOffer(tradeId);
        }*/

        return true;
    }

    public void sendOffer(int playerId) {
        tempTrade.setPlayerAId(player.getId());
        tempTrade.setPlayerBId(playerId);

        // Check if sendOffer was successful
        if (player.sendOffer(tempTrade.clone())) {
            // Deselect the current tile
            Tile tile = player.findTile(tempTrade.getTileAId());
            deselectTileView(tile);

            // Send message to service
            try {
                Message msg = Message.obtain(null, BluetoothService.MSG_SEND_OFFER,
                        tempTrade.getPlayerBId(), 0);
                msg.setData(tempTrade.toBundle());
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            tempTrade.reset();
            swapAction.reset();
        }

        invalidateOptionsMenu();
    }

    public void receiveOffer(TradeAction trade) {
        //player.receiveOffer(trade);
        Game game = Game.getInstance();

        Player playerB = game.getPlayerById(trade.getPlayerAId());
        playerB.receiveOffer(trade);

        invalidateOptionsMenu();
    }

    // Cancel offer if tabbed again
    public void cancelOffer(int playerId) {
        tempTrade.setPlayerBId(playerId);
        player.cancelOffer(tempTrade);

        invalidateOptionsMenu();
    }


    private void acceptOffer(int playerId) {
        Game game = Game.getInstance();
        tempTrade.setPlayerAId(player.getId());
        tempTrade.setPlayerBId(playerId);

        // Check if a tile is selected
        if (tempTrade.getTileAId() != -1) {

            // Fill remaining information
            int offeredTile = game.getPlayerById(playerId).tradeActions[player.getId()].getTileAId();
            tempTrade.setTileBId(offeredTile);

        }

        if (tempTrade.isValid()) {
            // Replace own tile with offered tile
            Tile ownTile = player.findTile(tempTrade.getTileAId());
            Tile offeredTile = game.getTile(tempTrade.getTileBId());

            player.getPuzzle().set(player.findGridIndex(ownTile.getId()), offeredTile);




            gridAdapter.notifyDataSetChanged();

            // Replace tile of the other player
            //game.getPlayerById(tempTrade.getPlayerBId()).acceptOffer(tempTrade);

            // Send offered tile back to the other player
            try {
                Message msg = Message.obtain(null, BluetoothService.MSG_ACCEPT_OFFER, tempTrade.getPlayerBId(), 0);
                msg.setData(tempTrade.toBundle());
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            // Reset trade object with the other player
            player.tradeActions[tempTrade.getPlayerBId()].reset();
            game.getPlayerById(tempTrade.getPlayerBId()).tradeActions[tempTrade.getPlayerAId()].reset();
            tempTrade.reset();
            swapAction.reset();


            invalidateOptionsMenu();

        }



        /*TradeAction offer = player.tradeActions[playerTradeId];

        if(offer == null) return false;

        Tile trade = findTile(offer.getTileAId());

        // Tile found
        if(trade != null) {
            // Create tile from tileId
            int sampleIndex = 15;
            String path = "sample_" + sampleIndex;
            int resource = getResources().getIdentifier(path, "drawable", getPackageName());
            Bitmap image = Game.puzzleTiles.get(15).getImage();

            offer.setTileBId(15);
            Tile replacingTile = new Tile(offer.getTileBId(), image);

            // Exchange tile with a hardcoded replacement
            player.getPuzzle().set(findGridIndex(offer.getTileAId()), replacingTile);
            gridAdapter.notifyDataSetChanged();

            player.tradeActions[playerTradeId].reset();
            swapAction.reset();
            invalidateOptionsMenu();
            return true;
        }

        return false;*/
    }

    public void receiveAcceptedOffer(TradeAction trade) {
        if (trade.isValid()) {
            Game game = Game.getInstance();
            // Replace own tile with offered tile
            Tile ownTile = player.findTile(trade.getTileBId());
            Tile offeredTile = game.getTile(trade.getTileAId());

            player.getPuzzle().set(player.findGridIndex(ownTile.getId()), offeredTile);

            // Reset trade object with the other player
            player.tradeActions[trade.getPlayerAId()].reset();
            game.getPlayerById(trade.getPlayerAId()).tradeActions[trade.getPlayerBId()].reset();
            tempTrade.reset();
            swapAction.reset();

            gridAdapter.notifyDataSetChanged();

            // Replace tile of the other player
            //game.getPlayerById(trade.getPlayerBId()).acceptOffer(trade);

            invalidateOptionsMenu();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Game game = Game.getInstance();

        // Debug: Add button to accept offers
        /*for (int i = 0; i < game.getNumOfPlayers(); i++) {
            Drawable tradeImage = ContextCompat.getDrawable(this, R.drawable.ic_trade).mutate();

            if (i != player.getId()) {
                if (player.tradeActions[i].getTileAId() != -1) {
                    tradeImage = tintDrawable(tradeImage, Player.getPlayerColor(this, i));
                }

                String title = "trade" + i;
                menu.add(title)
                        .setIcon(tradeImage)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            }
        }*/

        // Add player avatars for each other player
        for (int i = 0; i < game.getNumOfPlayers(); i++) {
            if (i != player.getId()) {
                Drawable avatar = player.getPlayerAvatar(this, i);
                String action = "send";

                // Use waiting avatar if there is an open trade
                if (player.hasOpenTrade(i)) {
                    avatar = ContextCompat.getDrawable(this, R.drawable.ic_avatar_wait_white).mutate();
                    avatar = tintDrawable(avatar, Player.getPlayerColor(this, i));
                    action = "wait";
                }

                // Use notified avatar if there is an incoming trade from another player
                if (game.playerHasOpenTrades(player.getId(), i)) {
                    avatar = ContextCompat.getDrawable(this, R.drawable.ic_avatar_notification).mutate();
                    avatar = tintDrawable(avatar, Player.getPlayerColor(this, i));
                    action = "accept";
                }

                menu.add(0, i, 0, action)
                        .setIcon(avatar)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }

        return true;
    }

    public Drawable tintDrawable(Drawable image, int color) {
        Drawable wrapped = DrawableCompat.wrap(image);
        DrawableCompat.setTint(wrapped, color);
        DrawableCompat.setTintMode(wrapped, PorterDuff.Mode.MULTIPLY);

        return wrapped;
    }

    @Override
    public void onPause() {
        super.onPause();
        Game game = Game.getInstance();
        game.setPlayer(player.getId(), player);
        dataFragment.setPlayer(player);
    }

    private void deselectTileView(TileView view) {
        // Deselect the clicked item
        view.setSelected(false);
    }

    private void deselectTileView(Tile tile) {
        int gridIndex = player.findGridIndex(tile.getId());

        View item = gridView.getChildAt(gridIndex);
        if (item != null) {
            TileView tileView = (TileView) item.findViewById(R.id.tile_item);
            deselectTileView(tileView);
        }
    }


    // returns true if a tile has been selected
    private void selectTileView(TileView view) {
        // Select the clicked item
        view.setSelected(true);
    }

    private void swap() {
        // Swap the tile with the tileIds
        int gridIndexA = player.findGridIndex(swapAction.getTileA().getId());
        int gridIndexB = player.findGridIndex(swapAction.getTileB().getId());

        gridAdapter.grid.swap(gridIndexA, gridIndexB);

        // Deselect both tiles
        View item = gridView.getChildAt(gridIndexA);
        if (item != null) {
            TileView tileView = (TileView) item.findViewById(R.id.tile_item);
            deselectTileView(tileView);
        }


        item = gridView.getChildAt(gridIndexB);
        if (item != null) {
            TileView tileView = (TileView) item.findViewById(R.id.tile_item);
            deselectTileView(tileView);
        }

        gridAdapter.notifyDataSetChanged();
        gridView.invalidateViews();
    }

    private class MyGridAdapter extends BaseAdapter {

        private Context context;
        private Grid<Tile> grid;
        public int tileWidth;
        public int tileHeight;

        public MyGridAdapter(Context context, Grid grid) {
            this.context = context;
            this.grid = grid;
        }

        @Override
        public int getCount() {
            return grid.size();
        }

        @Override
        public Object getItem(int position) {
            return grid.get(position);
        }


        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater =
                    (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View gridView = inflater.inflate(R.layout.tile_view, parent, false);

            // Set image of tile view
            TileView tileView = (TileView) gridView.findViewById(R.id.tile_item);
            BitmapDrawable img = new BitmapDrawable(getResources(), grid.get(position).getImage());
            tileView.setImage(img);

            // Set height of view to prevent scrolling
            /*DisplayMetrics display = getResources().getDisplayMetrics();
            float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 30, display);

            int imageHeight = img.getIntrinsicHeight();
            int imageWidth = img.getIntrinsicWidth();

            int screenHeight = display.heightPixels - actionbarHeight;
            int screenWidth = display.widthPixels - actionbarHeight;

            int gridHeight =  screenHeight;
            int gridWidth = screenWidth;
            int heightPerTile = gridHeight / grid.getRows();
            int widthPerTile = gridWidth / grid.getColumns();

            int height = Math.max(imageHeight, heightPerTile);
            int width = Math.max(imageWidth, widthPerTile);

            */
            ViewGroup.LayoutParams params = gridView.getLayoutParams();
            params.height = tileHeight;
            params.width = tileWidth;
            gridView.setLayoutParams(params);


            return gridView;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.exit_confirmation))
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent i = new Intent(LocalPuzzleActivity.this, MainActivity.class);
                        startActivity(i);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
