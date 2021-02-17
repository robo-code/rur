package rossum.common;

import robocode.*;
import robocode.robotinterfaces.peer.IAdvancedRobotPeer;

import java.io.PrintStream;
import java.util.Hashtable;
import java.util.LinkedList;

public class Board {
    public IAdvancedRobotPeer peer;
    public PrintStream out;
    public LinkedList<Turn> turns;
    public Turn currentTurn;
    public Turn previousTurn;
    public long time;
    public BattleResults battleResults;
    public Hashtable<Integer, Bullet> myActiveBullets = new Hashtable<Integer, Bullet>();
    public Hashtable<Integer, Bullet> myInactiveBullets = new Hashtable<Integer, Bullet>();
    public ScannedRobotEvent otherRobot;


}

