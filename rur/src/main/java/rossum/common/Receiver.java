package rossum.common;

import robocode.*;
import robocode.robotinterfaces.IAdvancedEvents;
import robocode.robotinterfaces.IBasicEvents3;

public class Receiver implements IAdvancedEvents, IBasicEvents3 {
    private Board board;
    public Receiver(Board board){
        this.board=board;
    }

    @Override
    public void onStatus(StatusEvent statusEvent) {
        RobotStatus status = statusEvent.getStatus();
        board.previousTurn = board.currentTurn;
        board.currentTurn = new Turn(status);
        board.turns.add(board.currentTurn);
        board.time = status.getTime();
        board.out.println("onStatus");
    }

    @Override
    public void onSkippedTurn(SkippedTurnEvent skippedTurnEvent) {
        board.currentTurn.isSkipped = true;
    }

    @Override
    public void onCustomEvent(CustomEvent customEvent) {
    }

    @Override
    public void onRoundEnded(RoundEndedEvent roundEndedEvent) {
        board.currentTurn.isRoundEnded = true;
    }

    @Override
    public void onBattleEnded(BattleEndedEvent battleEndedEvent) {
        board.battleResults = battleEndedEvent.getResults();
    }

    @Override
    public void onBulletHit(BulletHitEvent bulletHitEvent) {
        Bullet bullet = bulletHitEvent.getBullet();
        board.myInactiveBullets.put(bullet.hashCode(), bullet);
        board.myActiveBullets.remove(bullet.hashCode());
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent bulletHitBulletEvent) {
        Bullet bullet = bulletHitBulletEvent.getBullet();
        board.myInactiveBullets.put(bullet.hashCode(), bullet);
        board.myActiveBullets.remove(bullet.hashCode());
    }

    @Override
    public void onBulletMissed(BulletMissedEvent bulletMissedEvent) {
        Bullet bullet = bulletMissedEvent.getBullet();
        board.myInactiveBullets.put(bullet.hashCode(), bullet);
        board.myActiveBullets.remove(bullet.hashCode());
    }

    @Override
    public void onDeath(DeathEvent deathEvent) {
    }

    @Override
    public void onHitByBullet(HitByBulletEvent hitByBulletEvent) {
    }

    @Override
    public void onHitRobot(HitRobotEvent hitRobotEvent) {
    }

    @Override
    public void onHitWall(HitWallEvent hitWallEvent) {
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent scannedRobotEvent) {
        board.otherRobot = scannedRobotEvent;
    }

    @Override
    public void onRobotDeath(RobotDeathEvent robotDeathEvent) {
    }

    @Override
    public void onWin(WinEvent winEvent) {
    }
}
