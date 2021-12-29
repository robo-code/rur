package rossum;

import robocode.*;
import robocode.robotinterfaces.*;
import robocode.robotinterfaces.peer.IAdvancedRobotPeer;
import robocode.robotinterfaces.peer.IBasicRobotPeer;
import rossum.marius.MariusBoard;
import rossum.marius.MariusLogic;
import rossum.state.*;
import rossum.state.Robot;

import java.awt.*;
import java.io.PrintStream;

public class Marius implements IAdvancedRobot, Runnable, IPaintRobot, IPaintEvents, IAdvancedEvents, IBasicEvents3 {
    public PrintStream out;
    public MariusBoard board;
    public MariusLogic logic;
    public IAdvancedRobotPeer peer;

    public Marius() {
        board = new MariusBoard();
        logic = new MariusLogic();
        logic.setBoard(board);
    }

    @Override
    public IAdvancedEvents getAdvancedEventListener() {
        return this;
    }

    @Override
    public Runnable getRobotRunnable() {
        return this;
    }

    @Override
    public IBasicEvents getBasicEventListener() {
        return this;
    }

    @Override
    public void setPeer(IBasicRobotPeer peer) {
        this.peer = (IAdvancedRobotPeer) peer;
    }

    @Override
    public void setOut(PrintStream printStream) {
        this.out = printStream;
    }

    @Override
    public IPaintEvents getPaintEventListener() {
        return this;
    }

    @Override
    public void run() {
        peer.setAdjustGunForBodyTurn(true);
        peer.setAdjustRadarForGunTurn(true);

        peer.setEventPriority("ScannedRobotEvent", 98);
        peer.addCustomEvent(new Condition("turnLogic", 9) {
            public boolean test() {
                return !board.self.isDead;
            }
        });

        board.round = peer.getRoundNum();

        while (!board.self.isDead) {
            peer.setMove(board.setMove);
            if (board.setFire != 0) {
                if (board.self.energy > board.setFire) {
                    Bullet bullet = peer.setFire(board.setFire);
                    int bulletId = bullet.hashCode();
                    Wave wave = Wave.createWave(bulletId, 0, board.setFire, board.round, board.time,
                            board.previousTurn.enemy != null ? board.previousTurn.enemy : board.lastEnemy,
                            board.currentTurn.enemy,
                            null,
                            board.previousTurn.self,
                            board.currentTurn.self,
                            null
                    );
                    board.waves.put(bulletId, wave);

                } else {
                    board.setFire = 0;
                }
            }
            peer.setTurnBody(board.setBody);
            peer.setTurnGun(board.setGun);
            peer.setTurnRadar(board.setRadar);

            peer.execute();// other robots, my next tick events
        }
    }

    @Override
    public void onStatus(StatusEvent statusEvent) {
        RobotStatus status = statusEvent.getStatus();
        board.time = status.getTime();
        board.previous2Turn = board.previousTurn;
        board.previousTurn = board.currentTurn;
        board.currentTurn = Turn.createTurn(status.getRoundNum(), board.time);
        board.turns.add(board.currentTurn);
        board.prevEnemy = board.enemy;
        board.prevSelf = board.self;
        board.enemy = null;
        board.self = Myself.createSelf(peer.getName(), status, board.time);
        if (board.prevSelf == null) {
            board.enemyNextBulletId = 1 + (1 * 10000) + (status.getRoundNum() * 1000000);
        } else {
            board.self.isDead = board.prevSelf.isDead;
        }
        board.self.firedPower = board.setFire;
        board.setFire = 0;
        board.currentTurn.self = board.self;
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent scannedRobotEvent) {
        double enemyGunHeat = Math.max(board.lastEnemy == null
                        ? 3 - (MariusLogic.GUN_COOLING_RATE * board.time)
                        : board.lastEnemy.gunHeat - (MariusLogic.GUN_COOLING_RATE * (board.time - board.lastEnemy.time))
                , 0);

        board.currentTurn.enemy = Robot.createEnemy(board.self, scannedRobotEvent, enemyGunHeat, board.time);

        board.enemy = board.currentTurn.enemy;
        board.lastEnemy = board.currentTurn.enemy;
    }

    @Override
    public void onCustomEvent(CustomEvent e) {
        logic.turnLogic();
    }

    @Override
    public void onBattleEnded(BattleEndedEvent battleEndedEvent) {
        board.battleResults = battleEndedEvent.getResults();
        board.enemyNextBulletId = 0;
    }

    @Override
    public void onBulletHit(BulletHitEvent bulletHitEvent) {
        Bullet bullet = bulletHitEvent.getBullet();
        int bulletId = bullet.hashCode();
        // board.bullets.remove(bulletId);
        // board.waves.remove(bulletId);
        board.enemy.hitByBulletPower = bullet.getPower();
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent bulletHitBulletEvent) {
        Bullet bullet = bulletHitBulletEvent.getBullet();
        int bulletId = bullet.hashCode();
        // board.bullets.remove(bulletId);
        // board.waves.remove(bulletId);
        // TODO Bullet enemyBullet = bulletHitBulletEvent.getHitBullet()
        // TODO board.allBullets.remove(enemyBullet.hashCode());
        // TODO process new knowledge
    }

    @Override
    public void onBulletMissed(BulletMissedEvent bulletMissedEvent) {
        Bullet bullet = bulletMissedEvent.getBullet();
        int bulletId = bullet.hashCode();
        // board.bullets.remove(bulletId);
        // board.waves.remove(bulletId);
    }

    @Override
    public void onHitRobot(HitRobotEvent hitRobotEvent) {
        board.currentTurn.hitOther = hitRobotEvent;
        board.self.hitOther = true;
        if (board.enemy != null) {
            board.enemy.hitOther = true;
        }
    }

    @Override
    public void onHitWall(HitWallEvent hitWallEvent) {
        board.self.hitWall = true;
        board.currentTurn.hitWall = hitWallEvent;
    }

    @Override
    public void onHitByBullet(HitByBulletEvent hitByBulletEvent) {
        Bullet bullet = hitByBulletEvent.getBullet();
        board.currentTurn.hitByBullet = hitByBulletEvent;
        board.self.hitByBulletPower = hitByBulletEvent.getPower();
        board.self.hitByBulletId = hitByBulletEvent.hashCode();
        // TODO board.allBullets.remove(bullet.hashCode());
        // TODO process new knowledge
    }

    @Override
    public void onPaint(Graphics2D graphics2D) {
        if (board.enemy != null) {
            board.enemy.onPaint(graphics2D);
        }
        for (WaveProjection projection : board.currentTurn.waveProjections.values()) {
            if (projection.state != WaveState.GONE) {
                projection.onPaint(graphics2D);
            }
        }
    }

    @Override
    public void onSkippedTurn(SkippedTurnEvent skippedTurnEvent) {
        board.currentTurn.skipped = skippedTurnEvent;
    }

    @Override
    public void onRoundEnded(RoundEndedEvent roundEndedEvent) {
        board.currentTurn.roundEnded = roundEndedEvent;
    }

    @Override
    public void onDeath(DeathEvent deathEvent) {
        board.currentTurn.death = deathEvent;
        board.self.isDead = true;
    }

    @Override
    public void onRobotDeath(RobotDeathEvent robotDeathEvent) {
        if (board.enemy != null) {
            board.enemy.isDead = true;
        }
    }

    @Override
    public void onWin(WinEvent winEvent) {
    }
}

