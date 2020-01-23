package com.example.gt0p.ciu196project;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by gt0p on 13/10/2016.
 */
//TODO adjust game class a bit for the tasks:
//TODO    -server: init game with server-info
//TODO    -client: init game, set info provided by server
public class Game {
    private static Game instance = null;

    private Bitmap pictureForPuzzle;
    private int pictureForPuzzleID;
    private ArrayList<Tile> puzzleTiles;
    private ArrayList<Player> players;
    private int numTilesPerUser;
    private final int NUMBER_OF_EXHIBITION_PICTURES = 6;

    private Game() {
        // Game class is a singleton
        players = new ArrayList<Player>();
        numTilesPerUser = 9;
        puzzleTiles = new ArrayList<Tile>();
    }

    public static synchronized Game getInstance() {
        if (instance == null) {
            instance = new Game();
        }
        return instance;
    }

    // Reset the game in case that user go back and forth in the main activity
    public synchronized static void resetTheInstance() {
        if (instance == null)
            return;
        instance.players = new ArrayList<Player>();
        instance.numTilesPerUser = 9;
        instance.puzzleTiles = new ArrayList<Tile>();
    }

    // When a connection established it should be created a new player
    public void addPlayer() {
        int pid = getNumOfPlayers();
        Player p = new Player(pid);
        players.add(p);
    }

    public Player getPlayerById(int pid) {
        for (int i = 0; i < getNumOfPlayers(); ++i) {
            if (players.get(i).getId() == pid)
                return players.get(i);
        }
        return null;
    }

    /**
     * selects a random image and sets this image as pictureforpuzzle in this class
     */
    public void selectRandomPicture(Context c) {
        // TODO randomize algorithm
        Random rand = new Random();
        int rndNum = rand.nextInt(NUMBER_OF_EXHIBITION_PICTURES) % NUMBER_OF_EXHIBITION_PICTURES;
        pictureForPuzzleID = rndNum;
        String name = "exc" + rndNum;
        String packageName = c.getPackageName();
        int resId = c.getResources().getIdentifier(name, "drawable", packageName);
        Bitmap bmp = BitmapFactory.decodeResource(c.getResources(), resId);
        pictureForPuzzle = bmp;
    }


    public void selectImageFromId(Context c, int imageId) {
        String name = "exc" + imageId;
        String packageName = c.getPackageName();
        int resId = c.getResources().getIdentifier(name, "drawable", packageName);
        Bitmap bmp = BitmapFactory.decodeResource(c.getResources(), resId);
        pictureForPuzzle = bmp;
    }

    // called after game configuration (when there are all the information)
    public void startGame(Context c, int tilesPerUser, boolean isServer) {

        int numOfPlayers = getNumOfPlayers();

        if (isServer) {
            // select a random image to create the puzzle
            selectRandomPicture(c);
        }

        // generate all tiles of the puzzle
        numTilesPerUser = tilesPerUser;
        generateMainPuzzleTiles();

        for (int i = 0; i < numOfPlayers; ++i) {
            players.get(i).initTradeActions(numOfPlayers);
        }

        // execute only if player is a peer and not only a dump client
        if (isServer) {
            players.get(0).setServer(true); // Convension: the id of the server is zero

            // mix the tiles
            shuffleMainPuzzleTiles();

            // create the subpuzzle for each user
            createSubPuzzles();
        }
    }

    /**
     * Method for the server to init the Game singleton
     * After having executed this method the server can call the methods
     * getImageID();
     * getAllPlayer();
     * getPlayerTiles(pID)
     *
     * @param c            Context needed to select a random picture
     * @param numPlayers   create this many new players and do the splitting of the image accordingly
     * @param tilesPerUser
     */
    public void initGame(Context c, int numPlayers, int tilesPerUser) {

        //create numPlayers new Player
        for (int i = 0; i < numPlayers; i++) {
            addPlayer();
        }

        for (int i = 0; i < getNumOfPlayers(); ++i) {
            players.get(i).initTradeActions(getNumOfPlayers());
        }

        //selects a random picture and sets the class variable pictureForPuzzleID
        selectRandomPicture(c);

        // generate all tiles of the puzzle
        numTilesPerUser = tilesPerUser;
        generateMainPuzzleTiles();

        // execute only if player is a peer and not only a dump client
        players.get(0).setServer(true); // Convention: the id of the server is zero
        players.size();
        // mix the tiles
        shuffleMainPuzzleTiles();

        // create the subpuzzle for each user
        createSubPuzzles();
    }

