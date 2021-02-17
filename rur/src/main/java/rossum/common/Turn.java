package rossum.common;

import robocode.RobotStatus;

public class Turn {
    public Turn(RobotStatus status) {
        this.status = status;
    }

    public RobotStatus status;
    public boolean isSkipped;
    public boolean isRoundEnded;
}
