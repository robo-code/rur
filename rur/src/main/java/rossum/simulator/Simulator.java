package rossum.simulator;

import robocode.Bullet;
import robocode.Rules;
import robocode.util.Utils;

import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import rossum.marius.MariusLogic;
import rossum.state.Robot;

import static java.lang.Math.*;
import static robocode.util.Utils.*;

public final class Simulator extends Robot {
    public Simulator prev;
    public double radar;
    public double gun;
    public boolean isOverDriving;

    public final Simulator performMove(SimulatorCommands commands) {
        return performMove(commands, null, 0);
    }

    public final Simulator performMove(SimulatorCommands commands, List<Robot> robots, double zapEnergy) {
        Simulator next = new Simulator();
        next.prev = this;

        next.name = name;
        next.time = time + 1;
        next.body = body;
        next.gun = gun;
        next.x = x;
        next.y = y;
        next.radar = radar;
        next.isOverDriving = isOverDriving;
        next.velocity = velocity;
        next.hitWall = false;

        next.fireBullets(commands);
        next.updateGunHeat();
        if (!hitWall && !hitOther) {
            next.updateHeading(commands);
        }
        next.updategun(commands);
        next.updateradar(commands);
        next.updateMovement(commands);
        next.checkWallCollision(commands);
        if (robots != null) {
            next.checkRobotCollision(commands, robots);
        }

        if (zapEnergy != 0) {
            next.zap(commands, zapEnergy);
        }

        return next;
    }

    public final Bullet fireBullets(SimulatorCommands commands) {
        if (gunHeat > 0 || energy == 0 || commands.firePower < Rules.MIN_BULLET_POWER) {
            return null;
        }

        double firePower = min(energy, min(max(commands.firePower, Rules.MIN_BULLET_POWER), Rules.MAX_BULLET_POWER));

        updateEnergy(-firePower);
        gunHeat += Rules.getGunHeat(firePower);
        return new Bullet(gun, x, y, firePower, name, "victim", true, -1);
    }

    private void checkRobotCollision(SimulatorCommands commands, List<Robot> robots) {
        for (Robot otherRobot : robots) {
            if (!(otherRobot == null || otherRobot == this)
                    && getBoundingBox().intersects(otherRobot.getBoundingBox())) {
                // Bounce back
                double angle = atan2(otherRobot.x - x, otherRobot.y - y);

                double movedx = velocity * sin(body);
                double movedy = velocity * cos(body);

                double bearing = normalRelativeAngle(angle - body);

                if ((velocity > 0 && bearing > -PI / 2 && bearing < PI / 2)
                        || (velocity < 0 && (bearing < -PI / 2 || bearing > PI / 2))) {

                    hitOther = true;
                    velocity = 0;
                    commands.distanceRemaining = 0;
                    x -= movedx;
                    y -= movedy;

                    this.updateEnergy(-Rules.ROBOT_HIT_DAMAGE);
                    otherRobot.energy -= -Rules.ROBOT_HIT_DAMAGE;
                }
            }
        }
    }

    private void checkWallCollision(SimulatorCommands commands) {
        int minX = Robot.ROBOT_HALF_WIDTH;
        int minY = Robot.ROBOT_HALF_HEIGHT;
        int maxX = MariusLogic.BATTLEFIELD_WIDTH - Robot.ROBOT_HALF_WIDTH;
        int maxY = MariusLogic.BATTLEFIELD_HEIGHT - Robot.ROBOT_HALF_HEIGHT;

        double adjustX = 0, adjustY = 0;

        if (x < minX) {
            hitWall = true;
            adjustX = minX - x;

        } else if (x > maxX) {
            hitWall = true;
            adjustX = maxX - x;

        }
        if (y < minY) {
            hitWall = true;
            adjustY = minY - y;

        } else if (y > maxY) {
            hitWall = true;
            adjustY = maxY - y;
        }

        if (hitWall) {

            // only fix both x and y values if hitting wall at an angle
            if ((body % (Math.PI / 2)) != 0) {
                double tanHeading = tan(body);

                // if it hits bottom or top wall
                if (adjustX == 0) {
                    adjustX = adjustY * tanHeading;
                } // if it hits a side wall
                else if (adjustY == 0) {
                    adjustY = adjustX / tanHeading;
                } // if the robot hits 2 walls at the same time (rare, but just in case)
                else if (abs(adjustX / tanHeading) > abs(adjustY)) {
                    adjustY = adjustX / tanHeading;
                } else if (abs(adjustY * tanHeading) > abs(adjustX)) {
                    adjustX = adjustY * tanHeading;
                }
            }
            x += adjustX;
            y += adjustY;

            if (x < minX) {
                x = minX;
            } else if (x > maxX) {
                x = maxX;
            }
            if (y < minY) {
                y = minY;
            } else if (y > maxY) {
                y = maxY;
            }

            // Update energy, but do not reset inactiveTurnCount
            setEnergy(energy - Rules.getWallHitDamage(velocity));

            commands.distanceRemaining = 0;
            velocity = 0;
            hitWall = true;
            //TODO addEvent
        }
    }

