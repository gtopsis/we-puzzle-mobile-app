package com.example.gt0p.ciu196project;

import android.os.Bundle;

/**
 * Created by Manu on 13.10.2016.
 *
 * This class encapsulates a single trade action between two players
 */

public class TradeAction {
    private int playerAId = -1; // The player who sent the trade offer
    private int playerBId = -1; // The player we want to trade with

    private int tileAId = -1; // The tile we offer
    private int tileBId = -1; // The tile the other player returns

    public TradeAction() {

    }

    public TradeAction(int playerAId) {
        this.playerAId = playerAId;
    }

    public TradeAction(int playerAId, int playerBId, int tileAId) {
        this.playerAId = playerAId;
        this.playerBId = playerBId;
        this.tileAId = tileAId;
    }

    public void sendOffer(int playerAId, int tileAId, int playerBId) {

        // Information about ourselves
        this.tileAId = tileAId;

        // We only know the player id of the other player
        this.playerBId = playerBId;
    }

    public void setTileBId(int tileBId) {
        // Complete trade information with the tile id the other player sends back
        this.tileBId = tileBId;
    }

    public void setPlayerBId(int playerBId) {
        this.playerBId = playerBId;
    }

    public void setPlayerAId(int playerAId) {
        this.playerAId = playerAId;
    }

    public boolean isValid() {
        return playerAId != -1
                && playerBId != -1
                && tileAId != -1
                && tileBId != -1;
    }

    public int getPlayerAId() {
        return playerAId;
    }

    public int getPlayerBId() {
        return playerBId;
    }

    public int getTileAId() {
        return tileAId;
    }

    public int getTileBId() {
        return tileBId;
    }

    public void setTileAId(int tileAId) {
        this.tileAId = tileAId;
    }

    public void reset() {
        this.playerBId = -1;
        this.tileAId = -1;
        this.tileBId = -1;
    }

    public TradeAction clone() {
        TradeAction x = new TradeAction(playerAId, playerBId, tileAId);

        return x;
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();

        b.putInt("playerAId", playerAId);
        b.putInt("playerBId", playerBId);
        b.putInt("tileAId", tileAId);
        b.putInt("tileBId", tileBId);

        return b;
    }

    public static TradeAction fromBundle(Bundle b) {
        TradeAction t = new TradeAction();

        t.setPlayerAId(b.getInt("playerAId"));
        t.setPlayerBId(b.getInt("playerBId"));
        t.setTileAId(b.getInt("tileAId"));
        t.setTileBId(b.getInt("tileBId"));

        return t;
    }

    public String toOfferString() {
        String s = "offer;";

        s += "" + playerAId + ";" + playerBId + ";" + tileAId;

        return s;
    }

    public String toAcceptString() {
        String s = "accept;";

        s += "" + playerAId + ";" + playerBId + ";" + tileAId + ";" + tileBId;

        return s;
    }

    public String toReject() {
        String s = "accept;";

        s += "" + playerAId + ";" + playerBId;

        return s;
    }
}
