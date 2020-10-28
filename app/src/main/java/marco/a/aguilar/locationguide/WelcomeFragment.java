package marco.a.aguilar.locationguide;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.listeners.OnDetectionStateChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;
import com.robotemi.sdk.permission.Permission;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class WelcomeFragment extends Fragment implements
        OnDetectionStateChangedListener, OnRobotReadyListener, Robot.AsrListener {

    private static final String TAG = "WelcomeFragment";

    Robot mRobot;
    Handler mHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = layoutInflater.inflate(R.layout.fragment_welcome, container, false);

        mRobot = Robot.getInstance();
        mHandler = new Handler(Looper.getMainLooper());

        return view;
    }


    @Override
    public void onRobotReady(boolean isReady) {
        if(isReady) {

            // Tilt Robot head all up so that the text on the display is visible
            mRobot.tiltAngle(40);

            /**
             * For some odd reason, changing this value to 1.0 made it work.
             * I think I'm just going to not include the range for now.
             *
             * IMPORTANT:
             *  Also, calling setDetectionMode(true, range) will turn on
             *  the "Track User" feature until you manually turn it off.
             *
             *  The good news is that now we're getting printing out the log
             *  for onDetectionStateChanged() (Meaning it's working now).
             *
             *  So I think all we have to do now is turn it off and go to the
             *  next step.
             *
             *  IMPORTANT:
             *      The steps I took were
             *          1) Run the app on Temi
             *          2) Turn on Kiosk Mode
             *          3) Done
             *
             *      I didn't have to turn on Welcoming Mode or Greet Mode.
             *      I just had to manually enable the Settings permission
             *      for the app and turn on Kiosk Mode to make
             *      setDetectionModeOn() work.
             */

            mRobot.setDetectionModeOn(true);


//            Log.d(TAG, "onRobotReady: Setting Detection Mode on");
//            Log.d(TAG, "onRobotReady: isDetectionModeOn: " + mRobot.isDetectionModeOn());

            Log.d(TAG, "onRobotReady: Checking if selectedKioskApp");
            Log.d(TAG, "onRobotReady: isSelectedKioskApp(): " + mRobot.isSelectedKioskApp());

            /**
             * I had to manually go onto Settings -> Permissions -> Settings -> Location Guide
             * to turn on the Settings permission. Now mRobot.checkSelfPermission(Permission.SETTINGS)
             * is returning a value of 1 (AKA GRANTED)
             */
            Log.d(TAG, "onRobotReady: Checking for Settings permission....");
            Log.d(TAG, "onRobotReady: " + mRobot.checkSelfPermission(Permission.SETTINGS));
        }
    }

    @Override
    public void onDetectionStateChanged(int state) {

        if(state == OnDetectionStateChangedListener.DETECTED) {

            Log.d(TAG, "onDetectionStateChanged: User Detected ");

            /**
             * Turn off detection mode right after it detected a user
             * This will also keep the robot from following the User around (I hope)
             *
             * Attempting to turn on DetectionMode
             */
            mRobot.setDetectionModeOn(false);

            mRobot.askQuestion("Hello. May I help you find your location today?");

            /**
             * Going to wait 30 seconds before going back to
             * detecting again. Just in case user doens't respond or exists out
             * of question dialog
             */
            restartDetectionMode();

        }

    }

    /**
     * Uses member variable mHandler to turn on Detection Mode again
     * if the user has not given a response in 15 seconds.
     */
    private void restartDetectionMode() {
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRobot.setDetectionModeOn(true);
            }
        }, 15000);
    }

    @Override
    public void onAsrResult(String asrResult) {

        mRobot.finishConversation();

        Log.d(TAG, "onAsrResult: asrResult " + asrResult);

        if(asrResult.toLowerCase().contains("no")) {

            // Cancel mHandler.postDelayed() and restartDetectionMode()
            mHandler.removeCallbacksAndMessages(null);
            restartDetectionMode();

            TtsRequest request = TtsRequest.create("Have a nice day!", false);
            mRobot.speak(request);

        } else if (asrResult.toLowerCase().contains("yes")) {

            // Turn Off detection mode just in case it was turned on again due to the
            // delay inside onDetectionStateChanged()
            // Cancel handler postDelayed()
            mHandler.removeCallbacksAndMessages(null);
            mRobot.setDetectionModeOn(false);

            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);

        } else {

            // Cancel mHandler.postDelayed() and restartDetectionMode()
            mHandler.removeCallbacksAndMessages(null);
            restartDetectionMode();

            // Should create a loop
            mRobot.askQuestion("I'm sorry I couldn't understand. Please respond" +
                    " with a yes or no. Can I help you find your location?");

        }
    }

    public void onStart() {
        super.onStart();
        Robot.getInstance().addOnDetectionStateChangedListener(this);
        Robot.getInstance().addOnRobotReadyListener(this);
        Robot.getInstance().addAsrListener(this);
    }


    public void onStop() {
        super.onStop();
        Robot.getInstance().removeOnDetectionStateChangedListener(this);
        Robot.getInstance().removeOnRobotReadyListener(this);
        Robot.getInstance().removeAsrListener(this);
    }


}
