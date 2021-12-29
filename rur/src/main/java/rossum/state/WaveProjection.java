package rossum.state;

import robocode.util.Utils;
import rossum.marius.MariusLogic;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Ellipse2D;

public class WaveProjection {
    public Wave wave;
    public double currentDistanceTraveled;
    public double bulletHeading;
    public Line2D.Double idealBullet;
    public WaveState state = WaveState.MOVING;
    public Robot victim;
    public long age;

    public static WaveProjection create(Wave wave, long time, Robot victim, Double idealHeading) {
        WaveProjection wp = new WaveProjection();
        wp.wave = wave;
        wp.victim = victim;

        wp.age = time - wave.firedTime;
        wp.currentDistanceTraveled = wave.velocity * wp.age;

        if (idealHeading != null) {
            wp.bulletHeading = idealHeading;
            wp.detectCollision();
        } else {
            wp.bulletHeading = Utils.normalAbsoluteAngle(wp.headingToClosestEdge());
            if (!wp.detectCollision()) {
                wp.bulletHeading = Utils.normalAbsoluteAngle(MariusLogic.absoluteBearing(wave.firedOwner, victim));
                wp.detectCollision();
            }
        }

        return wp;
    }

    private double headingToClosestEdge() {
        double dx = victim.x - wave.firedOwner.x;
        double dy = victim.y - wave.firedOwner.y;
        double absDy = Math.abs(dy);
        double absDx = Math.abs(dx);
        double sigDx = Math.signum(dy);
        double sigDy = Math.signum(dx);
        if (absDx > absDy) {
            if (absDy > Robot.ROBOT_HALF_HEIGHT) {
                dy -= sigDx * Robot.ROBOT_HALF_HEIGHT;
            } else {
                dy = 0;
            }
            dx -= sigDy * Robot.ROBOT_HALF_WIDTH;
        } else {
            if (absDx > Robot.ROBOT_HALF_WIDTH) {
                dx -= sigDy * Robot.ROBOT_HALF_WIDTH;
            } else {
                dx = 0;
            }
            dy -= sigDx * Robot.ROBOT_HALF_HEIGHT;
        }
        return Math.atan2(dx, dy);
    }

    private boolean detectCollision() {
        double idealVelocityX = wave.velocity * Math.sin(bulletHeading);
        double idealVelocityY = wave.velocity * Math.cos(bulletHeading);

        double x1 = wave.firedOwner.x + idealVelocityX * (age - 1);
        double y1 = wave.firedOwner.y + idealVelocityY * (age - 1);
        idealBullet = new Line2D.Double(x1, y1, x1 + idealVelocityX, y1 + idealVelocityY);

        BoundingRectangle victimBounds = victim.getBoundingBox();
        double prevDistanceTraveled = wave.velocity * (age - 1);
        double victimDistanceFromFired = wave.firedOwner.distance(victim);


        boolean hitByIdealBullet = victimBounds.intersectsLine(idealBullet);
        if (hitByIdealBullet) {
            state = WaveState.PASSING;
            return true;
        } else if (prevDistanceTraveled > victimDistanceFromFired) {
            state = WaveState.GONE;
        } else {
            state = WaveState.MOVING;
        }
        return false;
    }

    public void onPaint(Graphics2D graphics2D) {
        Color paint = this.wave.ownerIndex == 0
                ? new Color(0, 192, 0, 30)
                : new Color(192, 0, 0, 30);

        Color color = this.wave.ownerIndex == 0
                ? new Color(0, 255, 0)
                : new Color(255, 0, 0);

        graphics2D.setColor(color);
        graphics2D.drawLine((int) idealBullet.x1, (int) idealBullet.y1, (int) idealBullet.x2, (int) idealBullet.y2);

        graphics2D.setColor(color);
        graphics2D.drawRect((int) victim.x - Robot.ROBOT_HALF_WIDTH, (int) victim.y - Robot.ROBOT_HALF_HEIGHT, Robot.ROBOT_WIDTH, Robot.ROBOT_HEIGHT);

        graphics2D.setColor(color);
        graphics2D.drawLine((int) this.wave.firedOwner.x, (int) this.wave.firedOwner.y - 10, (int) this.wave.firedOwner.x, (int) this.wave.firedOwner.y + 10);
        graphics2D.drawLine((int) this.wave.firedOwner.x - 10, (int) this.wave.firedOwner.y, (int) this.wave.firedOwner.x + 10, (int) this.wave.firedOwner.y);

        double prevDistance = wave.velocity * (age - 1);

        Ellipse2D.Double outer = new Ellipse2D.Double(wave.firedOwner.x - currentDistanceTraveled, wave.firedOwner.y - currentDistanceTraveled, currentDistanceTraveled * 2, currentDistanceTraveled * 2);
        Ellipse2D.Double inner = new Ellipse2D.Double(wave.firedOwner.x - prevDistance, wave.firedOwner.y - prevDistance, prevDistance * 2, prevDistance * 2);
        Area outerArea = new Area(outer);
        outerArea.subtract(new Area(inner));

        graphics2D.setPaint(paint);
        graphics2D.fill(outerArea);
    }

    @Override
    public String toString() {
        if (idealBullet == null) {
            return wave.toString();
        }
        return wave.id + " (" + wave.power + ") " + wave.firedTime + " X" + (int) idealBullet.x2 + " Y" + (int) idealBullet.y2
                + " ~" + Utils.angleToApproximateDirection(bulletHeading)
                + " " + state;
    }

    public void assertNear(WaveProjection actual) {
        wave.assertNear(actual.wave);
        victim.assertNear(actual.victim);
        Utils.assertNear("currentDistanceTraveled", currentDistanceTraveled, actual.currentDistanceTraveled);
        //noinspection StatementWithEmptyBody
        if (actual.state == WaveState.PASSING) {
            // this is OK to over detect collision
            // actual is assuming aiming missile which is following the target perfectly
        } else {
            Utils.assertEquals("state", state, actual.state);
        }
    }
}
