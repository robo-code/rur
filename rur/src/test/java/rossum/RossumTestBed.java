package rossum;

import net.sf.robocode.io.Logger;
import org.junit.After;
import org.junit.Before;
import robocode.control.*;
import robocode.robotinterfaces.IBasicRobot;

import java.io.File;
import java.io.IOException;

public abstract class RossumTestBed<R extends IBasicRobot> extends RobotTestBed<R> {

    @Override
    protected void beforeInit() {
        if (!new File("").getAbsolutePath().endsWith("rur")) {
            throw new Error("Please run test with current directory in 'rur'");
        }

        // Check that robocode.home is defined and points to a robocode installation.
        String robocodeHome;
        String robotDevel;

        try {
            File robocodeHomePath = new File("../.sandbox").getCanonicalFile().getAbsoluteFile();
            File robocodeDevelPath = new File("build/classes/java/main").getCanonicalFile().getAbsoluteFile();
            robocodeHome = robocodeHomePath.getPath();
            robotDevel = robocodeDevelPath.getPath();
        } catch (IOException e) {
            e.printStackTrace(Logger.realErr);
            throw new Error(e);
        }
        // Set some system properties for use by the robocode engine.
        System.setProperty("robocode.home", robocodeHome);
        System.setProperty("WORKINGDIRECTORY", robocodeHome);
        System.setProperty("NOSECURITY", "true");
        System.setProperty("debug", "true");
        System.setProperty("robocode.options.development.path", robotDevel);
        System.setProperty("robocode.options.common.recordingFormat", "csv");
        System.setProperty("robocode.options.common.enableReplayRecording", "true");
        System.setProperty("robocode.options.common.enableAutoRecording", "true");

        super.beforeInit();
        //System.setProperty("robocode.options.battle.desiredTPS", "100");
    }

    @Override
    public boolean isEnableRecording() {
        return true;
    }

    @Before
    public void before() {
        super.before();
    }

    @After
    public void after() {
        super.after();
    }

}
