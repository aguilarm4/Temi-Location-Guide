package marco.a.aguilar.locationguide;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.listeners.OnDetectionStateChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * todo: Create functionality where application checks if Robot is in Home Base,
 * if not, then go to Home Base after waiting 1 minute or something.
 */

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

        View exitView = view.findViewById(R.id.exit_view);
        exitView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRobot.showAppList();
            }
        });

        return view;
    }


    @Override
    public void onRobotReady(boolean isReady) {
        if(isReady) {

            // Tilt Robot head all up so that the text on the display is visible
            mRobot.tiltAngle(40);

            mRobot.hideTopBar();

            mRobot.setDetectionModeOn(true);

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
     *
     * todo: Change this to resetRobot() so that it checks if it's in home base
     * too. If not then return to Home base and setDetectionMode on,
     * if so, then just setDetectionMode on.
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

            TtsRequest request = TtsRequest.create("Have a nice day!", true);
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
