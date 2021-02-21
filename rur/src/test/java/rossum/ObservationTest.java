package rossum;


import static org.junit.Assert.assertTrue;

import org.junit.Test;
import robocode.control.events.BattleCompletedEvent;
import robocode.control.events.TurnEndedEvent;
import robocode.control.snapshot.IRobotSnapshot;


/**
 * Tests that sample.Walls moves to all four corners.
 *
 * @author Philip Johnson (original)
 * @author Pavel Savara (contributor)
 */
public class ObservationTest extends RobocodeTestBed {
    /**
     * True if the robot visited this corner during the test case.
     */
    boolean visitedUpperLeft = false;

    /**
     * True if the robot visited this corner during the test case.
     */
    boolean visitedUpperRight = false;

    /**
     * True if the robot visited this corner during the test case.
     */
    boolean visitedLowerLeft = false;

    /**
     * True if the robot visited this corner during the test case.
     */
    boolean visitedLowerRight = false;

    /**
     * Specifies that rossum.RurWalls is tested robot.
     *
     * @return robot name.
     */
    @Override
    public String getRobotName() {
        return "rossum.RurWalls";
    }

    /**
     * Specifies that SittingDuck is the enemy.
     *
     * @return robot name.
     */
    @Override
    public String getEnemyName() {
        return "sample.SittingDuck";
    }

    /**
     * This test runs for 1 round.
     *
     * @return The number of rounds.
     */
    @Override
    public int getNumRounds() {
        return 1;
    }

    /**
     * After each turn, check to see if we're at a corner.  If so, set the corresponding flag.
     *
     * @param event Info about the current state of the battle.
     */
    @Override
    public void onTurnEnded(TurnEndedEvent event) {
        IRobotSnapshot robot = event.getTurnSnapshot().getRobots()[0];
        double xPos = robot.getX();
        double yPos = robot.getY();

        if ((xPos < 40) && (yPos < 40)) {
            visitedUpperLeft = true;
        }
        int height = battleFieldSpec.getHeight();
        int width = battleFieldSpec.getWidth();
        if ((xPos < 40 && (yPos > (height - 40)))) {
            visitedLowerLeft = true;
        }
        if ((xPos > (width - 40)) && (yPos < 40)) {
            visitedUpperRight = true;
        }
        if ((xPos > (width - 40) && (yPos > (height - 40)))) {
            visitedLowerRight = true;
        }
    }

    /**
     * After the battle, check to see that we've visited the corners.
     *
     * @param event Details about the completed battle.
     */
    @Override
    public void onBattleCompleted(BattleCompletedEvent
                                          event) {
        assertTrue("Check UpperLeft", visitedUpperLeft);
        assertTrue("Check LowerLeft", visitedLowerLeft);
        assertTrue("Check UpperRight", visitedUpperRight);
        assertTrue("Check LowerRight", visitedLowerRight);
    }

    @Test
    public void run() {
        super.run();
    }
}