    private void updategun(SimulatorCommands currentCommands) {
        if (currentCommands.gunTurnRemaining > 0) {
            if (currentCommands.gunTurnRemaining < Rules.GUN_TURN_RATE_RADIANS) {
                gun += currentCommands.gunTurnRemaining;
                radar += currentCommands.gunTurnRemaining;
                currentCommands.radarTurnRemaining -= currentCommands.gunTurnRemaining;
                currentCommands.gunTurnRemaining = 0;
            } else {
                gun += Rules.GUN_TURN_RATE_RADIANS;
                radar += Rules.GUN_TURN_RATE_RADIANS;
                currentCommands.radarTurnRemaining -= Rules.GUN_TURN_RATE_RADIANS;
                currentCommands.gunTurnRemaining -= Rules.GUN_TURN_RATE_RADIANS;
            }
        } else if (currentCommands.gunTurnRemaining < 0) {
            if (currentCommands.gunTurnRemaining > -Rules.GUN_TURN_RATE_RADIANS) {
                gun += currentCommands.gunTurnRemaining;
                radar += currentCommands.gunTurnRemaining;
                currentCommands.radarTurnRemaining += currentCommands.gunTurnRemaining;
                currentCommands.gunTurnRemaining = 0;
            } else {
                gun -= Rules.GUN_TURN_RATE_RADIANS;
                radar -= Rules.GUN_TURN_RATE_RADIANS;
                currentCommands.radarTurnRemaining += Rules.GUN_TURN_RATE_RADIANS;
                currentCommands.gunTurnRemaining += Rules.GUN_TURN_RATE_RADIANS;
            }
        }
        gun = normalAbsoluteAngle(gun);
    }

    private void updateHeading(SimulatorCommands currentCommands) {
        boolean normalizeHeading = true;

        double turnRate = min(Rules.MAX_TURN_RATE_RADIANS, (.4 + .6 * (1 - (abs(velocity) / Rules.MAX_VELOCITY))) * Rules.MAX_TURN_RATE_RADIANS);

        if (currentCommands.bodyTurnRemaining > 0) {
            if (currentCommands.bodyTurnRemaining < turnRate) {
                body += currentCommands.bodyTurnRemaining;
                gun += currentCommands.bodyTurnRemaining;
                radar += currentCommands.bodyTurnRemaining;
                currentCommands.gunTurnRemaining -= currentCommands.bodyTurnRemaining;
                currentCommands.radarTurnRemaining -= currentCommands.bodyTurnRemaining;
                currentCommands.bodyTurnRemaining = 0;
            } else {
                body += turnRate;
                gun += turnRate;
                radar += turnRate;
                currentCommands.gunTurnRemaining -= turnRate;
                currentCommands.radarTurnRemaining -= turnRate;
                currentCommands.bodyTurnRemaining -= turnRate;
            }
        } else if (currentCommands.bodyTurnRemaining < 0) {
            if (currentCommands.bodyTurnRemaining > -turnRate) {
                body += currentCommands.bodyTurnRemaining;
                gun += currentCommands.bodyTurnRemaining;
                radar += currentCommands.bodyTurnRemaining;
                currentCommands.gunTurnRemaining += currentCommands.bodyTurnRemaining;
                currentCommands.radarTurnRemaining += currentCommands.bodyTurnRemaining;
                currentCommands.bodyTurnRemaining = 0;
            } else {
                body -= turnRate;
                gun -= turnRate;
                radar -= turnRate;
                currentCommands.gunTurnRemaining += turnRate;
                currentCommands.radarTurnRemaining += turnRate;
                currentCommands.bodyTurnRemaining += turnRate;
            }
        } else {
            normalizeHeading = false;
        }

        if (normalizeHeading) {
            if (currentCommands.bodyTurnRemaining == 0) {
                body = normalNearAbsoluteAngle(body);
            } else {
                body = normalAbsoluteAngle(body);
            }
        }
    }

