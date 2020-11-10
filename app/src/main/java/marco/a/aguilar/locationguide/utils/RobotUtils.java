package marco.a.aguilar.locationguide.utils;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.permission.Permission;

public class RobotUtils {

    public static boolean checkDetectionModeRequirements(Robot robot) {
        /**
         * Requirements for turning on Detection Mode:
         *  1) App must be the Kiosk app
         *  2) "Settings" permission
         */
        return robot.isSelectedKioskApp() && (robot.checkSelfPermission(Permission.SETTINGS) == Permission.GRANTED);
    }
}
