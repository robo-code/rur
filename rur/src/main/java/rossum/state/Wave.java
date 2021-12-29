package rossum.state;

import robocode.Rules;
import robocode.util.Utils;
import rossum.marius.MariusLogic;

public class Wave {
    public Integer id;
    public double velocity;
    public double power;
    public int firedRound;
    public long firedTime;
    public int ownerIndex;

    public Robot aimedOwner;
    public Robot aimedVictim;
    public Robot firedOwner;
    public Robot firedVictim;
    public Robot detectedOwner;
    public Robot detectedVictim;
    public Robot passedOwner;
    public Robot passedVictim;

    public WaveProjection finalProjection;
    public WaveProjection lastProjection;
    public long hitTime;
    public Long passingStartTime;
    public Long passingEndTime;
    public WaveState outcome = WaveState.MOVING;

    public static Wave createWave(int bulletId, int ownerIndex, double bulletPower, int firedRound, long firedTime,
                                  Robot aimedVictim,
                                  Robot firedVictim,
                                  Robot detectedVictim,
                                  Robot aimedOwner,
                                  Robot firedOwner,
                                  Robot detectedOwner) {
        Wave wave = new Wave();

        wave.id = bulletId;
        wave.ownerIndex = ownerIndex;
        wave.velocity = Rules.getBulletSpeed(bulletPower);

        wave.power = bulletPower;

        wave.firedVictim = firedVictim;
        wave.firedOwner = firedOwner;
        wave.aimedVictim = aimedVictim;
        wave.aimedOwner = aimedOwner;
        wave.detectedOwner = detectedOwner;
        wave.detectedVictim = detectedVictim;
        wave.firedTime = firedTime;
        wave.firedRound = firedRound;

        return wave;
    }

    public void assertNear(Wave actual) {
        Utils.assertEquals("id", id, actual.id);
        Utils.assertEquals("ownerIndex", ownerIndex, actual.ownerIndex);
        Utils.assertEquals("firedTime", firedTime, actual.firedTime);
        firedOwner.assertNear(actual.firedOwner);
        firedVictim.assertNear(actual.firedVictim);
        aimedOwner.assertNear(actual.aimedOwner);
        aimedVictim.assertNear(actual.aimedVictim);
        Utils.assertNear("velocity", velocity, actual.velocity);
    }

    @Override
    public String toString() {
        return id + " (" + power + ") Fired at " + firedTime + " from  X" + (int) firedOwner.x + " Y" + (int) firedOwner.y;
    }

    public WaveProjection projectWave(Board board) {
        WaveProjection projection = WaveProjection.create(this, board.time, ownerIndex == 0 ? board.prevEnemy : board.prevSelf, null);
        if (projection.state == WaveState.PASSING) {
            if (projection.wave.passedVictim == null) {
                projection.wave.passedVictim = ownerIndex == 0 ? board.prevEnemy : board.prevSelf;
                projection.wave.passedOwner = ownerIndex == 0 ? board.prevSelf : board.prevEnemy;
            }
            if (lastProjection == null || lastProjection.state == WaveState.MOVING) {
                passingStartTime = board.time;
            }
        } else if (projection.state == WaveState.GONE) {
            if (lastProjection == null || lastProjection.state == WaveState.PASSING) {
                passingEndTime = board.time;
            }
        }

        return projection;
    }

}