    private void updateradar(SimulatorCommands currentCommands) {
        if (currentCommands.radarTurnRemaining > 0) {
            if (currentCommands.radarTurnRemaining < Rules.RADAR_TURN_RATE_RADIANS) {
                radar += currentCommands.radarTurnRemaining;
                currentCommands.radarTurnRemaining = 0;
            } else {
                radar += Rules.RADAR_TURN_RATE_RADIANS;
                currentCommands.radarTurnRemaining -= Rules.RADAR_TURN_RATE_RADIANS;
            }
        } else if (currentCommands.radarTurnRemaining < 0) {
            if (currentCommands.radarTurnRemaining > -Rules.RADAR_TURN_RATE_RADIANS) {
                radar += currentCommands.radarTurnRemaining;
                currentCommands.radarTurnRemaining = 0;
            } else {
                radar -= Rules.RADAR_TURN_RATE_RADIANS;
                currentCommands.radarTurnRemaining += Rules.RADAR_TURN_RATE_RADIANS;
            }
        }

        radar = normalAbsoluteAngle(radar);
    }

    private void updateMovement(SimulatorCommands currentCommands) {
        double distance = currentCommands.distanceRemaining;
        velocity = getNewVelocity(velocity, distance);

        if (isNear(velocity, 0) && isOverDriving) {
            currentCommands.distanceRemaining = 0;
            distance = 0;
            isOverDriving = false;
        }

        if (Math.signum(distance * velocity) != -1) {
            isOverDriving = getDistanceTraveledUntilStop(velocity) > Math.abs(distance);
        }

        currentCommands.distanceRemaining = distance - velocity;

        if (velocity != 0) {
            x += velocity * sin(body);
            y += velocity * cos(body);
        }
    }

    private double getDistanceTraveledUntilStop(double velocity) {
        double distance = 0;

        velocity = Math.abs(velocity);
        while (velocity > 0) {
            distance += (velocity = getNewVelocity(velocity, 0));
        }
        return distance;
    }

    private double getNewVelocity(double velocity, double distance) {
        if (distance < 0) {
            return -getNewVelocity(-velocity, -distance);
        }

        final double goalVel;

        if (distance == java.lang.Double.POSITIVE_INFINITY) {
            goalVel = Rules.MAX_VELOCITY;
        } else {
            goalVel = Math.min(getMaxVelocity(distance), Rules.MAX_VELOCITY);
        }

        if (velocity >= 0) {
            return Math.max(velocity - Rules.DECELERATION, Math.min(goalVel, velocity + Rules.ACCELERATION));
        }
        // else
        return Math.max(velocity - Rules.ACCELERATION, Math.min(goalVel, velocity + maxDecel(-velocity)));
    }

    private static double getMaxVelocity(double distance) {
        final double decelTime = Math.max(1, Math.ceil(// sum of 0... decelTime, solving for decelTime using quadratic formula
                (Math.sqrt((4 * 2 / Rules.DECELERATION) * distance + 1) - 1) / 2));

        if (decelTime == java.lang.Double.POSITIVE_INFINITY) {
            return Rules.MAX_VELOCITY;
        }

        final double decelDist = (decelTime / 2.0) * (decelTime - 1) // sum of 0..(decelTime-1)
                * Rules.DECELERATION;

        return ((decelTime - 1) * Rules.DECELERATION) + ((distance - decelDist) / decelTime);
    }

    private static double maxDecel(double speed) {
        double decelTime = speed / Rules.DECELERATION;
        double accelTime = (1 - decelTime);

        return Math.min(1, decelTime) * Rules.DECELERATION + Math.max(0, accelTime) * Rules.ACCELERATION;
    }

    private void updateGunHeat() {
        gunHeat -= MariusLogic.GUN_COOLING_RATE;
        if (gunHeat < 0) {
            gunHeat = 0;
        }
    }

    private void zap(SimulatorCommands commands, double zapAmount) {
        if (energy == 0) {
            return;
        }
        energy -= abs(zapAmount);
        if (energy < .1) {
            energy = 0;
            commands.distanceRemaining = 0;
            commands.bodyTurnRemaining = 0;
        }
    }

    private void updateEnergy(double delta) {
        if (delta < 0) {
            setEnergy(energy + delta);
        }
    }

    private void setEnergy(double newEnergy) {
        energy = newEnergy;
        if (energy < .01) {
            energy = 0;
        }
    }

    @Override
    public String toString() {
        return "(" + (int) energy + ") X" + (int) x + " Y" + (int) y
                + " ~" + Utils.angleToApproximateDirection(body);
    }

}
