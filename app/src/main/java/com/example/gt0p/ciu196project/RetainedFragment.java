package com.example.gt0p.ciu196project;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Created by Manu on 11.10.2016.
 */


public class RetainedFragment extends Fragment {
    // Hold a grid to be available if activity is destroyed
    private Player player;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setRetainInstance(true);
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}