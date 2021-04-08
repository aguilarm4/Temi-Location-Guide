package marco.a.aguilar.locationguide;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import org.jetbrains.annotations.NotNull;

public class ReturnHomeActivity extends AppCompatActivity
    implements OnRobotReadyListener, OnGoToLocationStatusChangedListener {

    private static final String TAG = "ReturnHomeActivity";
    private static final String HOME_BASE = "home base";
    Robot mRobot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_home);

        mRobot = Robot.getInstance();
    }

    public void onStart() {
        super.onStart();
        Robot.getInstance().addOnRobotReadyListener(this);
        Robot.getInstance().addOnGoToLocationStatusChangedListener(this);
    }


    public void onStop() {
        super.onStop();
        Robot.getInstance().removeOnRobotReadyListener(this);
        Robot.getInstance().removeOnGoToLocationStatusChangedListener(this);
    }

    /**
     * ToDo: Create a Utility class that sends an email to a list of Developers
     *  when the status of the OnGoToLocationStatusChangedListener is ABORT.
     *  Give it fields like "issue", "time", "date", etc.
     *  Then developers can implement this into their own code if they add another
     *  feature that might need this.
     */
    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, @NotNull String status,
                                            int descriptionId, @NotNull String description) {

        if(status.equals(OnGoToLocationStatusChangedListener.COMPLETE) && location.equals(HOME_BASE)) {
            goToHomeScreenActivity();
        }
    }

    @Override
    public void onRobotReady(boolean isReady) {
        if(isReady) {
            mRobot.goTo(HOME_BASE);
        }
    }

    private void goToHomeScreenActivity() {
        Intent intent = new Intent(this, HomeScreenActivity.class);
        startActivity(intent);
    }
}