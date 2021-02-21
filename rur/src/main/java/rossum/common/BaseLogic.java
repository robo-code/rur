package rossum.common;

import rossum.board.Board;

import static java.lang.Math.PI;

public abstract class BaseLogic implements ILogic {
    public final static double PI = java.lang.Math.PI;
    public final static double TWO_PI = 2 * java.lang.Math.PI;
    public final static double THREE_PI_OVER_TWO = 3 * java.lang.Math.PI / 2;
    public final static double PI_OVER_TWO = java.lang.Math.PI / 2;

    public abstract void turnLogic();

    protected Board board;

    public BaseLogic(Board board) {
        this.board = board;
    }

    public Board getBoard(){
        return board;
    }

    protected void init() {
        board.fire = 0;
    }
}
