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
import java.util.function.Function;

import static rossum.marius.MariusLogic.EAST;

public class ExploreStrategyTests {
    double zoom = 10.0;
    private static final Point2D.Double start = new Point2D.Double(200, 150);

    @Test
    public void exploreStop() {
        explore(0.0);
    }

    @Test
    public void exploreSpeed() {
        explore(8.0);
    }

    public void explore(double startSpeed) {

        BufferedImage image = new BufferedImage(4000, 3000, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();

        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 4000, 3000);

        LinkedList<List<Simulator>> trajectories= new LinkedList<>();
        sim(v->Rules.MAX_VELOCITY * ((double) limit - v) / (double) limit,v->Rules.MAX_TURN_RATE_RADIANS,startSpeed,trajectories);
        sim(v->Rules.MAX_VELOCITY,v->MariusLogic.MAX_TURN_RATE * ((double) v) / (double) 20,startSpeed,trajectories);

        for (int fireDistance = 50; fireDistance < 300; fireDistance+=20) {
            Point2D.Double firedFrom = new Point2D.Double(start.x, start.y - fireDistance);
            double direct= MariusLogic.absoluteBearing(firedFrom,start);
            double max=Double.NEGATIVE_INFINITY;
            List<Simulator> maxs=null;


            graphics.setColor(Color.WHITE);
            drawCircle(graphics, firedFrom, fireDistance);

            for (List<Simulator> trajectory : trajectories) {
                for (int time = 1; time < limit; time++) {
                    Simulator next = trajectory.get(time);
                    double angle = Utils.normalRelativeAngle(MariusLogic.absoluteBearing(firedFrom, next)-direct);
                    if (max < angle) {
                        max = angle;
                        maxs = trajectory;
                    }
                }
            }
            for(List<Simulator> trajectory:trajectories){
                if (trajectory == maxs) {
                    for (int time = 1; time < limit; time++) {
                        Simulator next = trajectory.get(time);
                        double angle = Utils.normalRelativeAngle(MariusLogic.absoluteBearing(firedFrom, next)-direct);
                        if (Utils.isNear(max, angle)) {
                            graphics.setColor(Color.blue);
                            drawLine(graphics, firedFrom, next);
                        }
                    }
                }
            }
        }

        for (int time = 1; time < limit; time++) {
            for(List<Simulator> trajectory:trajectories){
                Simulator prev = trajectory.get(time-1);
                Simulator next = trajectory.get(time);
                graphics.setColor(colorByTime(time, 100));
                drawLine(graphics, prev, next);
            }
        }

        VisualUtils.save(image, "c:\\Dev\\robocode\\" + startSpeed + "distance.png");
    }


    private int limit = 30;
    public void sim(Function<Integer, Double> velFn, Function<Integer, Double> turnFn, double startSpeed, List<List<Simulator>> trajectories) {
        for (int v = 0; v < 14; v++) {
            List<Simulator> trajectory=new LinkedList<>();
            Simulator sim = new Simulator();
            sim.x = start.x;
            sim.y = start.y;
            sim.body = EAST;
            sim.velocity = startSpeed;
            trajectory.add(sim);
            for (int time = 1; time < limit; time++) {
                double velocity = velFn.apply(v);
                double maxTurnRate = turnFn.apply(v);
                SimulatorCommands cmd = new SimulatorCommands(velocity, maxTurnRate);
                Simulator next = sim.performMove(cmd);
                trajectory.add(next);
                sim = next;
            }
            trajectories.add(trajectory);
        }
    }

    void drawCircle(Graphics graphics, Point2D.Double center, double radius){
        graphics.drawOval((int)((center.x-radius)*zoom), (int)((center.y-radius)*zoom), (int)(radius*2*zoom), (int)(radius*2*zoom));
    }

    public void drawLine(Graphics graphics, Point2D.Double from, Point2D.Double to){
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

