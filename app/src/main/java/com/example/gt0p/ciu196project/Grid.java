package com.example.gt0p.ciu196project;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Manu on 05.10.2016.
 * This class encapsulates a 2D gridView of elements
 * Row major layout:
 * |0|1|2|
 * |3|4|5|
 * |6|7|8|
 */

public class Grid<T> {
    private int rows;
    private int columns;
    private ArrayList<T> elements;

    public Grid(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;

        elements = new ArrayList<>(this.rows * this.columns);
    }

    public Grid(int rows, int columns, ArrayList<T> elements) {
        this.rows = rows;
        this.columns = columns;
        this.elements = elements;
    }

    public ArrayList<T> getElements() {
        return elements;
    }

    public T get(int index) {
        return elements.get(index);
    }

    // Row and column are 0-based
    public T get(int row, int column) {
        int index = getIndex(row, column);

        return elements.get(index);
    }

    public void add(T element) { elements.add(element); }

    public void add(int index, T element) {
        elements.add(index, element);
    }

    public void add(int row, int column, T element) {
        int index = getIndex(row, column);

        elements.add(index, element);
    }

    public void set(int index, T element) {
        elements.set(index, element);
    }

    public void set(int row, int column, T element) {
        int index = getIndex(row, column);

        elements.set(index, element);
    }

    public void swap(int aIndex, int bIndex) {
        Collections.swap(elements, aIndex, bIndex);
    }

    public int size() {
        return rows * columns;
    }

    public int getRow(int index) {
        int row = index / columns;

        return row;
    }

    public int getColumn(int index) {
        int column = index % columns;

        return column;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() { return rows; }

    public int getIndex(int row, int column) {
        return row * columns + column;
    }


}
