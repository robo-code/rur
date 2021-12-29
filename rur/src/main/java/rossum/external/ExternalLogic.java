package rossum.external;

import robocode.Rules;
import robocode.control.snapshot.*;
import robocode.util.Utils;
import rossum.marius.MariusLogic;
import rossum.state.*;

import java.awt.geom.Point2D;

import static rossum.marius.MariusLogic.INACTIVITY_ZAP;
import static rossum.marius.MariusLogic.absoluteBearing;

public class ExternalLogic {
    public boolean shouldAssert = true;
    public Board board;

    public boolean processTurn(ITurnSnapshot turnSnapshot) {
        IRobotSnapshot self = turnSnapshot.getRobots()[0];
        IRobotSnapshot enemy = turnSnapshot.getRobots()[1];

        board.time = turnSnapshot.getTurn();
        board.round = turnSnapshot.getRound();
        if (self.getState() == RobotState.DEAD || enemy.getState() == RobotState.DEAD) {
            return false;
        }

        board.previous2Turn = board.previousTurn;
        board.previousTurn = board.currentTurn;
        board.currentTurn = Turn.createTurn(turnSnapshot.getRound(), board.time);
        board.turns.add(board.currentTurn);

        board.prevSelf = board.self;
        board.prevEnemy = board.lastEnemy;

        board.enemy = createEnemy(enemy, self, board.time);
        board.self = createSelf(self, enemy, board.time);
        if (board.time == 1) {
            board.enemyNextBulletId = 1 + (1 * 100000) + (turnSnapshot.getRound() * 100000000);
        }
        board.lastEnemy = board.enemy;
        board.currentTurn.self = board.self;
        board.currentTurn.enemy = board.enemy;
        if (board.time < 3) {
            return false;
        }
        if (board.prevEnemy == null) {
            return false;
        }

        for (IBulletSnapshot bullet : turnSnapshot.getBullets()) {
            processBullet(bullet);
        }
        for (Wave wave : board.waves.values()) {
            // capture passing event after bullet hit something
            if (wave.passingEndTime == null && board.round == wave.firedRound) {
                WaveProjection projection = wave.projectWave(board);
                wave.lastProjection = projection;
            }
        }

        processEnergyDrop();

        return true;
    }

    private void processBullet(IBulletSnapshot bullet) {
        int bulletId = bullet.getBulletId();
        double bulletPower = bullet.getPower();
        BulletState bulletState = bullet.getState();
        int ownerIndex = bullet.getOwnerIndex();
        int frame = bullet.getFrame();

        if (bulletState == BulletState.EXPLODED || bulletState == BulletState.INACTIVE) {
            // board.waves.remove(bulletId);
            return;
        }

        Wave wave;
        WaveProjection projection;
        boolean isNewBullet = !board.waves.containsKey(bulletId);
        Robot currentVictim = ownerIndex == 0 ? board.currentTurn.enemy : board.currentTurn.self;
        Robot prevVictim = ownerIndex == 0 ? board.previousTurn.enemy : board.previousTurn.self;
        Robot prev2Victim = ownerIndex == 0 ? board.previous2Turn.enemy : board.previous2Turn.self;
        Robot currentVillain = ownerIndex != 0 ? board.currentTurn.enemy : board.currentTurn.self;
        Robot prevVillain = ownerIndex != 0 ? board.previousTurn.enemy : board.previousTurn.self;
        Robot prev2Villain = ownerIndex != 0 ? board.previous2Turn.enemy : board.previous2Turn.self;

        if (isNewBullet) {
            currentVillain.firedPower = bulletPower;
            wave = Wave.createWave(bulletId, ownerIndex, bulletPower, board.round,board.time - 1,
                    prev2Victim,
                    prevVictim,
                    currentVictim,
                    prev2Villain,
                    prevVillain,
                    currentVillain
            );
            board.waves.put(bulletId, wave);
            currentVillain.detectedWave = wave;
        } else {
            wave = board.waves.get(bulletId);
        }
        projection = WaveProjection.create(wave, board.time, prevVictim, bullet.getHeading());

        if (bulletState == BulletState.HIT_VICTIM && frame == 0) {
            currentVictim.hitByBulletPower = wave.power;
            currentVictim.hitByBulletId = wave.id;
            projection.wave.passedVictim = currentVictim;
            projection.wave.passedOwner = currentVillain;
        }

        if (wave.passingEndTime == null) {
            board.currentTurn.waveProjections.put(bulletId, projection);
        }
        if (frame == 0) {
            if (bulletState == BulletState.HIT_VICTIM && frame == 0) {
                wave.outcome = WaveState.HIT_VICTIM;
                wave.hitTime = board.time;
                wave.finalProjection = projection;
            } else if (bulletState == BulletState.HIT_BULLET && frame == 0) {
                wave.outcome = WaveState.HIT_BULLET;
                wave.hitTime = board.time;
                wave.finalProjection = projection;
            } else if (bulletState == BulletState.HIT_WALL && frame == 0) {
                wave.outcome = WaveState.HIT_WALL;
                wave.hitTime = board.time;
                wave.finalProjection = projection;
            }
        } else {
            if (bulletState == BulletState.HIT_VICTIM || bulletState == BulletState.HIT_WALL || bulletState == BulletState.HIT_BULLET) {
                projection.state = WaveState.GONE;
            }
        }

        if (shouldAssert) {
            if (frame == 0) {
                if (bulletState == BulletState.HIT_VICTIM) {
                    Utils.assertEquals("Should hit", WaveState.PASSING, projection.state);
                }
                if (projection.state == WaveState.PASSING && bulletState != BulletState.HIT_WALL) {
                    Utils.assertEquals("Should hit", BulletState.HIT_VICTIM, bulletState);
                }
            }
            if (bulletState == BulletState.MOVING) {
                Utils.assertNear("Bullet power", bullet.getPower(), projection.wave.power);
                Utils.assertEquals("Bullet age", frame, (int) projection.age);
                Utils.assertNear("Bullet Heading", bullet.getHeading(), projection.bulletHeading);
                Utils.assertNear("Bullet X", bullet.getX(), projection.idealBullet.x2);
                Utils.assertNear("Bullet Y", bullet.getY(), projection.idealBullet.y2);
            }
        }
    }

