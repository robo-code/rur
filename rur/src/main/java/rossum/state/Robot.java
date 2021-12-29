package rossum.state;

import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import rossum.simulator.Simulator;

import java.awt.*;
import java.awt.geom.Point2D;

public class Robot extends Point2D.Double {
    public static final int
            ROBOT_WIDTH = 36,
            ROBOT_HEIGHT = 36;

    public static final int
            ROBOT_HALF_WIDTH = ROBOT_WIDTH / 2,
            ROBOT_HALF_HEIGHT = ROBOT_HEIGHT / 2;


    public String name;
    public long time;
    public double body;
    public double velocity;
    public double energy;
    public double gunHeat;

    public double firedPower;
    public double hitByBulletPower;
    public int hitByBulletId;
    public boolean hitOther;
    public boolean hitWall;
    public boolean inactiveZap;
    public boolean isDead;

    public double energyDrop;
    public double rawEnergyDrop;

    public Wave detectedWave;

    public static Robot createEnemy(Myself self, ScannedRobotEvent scan, double gunHeat, long time) {
        Robot robot = new Robot();
        robot.name = scan.getName();
        robot.velocity = scan.getVelocity();
        robot.energy = scan.getEnergy();
        robot.gunHeat = gunHeat;
        robot.time = time;
        robot.body = scan.getHeadingRadians();
        double bearing = Utils.normalRelativeAngle(self.body + scan.getBearingRadians());

        double distance = scan.getDistance();
        robot.x = self.x + Math.sin(bearing) * distance;
        robot.y = self.y + Math.cos(bearing) * distance;

        robot.hitByBulletPower = 0;
        robot.firedPower = 0;
        robot.hitWall = false;
        robot.inactiveZap = false;
        robot.hitOther = false;

        return robot;
    }

    public static Simulator createSimulator(Myself self, ScannedRobotEvent scan, double gunHeat, long time) {
        Simulator robot = new Simulator();
        robot.name = scan.getName();
        robot.velocity = scan.getVelocity();
        robot.energy = scan.getEnergy();
        robot.gunHeat = gunHeat;
        robot.time = time;
        robot.body = scan.getHeadingRadians();
        double bearing = Utils.normalRelativeAngle(self.body + scan.getBearingRadians());

        double distance = scan.getDistance();
        robot.x = self.x + Math.sin(bearing) * distance;
        robot.y = self.y + Math.cos(bearing) * distance;

        robot.hitByBulletPower = 0;
        robot.firedPower = 0;
        robot.hitWall = false;
        robot.inactiveZap = false;
        robot.hitOther = false;

        return robot;
    }

    public BoundingRectangle getBoundingBox(){
        return new BoundingRectangle(x, y, Robot.ROBOT_WIDTH, Robot.ROBOT_HEIGHT);
    }

    public void onPaint(Graphics2D graphics2D) {
        graphics2D.setColor(Color.BLACK);
        graphics2D.drawRect((int) x - Robot.ROBOT_HALF_WIDTH, (int) y - Robot.ROBOT_HALF_HEIGHT, Robot.ROBOT_WIDTH, Robot.ROBOT_HEIGHT);
    }

    public void assertNear(Robot actual) {
        Utils.assertNear("time", time, actual.time);
        Utils.assertNear("body", body, actual.body);
        Utils.assertNear("energy", energy, actual.energy);
        Utils.assertNear("velocity", velocity, actual.velocity);
        Utils.assertNear("x", x, actual.x);
        Utils.assertNear("y", y, actual.y);
        Utils.assertEquals("hitOther", hitOther, actual.hitOther);
    }

    public void assertNear2(Robot actual) {
        if(actual.rawEnergyDrop>0){
            Utils.assertEquals("rawEnergyDrop", rawEnergyDrop, actual.rawEnergyDrop);
        }
        if (rawEnergyDrop > 0 && hitByBulletPower == 0 && !inactiveZap && !actual.inactiveZap) {
            if (actual.hitWall) {
                Utils.assertTrue("hitWall", hitWall);
            }
            if (!hitWall && actual.hitWall) {
                Utils.assertEquals("energyDrop", energyDrop, actual.energyDrop);
            }
        }
        Utils.assertNear("hitByBulletPower", hitByBulletPower, actual.hitByBulletPower);

        if (!hitWall && actual.hitWall) {
            Utils.assertEquals("inactiveZap", inactiveZap, actual.inactiveZap);
        }
        Utils.assertNear("firedPower", firedPower, actual.firedPower);
        Utils.assertNear("gunHeat", gunHeat, actual.gunHeat);
    }

    @Override
    public String toString() {

        return time + " " + name + "(" + (int) energy + ") X" + (int) x + " Y" + (int) y
                + " ~" + Utils.angleToApproximateDirection(body) +
                " hit:" + (int) hitByBulletPower +
                " fired:" + (int) firedPower +
                " wall:" + hitWall +
                " bot:" + hitOther +
                " zap:" + inactiveZap
                ;

    }
}
