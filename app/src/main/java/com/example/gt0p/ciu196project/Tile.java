package com.example.gt0p.ciu196project;

import android.graphics.Bitmap;

/**
 * Created by Manu on 05.10.2016.
 * This class represents a single sub piece of the puzzle
 * Each tile has an unique ID and a path pointing to the image
 */

public class Tile {
    private int id;
    private Bitmap image;
    private boolean selected = false;

    public Tile(int id, Bitmap image) {
        this.id = id;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Bitmap getImage() {
        return image;
    }
}