    private void processEnergyDrop() {
        double enemyPrevEnergy = board.prevEnemy == null ? 100.0 : board.prevEnemy.energy;
        double enemyPowerDrop = enemyPrevEnergy - board.enemy.energy;
        double selfPrevEnergy = board.prevSelf == null ? 100.0 : board.prevSelf.energy;
        double selfPowerDrop = selfPrevEnergy - board.self.energy;
        board.self.rawEnergyDrop = selfPowerDrop;
        board.enemy.rawEnergyDrop = enemyPowerDrop;

        if (board.enemy.hitOther && board.enemy.velocity == 0) {
            enemyPowerDrop -= MariusLogic.ROBOT_HIT_DAMAGE;
            selfPowerDrop -= MariusLogic.ROBOT_HIT_DAMAGE;
        }
        if (board.self.hitOther && board.self.velocity == 0) {
            enemyPowerDrop -= MariusLogic.ROBOT_HIT_DAMAGE;
            selfPowerDrop -= MariusLogic.ROBOT_HIT_DAMAGE;
        }
        if (board.enemy.hitByBulletPower != 0) {
            enemyPowerDrop -= Rules.getBulletDamage(board.enemy.hitByBulletPower);
            selfPowerDrop += Rules.getBulletHitBonus(board.enemy.hitByBulletPower);
        }
        if (board.self.hitByBulletPower != 0) {
            enemyPowerDrop += Rules.getBulletHitBonus(board.self.hitByBulletPower);
            selfPowerDrop -= Rules.getBulletDamage(board.self.hitByBulletPower);
        }
        if (board.self.hitWall) {
            double selfVelocity = Math.signum(board.prevSelf.velocity) * Math.min(8, Math.abs(board.prevSelf.velocity) + 1);
            selfPowerDrop -= Rules.getWallHitDamage(selfVelocity);
        }
        if (board.enemy.hitWall) {
            double enemyVelocity = Math.signum(board.prevEnemy.velocity) * Math.min(8, Math.abs(board.prevEnemy.velocity) + 1);
            enemyPowerDrop -= Rules.getWallHitDamage(enemyVelocity);
        }
        if (board.self.firedPower != 0) {
            selfPowerDrop -= board.self.firedPower;
        }
        if (board.enemy.firedPower != 0) {
            enemyPowerDrop -= board.enemy.firedPower;
        }

        board.self.inactiveZap = board.enemy.inactiveZap = (selfPowerDrop >= INACTIVITY_ZAP && Utils.isNear(enemyPowerDrop, INACTIVITY_ZAP)) || (enemyPowerDrop >= INACTIVITY_ZAP && Utils.isNear(selfPowerDrop, INACTIVITY_ZAP));
        if (board.enemy.inactiveZap) {
            enemyPowerDrop -= INACTIVITY_ZAP;
            selfPowerDrop -= INACTIVITY_ZAP;
        }

        if (Utils.isNear(selfPowerDrop, 0)) {
            selfPowerDrop = 0;
        } else if (Utils.isNear(selfPowerDrop, Math.round(selfPowerDrop))) {
            selfPowerDrop = Math.round(selfPowerDrop);
        }
        board.self.energyDrop = selfPowerDrop;

        if (Utils.isNear(enemyPowerDrop, 0)) {
            enemyPowerDrop = 0;
        } else if (Utils.isNear(enemyPowerDrop, Math.round(enemyPowerDrop))) {
            enemyPowerDrop = Math.round(enemyPowerDrop);
        }
        board.enemy.energyDrop = enemyPowerDrop;
    }

    public static Myself createSelf(IRobotSnapshot self, IRobotSnapshot enemy, long time) {
        Myself robot = new Myself();
        robot.name = self.getName();
        robot.x = self.getX();
        robot.y = self.getY();
        robot.velocity = self.getVelocity();
        robot.body = self.getBodyHeading();
        robot.energy = self.getEnergy();
        robot.gunHeat = self.getGunHeat();
        robot.time = time;

        robot.gun = self.getGunHeading();
        robot.radar = self.getRadarHeading();

        robot.time = time;

        if (self.getState() == RobotState.DEAD) {
            robot.isDead = true;
        }
        if (self.getState() == RobotState.HIT_WALL) {
            robot.hitWall = true;
        }
        if (enemy.getState() == RobotState.HIT_ROBOT || self.getState() == RobotState.HIT_ROBOT) {
            robot.hitOther = true;
        }

        return robot;
    }

    public static Robot createEnemy(IRobotSnapshot enemy, IRobotSnapshot self, long time) {
        Robot robot = new Robot();
        robot.name = enemy.getName();
        robot.x = enemy.getX();
        robot.y = enemy.getY();
        robot.velocity = enemy.getVelocity();
        robot.body = enemy.getBodyHeading();
        robot.energy = enemy.getEnergy();
        robot.gunHeat = enemy.getGunHeat();

        robot.time = time;

        if (enemy.getState() == RobotState.DEAD) {
            robot.isDead = true;
        }
        if (enemy.getState() == RobotState.HIT_WALL) {
            robot.hitWall = true;
        }
        if (enemy.getState() == RobotState.HIT_ROBOT || self.getState() == RobotState.HIT_ROBOT) {
            robot.hitOther = true;
        }

        return robot;
    }

}
