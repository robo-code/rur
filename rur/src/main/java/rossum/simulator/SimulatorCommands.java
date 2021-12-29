package rossum.simulator;

public class SimulatorCommands {
    public SimulatorCommands() {
    }

    public SimulatorCommands(double distance, double body) {
        distanceRemaining = distance;
        bodyTurnRemaining = body;
    }

    public double firePower;
    public double bodyTurnRemaining;
    public double radarTurnRemaining;
    public double gunTurnRemaining;
    public double distanceRemaining;
}
