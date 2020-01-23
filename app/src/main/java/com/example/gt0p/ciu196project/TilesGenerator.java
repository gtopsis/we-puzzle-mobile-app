package com.example.gt0p.ciu196project;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by gt0p on 7/10/2016.
 */
public class TilesGenerator {

    ArrayList<Tile> chunkedImages;

    // inputs: image, number of players, id of the subpuzzle, number of tiles per subpuzzle nad the directory in which the tiles will be saved
    // output: saved the tiles in initial dimensions in the specified directory
    public int[] splitImage(Bitmap image, int numPlayers, int numTilesPerSubpuzzle) {

        //For the number of rows and columns of the grid to be displayed
        int rows = 0, cols = 0;

        //For height and width of the small image chunks
        int chunkHeight, chunkWidth, chunkNumbers;

        chunkNumbers = numPlayers * numTilesPerSubpuzzle;
        //To store all the small image chunks in bitmap format in this list
        chunkedImages = new ArrayList<>(chunkNumbers);

        //Getting the scaled bitmap of the source image
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), true);

        if(numPlayers < 4){
            cols = numPlayers * ((int) Math.sqrt(numTilesPerSubpuzzle));
            rows = ((int) Math.sqrt(numTilesPerSubpuzzle));
        }else if(numPlayers == 4 ){
            rows = 2 * ((int) Math.sqrt(numTilesPerSubpuzzle));
            cols = 2 * ((int) Math.sqrt(numTilesPerSubpuzzle));
        }
        chunkHeight = image.getHeight()/rows;
        chunkWidth = image.getWidth()/cols;

        //xCoord and yCoord are the pixel positions of the image chunks
        int xCoord;
        int yCoord = 0;
        //String path = Environment.getExternalStorageDirectory().toString();
        for (int x = 0; x < rows; x++) {
            xCoord = 0;
            for (int y = 0; y < cols; y++) {
                Bitmap chuncedBitmap = Bitmap.createBitmap(scaledBitmap, xCoord, yCoord, chunkWidth, chunkHeight);

                int id = x * cols + y;
                Tile tile = new Tile(id, chuncedBitmap);
                chunkedImages.add(tile);

                xCoord += chunkWidth;
            }
            yCoord += chunkHeight;
        }

        int[] tileDim = {chunkWidth, chunkHeight};
        return tileDim;
    }

    public ArrayList<Tile> getChunkedImages() {
        return chunkedImages;
    }
}
