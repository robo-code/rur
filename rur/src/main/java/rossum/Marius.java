package rossum;

import rossum.common.BaseRobot;
import rossum.marius.MariusBoard;
import rossum.marius.MariusLogic;

public class Marius extends BaseRobot {

    public Marius() {
        super(new MariusLogic(new MariusBoard()));
    }
}

