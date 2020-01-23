package com.example.gt0p.ciu196project;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

/**
 * Created by gt0p on 10/10/2016.
 */
public class Player {
    private int id;
    private boolean isServer = false;
    private Grid<Tile> puzzle;

    public TradeAction[] tradeActions;

    public Player(int id){
        this.id = id;
    }

    public Player(int id, boolean isServer) {
        this.id = id;
        this.isServer = isServer;
    }

    public Player(int id, boolean isServer, Grid<Tile> puzzle) {
        this.id = id;
        this.isServer = isServer;
        this.puzzle = puzzle;
    }

    public void initTradeActions(int numPlayers) {
        tradeActions = new TradeAction[numPlayers];

        // Initialize the trade actions with the player's own id
        for(int i = 0; i < tradeActions.length; i++) {
            tradeActions[i] = new TradeAction(id);
        }
    }

    public boolean hasOpenTrade(int playerId) {
        // Check if we have an open trade with playerId
        return tradeActions[playerId].getTileAId() != -1;

    }

    // Check if this tile has already been offered to another player
    public boolean isTileOffered(int tileId) {
        for(TradeAction t : tradeActions) {
            if(t.getTileAId() == tileId) {
                return true;
            }
        }

        return false;
    }

    // We are playerB / tileB
    public void acceptOffer(TradeAction trade) {
        Game game = Game.getInstance();
        // Find own tile
        Tile ownTile = findTile(trade.getTileBId());

        Tile offeredTile = game.getTile(trade.getTileAId());
        puzzle.set(findGridIndex(ownTile.getId()), offeredTile);

        // Reset the trade for this player
        tradeActions[trade.getPlayerAId()].reset();
    }

    public void receiveOffer(TradeAction trade) {
        tradeActions[trade.getPlayerBId()] = trade;
    }

    public boolean sendOffer(TradeAction trade) {
        // Check if this tile has already been offered
        if(!isTileOffered(trade.getTileAId())) {
            tradeActions[trade.getPlayerBId()] = trade;

            return true;
        }

        return false;
    }

    public void cancelOffer(TradeAction trade) {
        tradeActions[trade.getPlayerBId()].reset();
    }

    public Tile findTile(int tileId) {
        // Find tile in grid with tileId
        for(Tile t : puzzle.getElements()) {
            if(t.getId() == tileId) {
                return t;
            }
        }

        return null;
    }

    public int findGridIndex(int tileId) {
        for(int i = 0; i < puzzle.size(); i++) {
            if(puzzle.get(i).getId() == tileId) {
                return i;
            }
        }

        return -1;
    }

    public boolean isServer() {
        return isServer;
    }

    public void setServer(boolean server) {
        isServer = server;
    }

    public Grid<Tile> getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(Grid<Tile> puzzle) {
        this.puzzle = puzzle;
    }

    public int getId() {
        return id;
    }

    public int getPlayerColor(Context context) {
        return Player.getPlayerColor(context, id);
    }

    public static Drawable getPlayerAvatar(Context context, int playerId) {
        switch(playerId) {
            case 0:
                return ContextCompat.getDrawable(context, R.drawable.dog);
            case 1:
                return ContextCompat.getDrawable(context, R.drawable.wolf);
            case 2:
                return ContextCompat.getDrawable(context, R.drawable.panda);
            case 3:
                return ContextCompat.getDrawable(context, R.drawable.frog);
            default:
                return ContextCompat.getDrawable(context, R.drawable.ic_avatar_white);
        }
    }

    public static int getPlayerColor(Context context, int playerId) {
        switch(playerId) {
            case 0:
                return context.getResources().getColor(R.color.colorAvatarDog);

            case 1:
                return context.getResources().getColor(R.color.colorAvatarWolf);

            case 2:
                return context.getResources().getColor(R.color.colorAvatarPanda);

            case 3:
                return context.getResources().getColor(R.color.colorAvatarFrog);

            default:
                return context.getResources().getColor(R.color.black);
        }
    }

    public boolean isSubPuzzleCompleted(){
        int prevTileId = 0, nextTileId=0;

        Game game = Game.getInstance();
        int numPlayers = game.getNumOfPlayers();
        int idDiff_1 = 0;
        int idDiff_2 = 0;

        switch (numPlayers){
            case 2:
                idDiff_1 = 1;
                idDiff_2 = 4;
                break;
            case 3:
                idDiff_1 = 1;
                idDiff_2 = 7;
                break;
            case 4:
                idDiff_1 = 1;
                idDiff_2 = 4;
                break;
        }

        prevTileId = getPuzzle().get(0).getId();
        Log.d("tag", String.valueOf(prevTileId));
        for(int i=1;i< getPuzzle().size();++i){
            prevTileId=nextTileId;
            nextTileId=getPuzzle().get(i).getId();

            Log.d("tag", String.valueOf(nextTileId));


            if(nextTileId != prevTileId + idDiff_1 && nextTileId != prevTileId + idDiff_2) {
                Log.d("tag", "====================");
                return false;
            }
        }
        Log.d("tag", "====================");
        return true;
    }
}