    // Check if playerId has incoming trades from other players
    public boolean playerHasOpenTrades(int playerId, int incomingPlayerId) {
        for (int i = 0; i < getNumOfPlayers(); i++) {
            if (i != playerId && getPlayerById(incomingPlayerId).hasOpenTrade(playerId)) {
                return true;
            }
        }

        return false;
    }

    public Tile getTile(int tileId) {
        for (Tile t : puzzleTiles) {
            if (t.getId() == tileId) {
                return t;
            }
        }

        return null;
    }

    public void setPlayer(int pid, Player p) {
        if (pid >= 0 && p != null)
            players.set(pid, p);
    }

    public void shuffleMainPuzzleTiles() {
        Collections.shuffle(puzzleTiles);
    }

    public void generateMainPuzzleTiles() {

        //invoking this method makes the actual splitting of the source image to given number of chunks
        TilesGenerator utg = new TilesGenerator();
        int[] configs = utg.splitImage(pictureForPuzzle, getNumOfPlayers(), numTilesPerUser);
        puzzleTiles = utg.getChunkedImages();
    }

    public void createSubPuzzles() {
        int start,
                end = 0;
        int rows, cols;
        int numOfPlayers = getNumOfPlayers();

        rows = cols = (int) Math.sqrt(numTilesPerUser);
        for (int j = 0; j < numOfPlayers; j++) {
            start = end;
            end = end + numTilesPerUser;
            ArrayList<Tile> tiles = new ArrayList<>(puzzleTiles.subList(start, end));
            Grid<Tile> puzzle = new Grid<>(rows, cols, tiles);
            getPlayerById(j).setPuzzle(puzzle);
            // TODO spread tiles to other users
        }
    }

    public Grid<Tile> getPlayerTiles(int playerId) {
        if (playerId >= 0)
            return getPlayerById(playerId).getPuzzle();
        return null;
    }

    public int getNumOfPlayers() {
        if (players != null)
            return players.size();
        return 0;
    }

    public void startGame(Context context, int numPlayer, int playerId, int imageId, ArrayList<Integer> tileIds) {

        // Set the image based on the imageId
        selectImageFromId(context, imageId);

        // Create player stubs
        for (int i = 0; i < numPlayer; i++) {
            addPlayer();
        }

        for (int i = 0; i < numPlayer; ++i) {
            players.get(i).initTradeActions(numPlayer);
        }

        numTilesPerUser = tileIds.size();
        generateMainPuzzleTiles();
        setPlayerTiles(playerId, numTilesPerUser, tileIds);
    }

    public void setPlayerTiles(int playerId, int numTilesPerUser, ArrayList<Integer> tileIds) {
        int rows = (int) Math.sqrt(numTilesPerUser);
        int cols = rows;

        Grid<Tile> puzzle = new Grid<Tile>(rows, cols);

        for (int id : tileIds) {
            puzzle.add(getTile(id));
        }

        players.get(playerId).setPuzzle(puzzle);
    }


    public ArrayList<Player> getPlayers() {
        return players;
    }

    public int getPictureForPuzzleID() {
        return pictureForPuzzleID;
    }

    public void enableUserConnectivityIcon(int currNumPlayers, Activity a) {

        ImageView icon = null;
        Player p = null;
        Game game = Game.getInstance();
        switch (currNumPlayers) {
            case 1:
                icon = (ImageView) a.findViewById(R.id.user2);
                p = game.getPlayerById(1);
                break;
            case 2:
                icon = (ImageView) a.findViewById(R.id.user3);
                p = game.getPlayerById(2);
                break;
            case 3:
                icon = (ImageView) a.findViewById(R.id.user4);
                p = game.getPlayerById(3);
                break;
        }

        if (icon != null){
            icon.setImageDrawable(Player.getPlayerAvatar(a.getBaseContext(),currNumPlayers));
            icon.setVisibility(View.VISIBLE);
        }
    }
}
