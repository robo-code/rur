package rossum.board;

import robocode.*;

public class Turn {
    public Turn(RobotStatus status) {
        this.status = status;
    }

    public RobotStatus status;
    public SkippedTurnEvent skipped;
    public RoundEndedEvent roundEnded;
    public HitWallEvent hitWall;
    public HitRobotEvent hitOther;
}
