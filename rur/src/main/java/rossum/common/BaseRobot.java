package rossum.common;


import robocode.robotinterfaces.IAdvancedEvents;
import robocode.robotinterfaces.IAdvancedRobot;
import robocode.robotinterfaces.IBasicEvents;
import robocode.robotinterfaces.peer.IAdvancedRobotPeer;
import robocode.robotinterfaces.peer.IBasicRobotPeer;
import rossum.board.Board;

import java.io.PrintStream;


public abstract class BaseRobot implements IAdvancedRobot, Runnable {
    public Receiver receiver;
    protected Board board;
    protected ILogic logic;

    public BaseRobot(ILogic logic) {
        this.board = logic.getBoard();
        this.logic = logic;
        receiver = new Receiver(board);
    }

    @Override
    public void run() {
        while (board.currentTurn.roundEnded == null) {
            logic.turnLogic();

            board.peer.setMove(board.move);
            board.out.println("move"+board.move);
            if (board.fire != 0) {
                board.peer.setFire(board.fire);
            }
            board.peer.setTurnBody(board.body);
            board.peer.setTurnGun(board.gun);
            board.peer.setTurnRadar(board.radar);

            board.peer.execute();
        }
    }

    @Override
    public IAdvancedEvents getAdvancedEventListener() {
        return receiver;
    }

    @Override
    public Runnable getRobotRunnable() {
        return this;
    }

    @Override
    public IBasicEvents getBasicEventListener() {
        return receiver;
    }

    @Override
    public void setPeer(IBasicRobotPeer peer) {
        this.board.peer = (IAdvancedRobotPeer) peer;
    }

    @Override
    public void setOut(PrintStream printStream) {
        this.board.out = printStream;
    }
}

