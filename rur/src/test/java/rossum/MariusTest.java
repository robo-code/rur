package rossum;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import robocode.control.events.RoundStartedEvent;
import robocode.control.events.TurnEndedEvent;
import robocode.control.snapshot.ITurnSnapshot;
import robocode.control.snapshot.RobotState;
import rossum.external.ExternalLogic;
import rossum.marius.MariusBoard;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class MariusTest extends RossumTestBed<Marius> {
    ExternalLogic externalLogic = new ExternalLogic();

    String self;
    String enemy;

    @Parameterized.Parameters(name = "{index}: Test with {0} and {1}")
    public static Iterable<Object[]> battles() {
        return Arrays.asList(new Object[][]{
                {"rossum.Marius", "sample.Crazy"},
                {"rossum.Marius", "sample.SittingDuck"},
                {"rossum.Marius", "sample.Walls"},
                {"rossum.Marius", "sample.Fire"},
        });
    }

    ITurnSnapshot prevTurnSnapshot;

    @Override
    public void onTurnEnded(TurnEndedEvent event) {
        ITurnSnapshot turn = event.getTurnSnapshot();
        if (turn.getTurn() == 1) {
            externalLogic.board = new MariusBoard();
        }

        if (externalLogic.processTurn(turn)) {
            boolean anyRobotDead = Arrays.stream(turn.getRobots()).anyMatch(r -> r.getState() == RobotState.DEAD);
            if (!anyRobotDead) {
                externalLogic.board.AssertNear(this.robotObject.board);
            }
        }
        prevTurnSnapshot = turn;
    }

    public MariusTest(String self, String enemy) {
        this.enemy = enemy;
        this.self = self;
    }

    @Override
    public boolean isEnableScreenshots() {
        return true;
    }

    @Override
    public int getNumRounds() {
        return 25;
    }

    @Override
    public String getEnemyName() {
        return enemy;
    }

    @Override
    public String getRobotName() {
        return self;
    }

    @Test
    public void run() {
        super.run();
    }
}
