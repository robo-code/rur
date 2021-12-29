package rossum.state;

import robocode.*;
import robocode.robotinterfaces.peer.IAdvancedRobotPeer;
import robocode.util.Utils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;

public class Board {
    public long time;
    public int round;
    public ArrayList<Turn> turns = new ArrayList<>();
    public Hashtable<Integer, Wave> waves = new Hashtable<Integer, Wave>();
    public BattleResults battleResults;

    public Turn currentTurn;
    public Turn previousTurn;
    public Turn previous2Turn;

    public int enemyNextBulletId = 0;
    public Wave enemyLastWave;
    public Robot prevEnemy;
    public Robot lastEnemy;
    public Robot enemy;

    public Myself self;
    public Myself prevSelf;

    // commands
    public double setFire;
    public double setMove;
    public double setBody;
    public double setGun;
    public double setRadar;

    public void AssertNear(Board actual) {
        Utils.assertEquals("time", time, actual.time);
        self.assertNear(actual.self);
        if (time > 2 && actual.enemy != null) {
            enemy.assertNear(actual.enemy);
            if (currentTurn.enemy.detectedWave == null) {
                Utils.assertEquals("there is no bullet", null, actual.currentTurn.enemy.detectedWave);
            } else {
                Utils.assertNotNull("there is a bullet", actual.currentTurn.enemy.detectedWave);
                currentTurn.enemy.detectedWave.assertNear(actual.currentTurn.enemy.detectedWave);
            }

            for (Integer bulletId : waves.keySet()) {
                Wave expectedWave = waves.get(bulletId);
                if (expectedWave.lastProjection.state != WaveState.GONE) {
                    Wave actualWave = actual.waves.get(bulletId);
                    Utils.assertNotNull("should detect all waves", actualWave);
                    expectedWave.assertNear(actualWave);
                }
            }

            for (Integer bulletId : currentTurn.waveProjections.keySet()) {
                WaveProjection expectedProjection = currentTurn.waveProjections.get(bulletId);
                if (expectedProjection.state != WaveState.GONE) {
                    WaveProjection actualProjection = actual.currentTurn.waveProjections.get(bulletId);
                    expectedProjection.assertNear(actualProjection);
                }
            }

            enemy.assertNear2(actual.enemy);
        }
        self.assertNear2(actual.self);
    }
}

