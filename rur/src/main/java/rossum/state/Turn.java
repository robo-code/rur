package rossum.state;

import robocode.*;

import java.util.Hashtable;

public class Turn {
    public Myself self;
    public Robot enemy;
    public SkippedTurnEvent skipped;
    public RoundEndedEvent roundEnded;
    public HitByBulletEvent hitByBullet;
    public HitWallEvent hitWall;
    public HitRobotEvent hitOther;
    public DeathEvent death;

    public Hashtable<Integer, WaveProjection> waveProjections = new Hashtable<>();
    public long time;
    public long round;

    @Override
    public String toString() {
        return time + " " +
                (skipped != null ? "skipped " : "") +
                (roundEnded != null ? "roundEnded " : "") +
                (hitOther != null ? "hitOther " : "") +
                (hitByBullet != null ? "hitByBullet " : "") +
                (hitWall != null ? "hitWall " : "") +
                (death != null ? "hitWall " : "")
                ;
    }

    public static Turn createTurn(long round,long time) {
        Turn turn = new Turn();
        turn.round=round;
        turn.time = time;
        return turn;
    }
}
