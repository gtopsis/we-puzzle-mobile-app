package com.example.gt0p.ciu196project;

/**
 * Created by Manu on 17.10.2016.
 */

public class SwapAction {
    private Tile tileA;
    private Tile tileB;

    // Returns true if the tile has been added
    public boolean addTile(Tile tile) {

        // Don't add the tile if the tile is already part of the action
        if(containsTile(tile)) {
            return false;
        }

        // Assign the tile to the free position
        if(tileA == null) {
            tileA = tile;
            tileA.setSelected(true);
            return true;
        }

        if(tileB == null) {
            tileB = tile;
            tileB.setSelected(true);
            return true;
        }

        return false;
    }

    // Returns true if the tile has been removed
    public boolean removeTile(Tile tile) {
        if(tileA != null && tileA.getId() == tile.getId()) {
            tileA.setSelected(false);
            tileA = null;
            return true;
        } else if(tileB != null && tileB.getId() == tile.getId()) {
            tileB.setSelected(false);
            tileB = null;
            return true;
        }

        return false;
    }

    public boolean isValid() {
        return tileA != null && tileB != null;

    }

    // Checks if the tile is part of the action
    public boolean containsTile(Tile tile) {
        return containsTile(tile.getId());
    }

    // Checks if the tile is part of the action
    public boolean containsTile(int tileId) {
        if (tileA != null && tileA.getId() == tileId) {
            return true;
        } else if(tileB != null && tileB.getId() == tileId) {
            return true;
        }

        return false;
    }

    // Returns the tile of the tileId if it is part of the action or NULL
    public Tile getTile(int tileId) {
        if(tileA != null && tileA.getId() == tileId) {
            return tileA;
        } else if (tileB != null && tileB.getId() == tileId) {
            return tileB;
        }

        return null;
    }

    public Tile getTileA() {
        return tileA;
    }

    public Tile getTileB() {
        return tileB;
    }

    public void reset() {
        if(tileA != null) tileA.setSelected(false);
        if(tileB != null) tileB.setSelected(false);

        tileA = null;
        tileB = null;
    }
}
