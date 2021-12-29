package rossum;

import org.junit.Test;
import robocode.Rules;
import robocode.util.Utils;
import rossum.marius.MariusLogic;
import rossum.simulator.Simulator;
import rossum.simulator.SimulatorCommands;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static rossum.marius.MariusLogic.EAST;

public class MaxEscapeTest {
    double zoom = 10.0;
    private static final Point2D.Double start = new Point2D.Double(200, 150);

    @Test
    public void explore() {

        System.out.println("distance, speed, angle, fast, fwd, escape");
        for (int distance = 1; distance < 6; distance++) {
            for (int speed = -8; speed <= 8; speed += 2) {
                for (int angle = 0; angle <= 9; angle += 3) {
                    for (int fast = 0; fast <= 1; fast++) {
                        for (int fwd = 0; fwd <= 1; fwd++) {
                            double escape = explore(speed, angle, distance, fast,fwd);
                            System.out.println(distance + "," + speed + "," + angle + "," + fast + "," + fwd+","+escape);
                        }
                    }
                }
            }
        }
        //explore(8, 9, 6, 0);
    }

    public double explore(int s, int a, int d, int fast, int fwd) {
        double startSpeed = s;
        double angle = MariusLogic.PI_OVER_TWO / 9 * a;
        double fireDistance = 50 + d * 50;
        double bulletSpeed = fast == 1 ? MariusLogic.MAX_BULLET_VELOCITY : MariusLogic.MIN_BULLET_VELOCITY;
        int limit = (int) Math.floor(fireDistance / bulletSpeed);

        BufferedImage image = new BufferedImage(4000, 3000, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();

        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 4000, 3000);

        LinkedList<List<Simulator>> trajectories = new LinkedList<>();

        Point2D.Double firedFrom = new Point2D.Double(start.x + fireDistance * sin(angle), start.y - fireDistance * cos(angle));

        BiFunction<Integer, Simulator, Double> turnFn = (time, prev) -> {
            double bearing = MariusLogic.absoluteBearing(prev, firedFrom);
            double idealAngle = bearing + MariusLogic.PI_OVER_TWO;
            return Utils.normalRelativeAngle(prev.body - idealAngle);
        };
        sim(limit, (time, prev) -> fwd==0 ? Rules.MAX_VELOCITY:-Rules.MAX_VELOCITY, turnFn, startSpeed, trajectories);
/*
        graphics.setColor(Color.WHITE);
        drawCircle(graphics, firedFrom, fireDistance);

        for (List<Simulator> trajectory : trajectories) {
            for (int time = 1; time < limit; time++) {
                Simulator prev = trajectory.get(time - 1);
                Simulator next = trajectory.get(time);
                graphics.setColor(colorByTime(time, 100));
                drawLine(graphics, prev, next);
            }
        }
        VisualUtils.save(image, "c:\\Dev\\robocode\\escape a" + a + "d" + d + "f" + fast + "s" + s + "w" + fwd + " .png");
 */

        double bearing = MariusLogic.absoluteBearing(firedFrom, trajectories.get(0).get(limit - 1));

        return bearing;
    }


    public void sim(int limit, BiFunction<Integer, Simulator, Double> velFn, BiFunction<Integer, Simulator, Double> turnFn, double startSpeed, List<List<Simulator>> trajectories) {
        List<Simulator> trajectory = new LinkedList<>();
        Simulator sim = new Simulator();
        sim.x = start.x;
        sim.y = start.y;
        sim.body = EAST;
        sim.velocity = startSpeed;
        trajectory.add(sim);
        for (int time = 1; time < limit; time++) {
            double velocity = velFn.apply(time, sim);
            double maxTurnRate = turnFn.apply(time, sim);
            SimulatorCommands cmd = new SimulatorCommands(velocity, maxTurnRate);
            Simulator next = sim.performMove(cmd);
            trajectory.add(next);
            sim = next;
        }
        trajectories.add(trajectory);
    }

    void drawCircle(Graphics graphics, Point2D.Double center, double radius) {
        graphics.drawOval((int) ((center.x - radius) * zoom), (int) ((center.y - radius) * zoom), (int) (radius * 2 * zoom), (int) (radius * 2 * zoom));
    }

    public void drawLine(Graphics graphics, Point2D.Double from, Point2D.Double to) {
        graphics.drawLine((int) (zoom * from.x), (int) (zoom * from.y), (int) (zoom * to.x), (int) (zoom * to.y));
    }

    public Color colorByTime(int time, int offset) {
        int m = time % 3;
        int c = 200 + (time);
        switch (m) {
            case 0:
                return new Color(c, offset, offset);
            case 1:
                return new Color(offset, c, offset);
            case 2:
                return new Color(offset, offset, c);
        }
        return null;
    }
}

