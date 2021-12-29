package net.sf.robocode.plugin.kusto;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.StandardBlobTier;
import net.sf.robocode.io.FileUtil;
import net.sf.robocode.recording.BattleRecordFormat;
import net.sf.robocode.recording.RecordManager;
import net.sf.robocode.serialization.CsvWriter;
import net.sf.robocode.serialization.SerializableOptions;
import net.sf.robocode.settings.ISettingsManager;
import net.sf.robocode.version.IVersionManager;
import robocode.Rules;
import robocode.util.Utils;
import rossum.external.ExternalLogic;
import rossum.marius.MariusBoard;
import rossum.marius.MariusLogic;
import rossum.state.Wave;
import rossum.state.WaveProjection;
import rossum.state.WaveState;

import java.awt.geom.Point2D;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import static net.sf.robocode.io.Logger.logError;
import static net.sf.robocode.io.Logger.logMessage;

public class KustoRecordManager extends RecordManager {
    private String version;
    private CloudBlobContainer container;

    public KustoRecordManager(ISettingsManager properties, IVersionManager versionManager) {
        super(properties, versionManager);
        version = versionManager.getVersion();

        String storageConnectionString = System.getenv("RumbleStorageAccountContainer");
        if(storageConnectionString==null){
            return;
        }
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient cloudBlobClient = storageAccount.createCloudBlobClient();
            container = cloudBlobClient.getContainerReference("rumble");
        } catch (URISyntaxException | StorageException | InvalidKeyException e) {
            logError(e);
        }
    }

    @Override
    public void saveRecord(String recordFilename, BattleRecordFormat format, SerializableOptions options) {
        if (container != null) {
            uploadToKusto(recordFilename, options);
        } else {
            saveToFiles(recordFilename, options);
        }
    }

    private void uploadToKusto(String recordFilename, SerializableOptions options) {
        ByteArrayOutputStream fosResults = new ByteArrayOutputStream();
        ByteArrayOutputStream fosRounds = new ByteArrayOutputStream();
        ByteArrayOutputStream fosRobots = new ByteArrayOutputStream();
        ByteArrayOutputStream fosBullets = new ByteArrayOutputStream();
        ByteArrayOutputStream fosWaves = new ByteArrayOutputStream();

        try {
            processWaves(fosResults, fosRounds, fosRobots, fosBullets, fosWaves, options);
            int prefixLen = FileUtil.getBattlesDir().getCanonicalPath().length();
            recordFilename = recordFilename.substring(prefixLen + 1);

            uploadBlob(recordFilename, fosResults, ".results.csv");
            uploadBlob(recordFilename, fosRounds, ".rounds.csv");
            uploadBlob(recordFilename, fosRobots, ".robots.csv");
            uploadBlob(recordFilename, fosBullets, ".bullets.csv");
            uploadBlob(recordFilename, fosWaves, ".waves.csv");
        } catch (StorageException | URISyntaxException | IOException | ClassNotFoundException e) {
            logError(e);
        }
    }

    private void uploadBlob(String recordFilename, ByteArrayOutputStream fos, String suffix) throws IOException, StorageException, URISyntaxException {
        byte[] buffResults = fos.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffResults);
        CloudBlockBlob blob = container.getBlockBlobReference(recordInfo.battleId + "." + recordFilename + suffix);
        logMessage("Uploading to  " + blob.getUri());
        blob.upload(byteArrayInputStream, buffResults.length, StandardBlobTier.COOL, null, null, null);
    }


    private void saveToFiles(String recordFilename, SerializableOptions options) {
        FileOutputStream fosResults = null;
        FileOutputStream fosRounds = null;
        FileOutputStream fosBullets = null;
        FileOutputStream fosRobots = null;
        FileOutputStream fosWaves = null;

        try {
            fosResults = new FileOutputStream(recordFilename + ".results.csv");
            fosRounds = new FileOutputStream(recordFilename + ".rounds.csv");
            fosRobots = new FileOutputStream(recordFilename + ".robots.csv");
            fosBullets = new FileOutputStream(recordFilename + ".bullets.csv");
            fosWaves = new FileOutputStream(recordFilename + ".waves.csv");

            processWaves(fosResults, fosRounds, fosRobots, fosBullets, fosWaves, options);
        } catch (IOException | ClassNotFoundException e) {
            logError(e);
        } finally {
            FileUtil.cleanupStream(fosResults);
            FileUtil.cleanupStream(fosRounds);
            FileUtil.cleanupStream(fosBullets);
            FileUtil.cleanupStream(fosRobots);
            FileUtil.cleanupStream(fosWaves);
        }
    }


    private void processWaves(OutputStream fosResults, OutputStream fosRounds, OutputStream fosRobots, OutputStream fosBullets, OutputStream fosWaves, SerializableOptions options) throws IOException, ClassNotFoundException {
        final Charset utf8 = Charset.forName("UTF-8");

        OutputStreamWriter oswEvents = new OutputStreamWriter(fosWaves, utf8);
        CsvWriter cwrEvents = new CsvWriter(oswEvents, false);
        cwrEvents.startDocument("version,battleId,roundIndex,ownerName,victimName,bulletId,ownerIndex,firedTime,outcome,velocity,power,damage,heading," +
                "aimedOwnerX,aimedOwnerY,aimedOwnerBody,aimedOwnerVelocity,aimedOwnerEnergy,aimedOwnerGunHeat," +
                "aimedVictimX,aimedVictimY,aimedVictimBody,aimedVictimVelocity,aimedVictimEnergy,aimedVictimGunHeat," +
                "aimedBearing,aimedDistance,aimedDistanceTime,aimedRelativeAngle," +
                "firedOwnerX,firedOwnerY,firedOwnerBody,firedOwnerVelocity,firedOwnerEnergy," +
                "firedVictimX,firedVictimY,firedVictimBody,firedVictimVelocity,firedVictimEnergy,firedVictimGunHeat," +
                "firedBearing,firedDistance,firedDistanceTime,firedRelativeAngle," +
                "detectedVictimX,detectedVictimY,detectedVictimBody,detectedVictimVelocity,detectedVictimEnergy,detectedVictimGunHeat," +
                "detectedBearing,detectedDistance,detectedDistanceTime,detectedRelativeAngle," +
                "passedVictimX,passedVictimY,passedVictimBody,passedVictimVelocity,passedVictimEnergy,passedVictimGunHeat," +
                "passedBearing,passedDistance,passedDistanceTime,passedRelativeAngle," +
                "passingStartTime,hitTime,passingEndTime,passingStartAge,hitAge,passingEndAge"
        );

        ExternalLogic externalLogic = new ExternalLogic();
        externalLogic.shouldAssert = false;
        externalLogic.board = new MariusBoard();

        generateCsvRecord(fosResults, fosRounds, fosRobots, fosBullets, options, externalLogic::processTurn);

        try {
            List<Wave> toSort = new ArrayList<>(externalLogic.board.waves.values());
            toSort.sort((a, b) -> {
                if (a.firedRound != b.firedRound) return (a.firedRound - b.firedRound);
                return (int) (a.firedTime - b.firedTime);
            });

            for (Wave wave : toSort) {
                if (wave.outcome != WaveState.MOVING && wave.passingEndTime != null) {
                    writeProjection(cwrEvents, wave.finalProjection, options);
                }
            }

        } finally {
            FileUtil.cleanupStream(oswEvents);
        }
    }

    void writeProjection(CsvWriter cwrEvents, WaveProjection projection, SerializableOptions options) throws IOException {
        cwrEvents.writeValue(version);
        cwrEvents.writeValue(recordInfo.battleId.toString());
        Wave wave = projection.wave;
        cwrEvents.writeValue(wave.firedRound);
        cwrEvents.writeValue(wave.firedOwner.name);
        cwrEvents.writeValue(wave.firedVictim.name);
        cwrEvents.writeValue(wave.id);
        cwrEvents.writeValue(wave.ownerIndex);
        cwrEvents.writeValue(wave.firedTime);
        cwrEvents.writeValue(wave.outcome.toString());
        cwrEvents.writeValue(wave.velocity, options.trimPrecision);
        cwrEvents.writeValue(wave.power, options.trimPrecision);
        cwrEvents.writeValue(Rules.getBulletDamage(wave.power), options.trimPrecision);
        cwrEvents.writeValue(projection.bulletHeading, options.trimPrecision);

        // aimedOwnerX,aimedOwnerY,aimedOwnerBody,aimedOwnerVelocity,aimedOwnerEnergy,aimedOwnerGunHeat,
        cwrEvents.writeValue(wave.aimedOwner.x, options.trimPrecision);
        cwrEvents.writeValue(wave.aimedOwner.y, options.trimPrecision);
        cwrEvents.writeValue(wave.aimedOwner.body, options.trimPrecision);
        cwrEvents.writeValue(wave.aimedOwner.velocity, options.trimPrecision);
        cwrEvents.writeValue(wave.aimedOwner.energy, options.trimPrecision);
        cwrEvents.writeValue(wave.aimedOwner.gunHeat, options.trimPrecision);

        // aimedVictimX,aimedVictimY,aimedVictimBody,aimedVictimVelocity,aimedVictimEnergy,aimedVictimGunHeat,
        cwrEvents.writeValue(wave.aimedVictim.x, options.trimPrecision);
        cwrEvents.writeValue(wave.aimedVictim.y, options.trimPrecision);
        cwrEvents.writeValue(wave.aimedVictim.body, options.trimPrecision);
        cwrEvents.writeValue(wave.aimedVictim.velocity, options.trimPrecision);
        cwrEvents.writeValue(wave.aimedVictim.energy, options.trimPrecision);
        cwrEvents.writeValue(wave.aimedVictim.gunHeat, options.trimPrecision);
        // aimedBearing,aimedDistance,aimedDistanceTime,aimedRelativeAngle,
        writePosition(cwrEvents, wave.aimedOwner, wave.aimedVictim, wave.velocity, options); //current
        cwrEvents.writeValue(Utils.normalRelativeAngle(projection.bulletHeading - MariusLogic.absoluteBearing(wave.aimedOwner, wave.aimedVictim)), options.trimPrecision);// bullet relative angle to current victim angle at aim time

        // firedOwnerX,firedOwnerY,firedOwnerBody,firedOwnerVelocity,firedOwnerEnergy,firedOwnerGunHeat,
        cwrEvents.writeValue(wave.firedOwner.x, options.trimPrecision);
        cwrEvents.writeValue(wave.firedOwner.y, options.trimPrecision);
        cwrEvents.writeValue(wave.firedOwner.body, options.trimPrecision);
        cwrEvents.writeValue(wave.firedOwner.velocity, options.trimPrecision);
        cwrEvents.writeValue(wave.firedOwner.energy, options.trimPrecision);
        //cwrEvents.writeValue(wave.firedOwner.gunHeat, options.trimPrecision);

        // firedVictimX,firedVictimY,firedVictimBody,firedVictimVelocity,firedVictimEnergy,firedVictimGunHeat,
        cwrEvents.writeValue(wave.firedVictim.x, options.trimPrecision);
        cwrEvents.writeValue(wave.firedVictim.y, options.trimPrecision);
        cwrEvents.writeValue(wave.firedVictim.body, options.trimPrecision);
        cwrEvents.writeValue(wave.firedVictim.velocity, options.trimPrecision);
        cwrEvents.writeValue(wave.firedVictim.energy, options.trimPrecision);
        cwrEvents.writeValue(wave.firedVictim.gunHeat, options.trimPrecision);
        // firedBearing,firedDistance,firedDistanceTime,firedRelativeAngle,
        writePosition(cwrEvents, wave.firedOwner, wave.firedVictim, wave.velocity, options); //current
        cwrEvents.writeValue(Utils.normalRelativeAngle(projection.bulletHeading - MariusLogic.absoluteBearing(wave.firedOwner, wave.firedVictim)), options.trimPrecision);// bullet relative angle to current victim angle at fire time (guess future segment)

        // detectedVictimX,detectedVictimY,detectedVictimBody,detectedVictimVelocity,detectedVictimEnergy,detectedVictimGunHeat,
        cwrEvents.writeValue(wave.detectedVictim.x, options.trimPrecision);
        cwrEvents.writeValue(wave.detectedVictim.y, options.trimPrecision);
        cwrEvents.writeValue(wave.detectedVictim.body, options.trimPrecision);
        cwrEvents.writeValue(wave.detectedVictim.velocity, options.trimPrecision);
        cwrEvents.writeValue(wave.detectedVictim.energy, options.trimPrecision);
        cwrEvents.writeValue(wave.detectedVictim.gunHeat, options.trimPrecision);
        // TODO writePosition(cwrEvents, wave.detectedOwner, wave.detectedVictim, wave.velocity, options); //current
        // detectedBearing,detectedDistance,detectedDistanceTime,detectedRelativeAngle,
        writePosition(cwrEvents, wave.firedOwner, wave.detectedVictim, wave.velocity, options); //from origin
        cwrEvents.writeValue(Utils.normalRelativeAngle(projection.bulletHeading - MariusLogic.absoluteBearing(wave.firedOwner, wave.detectedVictim)), options.trimPrecision);// bullet relative angle to current victim angle at detect time

        // passedVictimX,passedVictimY,passedVictimBody,passedVictimVelocity,passedVictimEnergy,passedVictimGunHeat,
        cwrEvents.writeValue(wave.passedVictim.x, options.trimPrecision);
        cwrEvents.writeValue(wave.passedVictim.y, options.trimPrecision);
        cwrEvents.writeValue(wave.passedVictim.body, options.trimPrecision);
        cwrEvents.writeValue(wave.passedVictim.velocity, options.trimPrecision);
        cwrEvents.writeValue(wave.passedVictim.energy, options.trimPrecision);
        cwrEvents.writeValue(wave.passedVictim.gunHeat, options.trimPrecision);
        // TODO writePosition(cwrEvents, wave.passedOwner, wave.passedVictim, wave.velocity, options); //current
        // passedBearing,passedDistance,passedDistanceTime,passedRelativeAngle
        writePosition(cwrEvents, wave.firedOwner, wave.passedVictim, wave.velocity, options); //from origin, this is ideal bearing (actual segment)
        cwrEvents.writeValue(Utils.normalRelativeAngle(projection.bulletHeading - MariusLogic.absoluteBearing(wave.firedOwner, wave.passedVictim)), options.trimPrecision);// bullet relative angle to current victim angle at passed time (how much wrong)

        // passingStartTime,hitTime,passingEndTime,passingStartAge,hitAge,passingEndAge,
        cwrEvents.writeValue(wave.passingStartTime);
        cwrEvents.writeValue(wave.hitTime);
        cwrEvents.writeValue(wave.passingEndTime);
        cwrEvents.writeValue(wave.passingStartTime - wave.firedTime);
        cwrEvents.writeValue(wave.hitTime - wave.firedTime);
        cwrEvents.writeValue(wave.passingEndTime - wave.firedTime);

        cwrEvents.endLine();
    }

    void writePosition(CsvWriter cwrEvents, Point2D.Double source, Point2D.Double target, double velocity, SerializableOptions options) throws IOException {
        double distance = source.distance(target);
        double bearing = MariusLogic.absoluteBearing(source, target);
        int distanceTime = (int) (distance / velocity);

        cwrEvents.writeValue(bearing, options.trimPrecision);
        cwrEvents.writeValue(distance, options.trimPrecision);
        cwrEvents.writeValue(distanceTime);
    }
}