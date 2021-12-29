package rossum.marius;

import robocode.Rules;
import robocode.util.Utils;
import rossum.state.Robot;
import rossum.state.Wave;
import rossum.state.WaveProjection;
import rossum.state.WaveState;

import java.awt.geom.Point2D;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static robocode.Rules.MAX_BULLET_POWER;
import static robocode.Rules.MIN_BULLET_POWER;

public class MariusLogic {
    protected MariusBoard board;

    public MariusLogic() {
    }

    public void setBoard(MariusBoard board) {
        this.board = board;
        board.movingForward = true;
        board.setMove = 40000;
    }


    public void turnLogic() {
        init();

        detectionLogic();
        moveLogic();
        bodyLogic();
        radarLogic();
        gunLogic();
    }

    public boolean detectionLogic() {
        if (board.time < 3) {
            return false;
        }
        if (board.enemy == null || board.prevEnemy == null) {
            return false;
        }
        if (board.self.isDead || board.enemy.isDead) {
            return false;
        }

        double enemyPrevEnergy = board.prevEnemy == null ? 100.0 : board.prevEnemy.energy;
        double enemyPowerDrop = enemyPrevEnergy - board.enemy.energy;
        double selfPrevEnergy = board.prevSelf == null ? 100.0 : board.prevSelf.energy;
        double selfPowerDrop = selfPrevEnergy - board.self.energy;
        board.self.rawEnergyDrop = selfPowerDrop;
        board.enemy.rawEnergyDrop = enemyPowerDrop;

        if (board.enemy.hitOther && board.enemy.velocity == 0) {
            enemyPowerDrop -= ROBOT_HIT_DAMAGE;
            selfPowerDrop -= ROBOT_HIT_DAMAGE;
        }
        if (board.self.hitOther && board.self.velocity == 0) {
            enemyPowerDrop -= ROBOT_HIT_DAMAGE;
            selfPowerDrop -= ROBOT_HIT_DAMAGE;
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
        if (board.self.firedPower != 0) {
            selfPowerDrop -= board.self.firedPower;
        }

        board.self.inactiveZap = board.enemy.inactiveZap = (selfPowerDrop >= INACTIVITY_ZAP && Utils.isNear(enemyPowerDrop, INACTIVITY_ZAP)) || (enemyPowerDrop >= INACTIVITY_ZAP && Utils.isNear(selfPowerDrop, INACTIVITY_ZAP));
        if (board.enemy.inactiveZap) {
            enemyPowerDrop -= INACTIVITY_ZAP;
            selfPowerDrop -= INACTIVITY_ZAP;

            if (board.enemy.energy == 0 && enemyPowerDrop < 0.1) {
                enemyPowerDrop = 0;
            }
            if (board.self.energy == 0 && selfPowerDrop < 0.1) {
                selfPowerDrop = 0;
            }
        }

        if (Math.abs(board.prevEnemy.velocity) >= 1 && // there is zero energy drop from slow hit wall, so we would not notice anyway - prev 1 + 1 acceleration = max 2 recognizable
                Math.abs(board.enemy.velocity) <= 1 && // after we hit wall we could only accelerate to 1
                enemyPowerDrop > Utils.NEAR_DELTA && // not just math fluke
                !board.prevEnemy.hitWall) {// he didn't hit the wall before

            int minX = 1 + Robot.ROBOT_HALF_WIDTH;
            int minY = 1 + Robot.ROBOT_HALF_HEIGHT;
            int maxX = BATTLEFIELD_WIDTH - Robot.ROBOT_HALF_WIDTH - 1;
            int maxY = BATTLEFIELD_HEIGHT - Robot.ROBOT_HALF_HEIGHT - 1;

            if (board.enemy.x <= minX) {
                board.enemy.hitWall = true;
            } else if (board.enemy.x >= maxX) {
                board.enemy.hitWall = true;
            } else if (board.enemy.y <= minY) {
                board.enemy.hitWall = true;
            } else if (board.enemy.y >= maxY) {
                board.enemy.hitWall = true;
            }
        }
        if (board.enemy.hitWall) {
            double enemyVelocity = Math.signum(board.prevEnemy.velocity) * Math.min(8, Math.abs(board.prevEnemy.velocity) + 1);
            enemyPowerDrop -= Rules.getWallHitDamage(enemyVelocity);
        }


        if (Utils.isNear(enemyPowerDrop, 0)) {
            enemyPowerDrop = 0;
        } else if (Utils.isNear(enemyPowerDrop, Math.round(enemyPowerDrop))) {
            enemyPowerDrop = Math.round(enemyPowerDrop);
        }

        boolean enemyHotGun = board.enemy.gunHeat > 0;

        if (enemyPowerDrop <= MAX_BULLET_POWER && (enemyPowerDrop >= MIN_BULLET_POWER || (enemyPowerDrop > 0 && board.enemy.energy == 0)) && !enemyHotGun) {
            board.enemy.firedPower = enemyPowerDrop;

            board.enemyNextBulletId++;
            Wave wave = Wave.createWave(board.enemyNextBulletId, 1, enemyPowerDrop, board.round,board.time - 1,
                    // where I was when he aimed
                    board.previous2Turn.self,
                    // where he was when he fired
                    board.previousTurn.self,
                    board.currentTurn.self,
                    board.previous2Turn.enemy,
                    board.previousTurn.enemy,
                    board.currentTurn.enemy
            );
            board.waves.put(wave.id, wave);
            board.enemyLastWave = wave;
            board.enemy.detectedWave = wave;
            board.enemy.gunHeat = Rules.getGunHeat(enemyPowerDrop) - MariusLogic.GUN_COOLING_RATE;
            enemyPowerDrop = 0;
        }

        for (Wave wave : board.waves.values()) {
            if (wave.firedTime == board.time - 1 && wave.detectedOwner == null) {
                wave.detectedVictim = wave.ownerIndex == 0 ? board.enemy : board.self;
                wave.detectedOwner = wave.ownerIndex == 0 ? board.self : board.enemy;
            }
            if (wave.passingEndTime == null) {
                WaveProjection projection = wave.projectWave(board);
                board.currentTurn.waveProjections.put(wave.id, projection);
                wave.lastProjection = projection;

                if(board.self.hitByBulletId!=0){
                    wave.passedVictim = board.self;
                    wave.passedOwner = board.enemy;

                    wave.outcome = WaveState.HIT_VICTIM;
                    wave.hitTime = board.time;
                    wave.finalProjection = projection;
                }
                if(board.enemy.hitByBulletId!=0){
                    wave.passedVictim = board.enemy;
                    wave.passedOwner = board.self;

                    wave.outcome = WaveState.HIT_VICTIM;
                    wave.hitTime = board.time;
                    wave.finalProjection = projection;
                }
                // TODO bullet hit bullet
                // TODO bullet hit wall
            }
        }

        if (Utils.isNear(selfPowerDrop, 0)) {
            selfPowerDrop = 0;
        } else if (Utils.isNear(selfPowerDrop, Math.round(selfPowerDrop))) {
            selfPowerDrop = Math.round(selfPowerDrop);
        }


        board.self.energyDrop = selfPowerDrop;
        board.enemy.energyDrop = enemyPowerDrop;

        return true;
    }

    private void moveLogic() {
        if (board.currentTurn.hitWall != null || (board.currentTurn.hitOther != null && board.currentTurn.hitOther.isMyFault())) {
            if (board.movingForward) {
                board.setMove = -40000;
                board.movingForward = false;
            } else {
                board.setMove = 40000;
                board.movingForward = true;
            }
        }
    }

    private void bodyLogic() {
        board.setBody = board.random.nextDouble() * TWO_PI - PI;
    }

    private void radarLogic() {
        if (board.enemy != null) {
            double enemyBearing = MariusLogic.absoluteBearing(board.self ,board.enemy);
            board.setRadar = Utils.normalRelativeAngle(enemyBearing - board.self.radar) * 2;
        } else {
            board.setRadar = Double.POSITIVE_INFINITY;
        }
    }

    private void gunLogic() {
        if (board.self.gunHeat == 0 && board.currentTurn.enemy != null) {
            board.setFire = 0.1;
        }
    }

    public static final int
            BATTLEFIELD_WIDTH = 800,
            BATTLEFIELD_HEIGHT = 600;

    public static final int
            BULLET_RADIUS = 3;


    public final static double GUN_COOLING_RATE = 0.1;
    public final static long INACTIVITY_TIME = 450;
    public final static double INACTIVITY_ZAP = .1;
    public final static double ROBOT_HIT_DAMAGE = 0.6;

    public final static double MIN_TURN_RATE = Math.toRadians(4.0);
    public final static double MAX_TURN_RATE = Math.toRadians(10.0);
    public final static double MAX_BULLET_VELOCITY = 19.7; //20 - 3 * MIN_BULLET_POWER;
    public final static double MIN_BULLET_VELOCITY = 11.0; //20 - 3 * MAX_BULLET_POWER;

    public final static double PI = java.lang.Math.PI;
    public final static double TWO_PI = 2 * java.lang.Math.PI;
    public final static double THREE_PI_OVER_TWO = 3 * java.lang.Math.PI / 2;
    public final static double PI_OVER_TWO = java.lang.Math.PI / 2;
    private final static double PI_OVER_FOUR = PI / 4;
    private final static double PI_OVER_EIGHT = PI / 8;

    public final static double NORTH = 0 * PI_OVER_FOUR;
    public final static double NORTH_EAST = 1 * PI_OVER_FOUR;
    public final static double EAST = 2 * PI_OVER_FOUR;
    public final static double SOUTH_EAST = 3 * PI_OVER_FOUR;
    public final static double SOUTH = 4 * PI_OVER_FOUR;
    public final static double SOUTH_WEST = 5 * PI_OVER_FOUR;
    public final static double WEST = 6 * PI_OVER_FOUR;
    public final static double NORTH_WEST = 7 * PI_OVER_FOUR;

    protected void init() {
        board.setFire = 0;
    }

    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }

    public static double absoluteBearing(Double x1, Double y1, Double x2, Double y2) {
        return Math.atan2(x2 - x1, y2 - y1);
    }

    public double turnRate(Robot robot, Point2D.Double firedFrom) {
        double bearing = absoluteBearing(firedFrom, robot);
        double idealAngle = bearing + MariusLogic.PI_OVER_TWO;
        return Utils.normalRelativeAngle(robot.body + idealAngle);
    }

    /*public double escapeAngle(Robot robot, Point2D.Double firedFrom, double bulletPower) {
        double distance = robot.distance(firedFrom);
        double ticks = Math.floor(distance / distance / Rules.getBulletSpeed(bulletPower));
        double bearing = absoluteBearing(firedFrom, robot);
        double idealAngle = bearing + MariusLogic.PI_OVER_TWO;
        return Utils.normalRelativeAngle(robot.body + idealAngle);
    }*/
}
