package rossum;

import net.sf.robocode.io.Logger;
import org.junit.After;
import org.junit.Before;
import robocode.control.*;
import robocode.robotinterfaces.IBasicRobot;

import java.io.File;
import java.io.IOException;

public abstract class RobocodeTestBed<R extends IBasicRobot> extends RobotTestBed<R> {

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
        System.setProperty("robocode.options.development.path", robotDevel);

        super.beforeInit();
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
