package rossum;

import org.junit.Assert;
import org.junit.Test;
import robocode.control.events.TurnEndedEvent;

public class DirectTest extends RobocodeTestBed<RurWalls> {
    @Override
    public String getRobotName() {
        return "rossum.RurWalls";
    }

    @Override
    public void onTurnEnded(TurnEndedEvent event) {
        int turn = event.getTurnSnapshot().getTurn();
        if(turn==1){
            Assert.assertEquals(this.robotObject.peek, false);
        }
        if(turn==100){
            Assert.assertEquals(this.robotObject.peek, true);
        }
    }

    @Test
    public void run() {
        super.run();
    }
}
