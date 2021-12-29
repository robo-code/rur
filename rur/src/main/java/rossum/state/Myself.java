package rossum.state;

import robocode.RobotStatus;
import rossum.simulator.Simulator;

import static rossum.marius.MariusLogic.absoluteBearing;

public class Myself extends Robot {
    public double radar;
    public double gun;

    public static Myself createSelf(String name, RobotStatus self, long time) {
        Myself robot = new Myself();
        robot.name = name;
        robot.x = self.getX();
        robot.y = self.getY();
        robot.velocity = self.getVelocity();
        robot.body = self.getHeadingRadians();
        robot.energy = self.getEnergy();
        robot.gunHeat = self.getGunHeat();
        robot.time = time;

        robot.gun = self.getGunHeadingRadians();
        robot.radar = self.getRadarHeadingRadians();

        return robot;
    }

    public static Simulator createSimulator(String name, RobotStatus self, long time) {
        Simulator robot = new Simulator();
        robot.name = name;
        robot.x = self.getX();
        robot.y = self.getY();
        robot.velocity = self.getVelocity();
        robot.body = self.getHeadingRadians();
        robot.energy = self.getEnergy();
        robot.gunHeat = self.getGunHeat();
        robot.time = time;
        robot.gun = self.getGunHeadingRadians();
        robot.radar = self.getRadarHeadingRadians();

        return robot;
    }

}
