package rossum.marius;

import robocode.util.Utils;
import rossum.common.BaseLogic;

public class MariusLogic extends BaseLogic {
    protected MariusBoard board;

    public MariusLogic(MariusBoard board) {
        super(board);
        this.board=board;
        board.movingForward = true;
        board.move = 40000;
    }

    public void reverseDirection() {
        if (board.movingForward) {
            board.move = -40000;
            board.movingForward = false;
        } else {
            board.move = 40000;
            board.movingForward = true;
        }
    }

    @Override
    public void turnLogic() {
        init();
        board.body = board.random.nextDouble() * TWO_PI - PI;
        if (board.currentTurn.hitWall != null || ( board.currentTurn.hitOther !=null && board.currentTurn.hitOther.isMyFault())) {
            reverseDirection();
        }
    }
}
