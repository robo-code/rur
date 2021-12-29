package rossum;

import org.junit.Test;
import robocode.Rules;
import rossum.marius.MariusLogic;
import rossum.simulator.Simulator;
import rossum.simulator.SimulatorCommands;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.util.stream.Collectors.groupingBy;
import static rossum.marius.MariusLogic.EAST;

public class ExploreBruteTests {
    private static final int zoom = 5;
    private static final Point2D.Double start = new Point2D.Double(400, 300);

    Function<Simulator, Long> grp = s -> Math.round(MariusLogic.absoluteBearing(start, s) * 90 / Math.PI) + (1000 * (1 + Math.round(s.body * 45 / Math.PI)));

    @Test
    public void explore() {
        Simulator sim = new Simulator();
        sim.prev = sim;// self
        List<Simulator> in = new LinkedList<>();
        sim.x = start.x;
        sim.y = start.y;
        sim.body = EAST;
        sim.velocity = 0;
        in.add(sim);

        BufferedImage image = new BufferedImage(zoom * MariusLogic.BATTLEFIELD_WIDTH, zoom * MariusLogic.BATTLEFIELD_WIDTH, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        graphics.setColor(Color.RED);
        graphics.drawOval((int) start.x - zoom, (int) start.y - zoom, 3 * zoom, 3 * zoom);

        Map<Long, Double> reach = new HashMap<>();

        for (int time = 0; time < 50; time++) {

            List<Simulator> out = new LinkedList<>();
            for (Simulator i : in) {
                step(i, out);
            }


            Collection<List<Simulator>> groups = out.stream()
                    .collect(groupingBy(grp))
                    .values();
            out = new LinkedList<>();
            for (List<Simulator> gr : groups) {
                Simulator best = gr.stream().sorted((Simulator a, Simulator b) -> {
                    double da = a.distance(start);
                    double db = b.distance(start);
                    return (int) Math.signum(db - da);
                }).limit(2).findAny().get();

                Long bestGroup = grp.apply(best);
                double bestDistance = best.distance(start);
                Double bestSoFar = reach.get(bestGroup);

                if (bestSoFar == null || bestSoFar < bestDistance) {
                    reach.put(bestGroup, bestDistance);
                    out.add(best);
                }
            }
            in = out;
        }
        List<Simulator> winners = new LinkedList<>();
        for (Simulator s : in) {
            pick(winners, s);
        }
        winners = winners
                .stream().sorted((Simulator a, Simulator b) -> {
                    int bytime=(int) (b.time - a.time);
                    if(bytime!=0){
                        return bytime;
                    }
                    double ba = MariusLogic.absoluteBearing(start, a) + Math.PI + Math.PI;
                    double bb = MariusLogic.absoluteBearing(start, a) + Math.PI + Math.PI;
                    return (int)(bb-ba);
                })
                .collect(Collectors.toList());

        Simulator p=new Simulator();
        p.x=start.x;
        p.y=start.y;
        for (Simulator s : winners) {
            int m = (int) s.time % 3;
            int c = 100 + (int)s.time * 3;
            switch (m) {
                case 0:
                    graphics.setColor(new Color(c, 200, 200));
                    break;
                case 1:
                    graphics.setColor(new Color(200, c, 200));
                    break;
                case 2:
                    graphics.setColor(new Color(200, 200, c));
                    break;
            }
            //graphics.drawLine((int) (zoom * p.x), (int) (zoom * p.y),(int) (zoom * s.x), (int) (zoom * s.y));
            p=s;
            graphics.drawOval((int) (zoom * s.x), (int) (zoom * s.y), 1, 1);
        }

        VisualUtils.save(image, "c:\\Dev\\robocode\\distance.png");
    }

    public void pick(List<Simulator> winners, Simulator sim) {
        winners.add(sim);

        Simulator prev = sim.prev;
        sim.prev = null;
        if (prev != null && prev != sim) {
            pick(winners, prev);
        }
    }


    public void step(Simulator sim, List<Simulator> out) {
        double maxTurnRate = min(Rules.MAX_TURN_RATE_RADIANS, (.4 + .6 * (1 - (abs(sim.velocity) / Rules.MAX_VELOCITY))) * Rules.MAX_TURN_RATE_RADIANS);
        boolean brule = sim.time < 20;
        boolean vrule = sim.time < 20;
        int lowerB = brule ? 2 : 1;
        int lowerV = vrule ? 2 : 1;
        for (int sb = 0; sb < lowerB; sb++) {
            double signb = brule
                    ? (sb == 0 ? 1 : -1)
                    : Math.signum(sim.body);
            for (int b = 0; b < 5; b++) {
                for (int sv = 0; sv < lowerV; sv++) {
                    double signv =
                            vrule
                                    ? (sv == 0 ? 1 : -1)
                                    : Math.signum(sim.velocity);
                    for (int v = 0; v < 5; v++) {
                        double distance = Rules.MAX_VELOCITY / 5.0 * v * signv;
                        double body = maxTurnRate / 5.0 * b * signb;
                        SimulatorCommands cmd = new SimulatorCommands(distance, body);
                        Simulator next = sim.performMove(cmd);
                        out.add(next);
                    }
                }
            }
        }
    }
}
