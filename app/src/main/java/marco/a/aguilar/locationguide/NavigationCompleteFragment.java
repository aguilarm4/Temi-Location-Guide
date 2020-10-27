package marco.a.aguilar.locationguide;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class NavigationCompleteFragment extends Fragment
        implements OnRobotReadyListener, Robot.AsrListener, OnGoToLocationStatusChangedListener {

    private static final String TAG = "NavCompleteFragment";
    private static final String HOME_BASE = "urbes";

    // Member variables
    Robot mRobot;
    Timer mTimer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_navigation_complete, container, false);

        mRobot = Robot.getInstance();
        mTimer = new Timer();

        initButtons(view);

        return view;
    }

    public void onStart() {
        super.onStart();
        Robot.getInstance().addOnRobotReadyListener(this);
        Robot.getInstance().addAsrListener(this);
        Robot.getInstance().addOnGoToLocationStatusChangedListener(this);
    }


    public void onStop() {
        super.onStop();
        Robot.getInstance().removeOnRobotReadyListener(this);
        Robot.getInstance().removeAsrListener(this);
        Robot.getInstance().removeOnGoToLocationStatusChangedListener(this);
    }

    /**
     * It seems like if you want to call methods associated with the Temi SDK,
     * you need to make sure that you call it inside onRobotReady().
     */
    @Override
    public void onRobotReady(boolean isReady) {

        if(isReady) {
            // Tilt Robot head so the user doesn't have a hard time pressing buttons.
            mRobot.tiltAngle(55);

            navigationCompletePrompt();
        }

    }

    @Override
    public void onGoToLocationStatusChanged(String location, String status, int descriptionId, String description) {
        Log.d(TAG, "onGoToLocationStatusChanged: \n location: " + location + " status: " + status +
                " descriptionId: " + descriptionId + " description: " + description);


        /**
         * Only go to LocationFragment when the user is completing a trip to HOME_BASE
         */
        if(status.equals(OnGoToLocationStatusChangedListener.COMPLETE) && location.equals(HOME_BASE)) {
            // Timer.cancel() called twice but after arriving to HOME_BASE but this is okay, documentation
            // says it will have no effect.
            goToLocationsFragment();
        }

    }


    @Override
    public void onAsrResult(@NotNull String asrResult) {
        Log.d(TAG, "onAsrResult: " + asrResult);

        /**
         * IMPORTANT!!! We need to call finishConversation() or else
         * Temi will try to interpret the answer itself and say something
         * like "I'm not sure how to help with that" or something along those
         * lines. So for future reference, whenever you use the onAsrResult()
         * method with the Robot.askQuestion() method you need to make sure you
         * call finishConversation() right before you make the robot speak again.
         * this is what has worked so far.
         */
        mRobot.finishConversation();

        if(asrResult.toLowerCase().contains("no")) {

            goToHomeBase();

        } else if (asrResult.toLowerCase().contains("yes")) {

            goToLocationsFragment();

        } else {
            TtsRequest request = TtsRequest.create("I'm sorry I couldn't understand. Please respond" +
                    " with a yes or no.", false);

            mRobot.speak(request);
        }

        /**
         * todo: Check if string equals "yes" or "no" (make sure to lowercase asrResult just in case)
         * and based on results, call goToHomeBase() or goToLocationsFragment()
         *
         * todo: Create an infinite loop if Temi is unable to understand what the user is saying.
         *
         * You can call askQuestion() again but before that you can have the robot speak and say
         * "I'm sorry, I couldn't understand, please respond with a Yes or No."
         */

    }

    /**
     * Waits 0 seconds before asking User if they need more help and repeats every 15 seconds.
     */
    private void navigationCompletePrompt() {

        final Handler handler = new Handler(Looper.getMainLooper());

        final int[] counter = {0};

        TimerTask askUserIfFinished = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {

                        if(counter[0] < 3) {
                            // Ask question again
                            mRobot.askQuestion("We have arrived to your location. Is there anything else I can help you with?");
                        } else {
                            goToHomeBase();
                        }

                        counter[0]++;

                    }
                });
            }
        };
        /**
         * Making this 15 seconds because the prompt stays on the screen for about 10
         * seconds. This will give the User 5 seconds to click the buttons instead before
         * asking again.
         */
        mTimer.schedule(askUserIfFinished, 0, 15000);
    }

    private void goToHomeBase() {

        TtsRequest request = TtsRequest.create("Goodbye, have a nice day.", false);
        mRobot.speak(request);

        mTimer.cancel();

        // For now HOME_BASE is urbes.
        mRobot.goTo(HOME_BASE);

        /**
         * todo: Go to HOME_BASE and only on arrival go back to Locations Fragment.
         * I'm not 100% sure if I need to do this though, maybe when the Robot goes
         * to Home Base it'll do it automatically, so save this for later.
         *
         * Might have to implement OnGoToLocationStatusChangedListener
         * to in order to go back to LocationsFragment (AKA HomeScreen in the future).
         *
         * You're going to have to check for "location" and only go back to LocationsFragment
         * if status is COMPLETE and location is HOME_BASE
         */
    }


    /**
     * You should only put go to LocationsFragment here.
     * Leave the "Please select a location" speech for when the
     * user enters to LocationsFragment.
     */
    private void goToLocationsFragment() {

        mTimer.cancel();

        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new LocationsFragment());
        transaction.commit();
    }


    private void initButtons(View view) {
        Button yesButton = view.findViewById(R.id.button_nav_complete_yes);
        Button noButton = view.findViewById(R.id.button_nav_complete_no);

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToLocationsFragment();
            }
        });

        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToHomeBase();
            }
        });
    }
}
