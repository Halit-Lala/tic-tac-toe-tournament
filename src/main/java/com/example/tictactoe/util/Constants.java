package com.example.tictactoe.util;

public class Constants {
    
    public static final int MAX_NAME_LENGTH = 50;
    
    public static final int BOARD_SIZE = 3;

    public static final int BOARD_CELLS_LENGTH = 9;

    public static final String EMPTY_BOARD = "---------";

    public static final char EMPTY_CELL = '-';

    public static final int[][] WIN_LINES = {
        {0,1,2},{3,4,5},{6,7,8},   // rows
        {0,3,6},{1,4,7},{2,5,8},   // columns
        {0,4,8},{2,4,6}            // diagonals
    };

}
