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
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class NavigationCompleteFragment extends Fragment
        implements OnRobotReadyListener, Robot.AsrListener {

    private static final String TAG = "NavCompleteFragment";

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
    }


    public void onStop() {
        super.onStop();
        Robot.getInstance().removeOnRobotReadyListener(this);
        Robot.getInstance().removeAsrListener(this);
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
    public void onAsrResult(@NotNull String asrResult) {
        Log.d(TAG, "onAsrResult: " + asrResult);


        if(asrResult.toLowerCase().contains("no")) {
            mTimer.cancel();

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

            goToHomeBase();

        } else if (asrResult.toLowerCase().contains("yes")) {
            goToLocationFragment();
        } else {
            // Ask question again, hopefully creating a loop.
            mRobot.finishConversation();


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
     * Wait 3 seconds after saying "We have arrived" to say "Is there anything else I can help you with"
     * for the FIRST time only, then after we wait 10 seconds before asking again and so on.
     */
    private void navigationCompletePrompt() {

        Log.d(TAG, "navigationCompletePrompt: Asking Question...");
        mRobot.askQuestion("We have arrived to your location. Is there anything else I can help you with?");

        final Handler handler = new Handler(Looper.getMainLooper());

        final int[] counter = {0};

        TimerTask askUserIfFinished = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {

                        if(counter[0] < 3) {
                            Toast.makeText(getActivity(), "Is there anything else I can help you with?", Toast.LENGTH_SHORT).show();
                        } else {
                            goToHomeBase();
                            mTimer.cancel();
                        }

                        counter[0]++;

                    }
                });
            }
        };
        mTimer.schedule(askUserIfFinished, 3000, 12000);

    }

    private void goToHomeBase() {

        TtsRequest request = TtsRequest.create("Goodbye, have a nice day.", false);
        mRobot.speak(request);

        // Right now I'm simply making it go back to urbes.
        mRobot.goTo("urbes");

        /**
         * todo: Go to Home Base and only on arrival go back to Locations Fragment.
         * I'm not 100% sure if I need to do this though, maybe when the Robot goes
         * to Home Base it'll do it automatically, so save this for later.
         *
         * Might have to implement OnGoToLocationStatusChangedListener
         * to in order to go back to LocationsFragment (AKA HomeScreen in the future).
         */
    }


    /**
     * You should only put go to LocationsFragment here.
     * Leave the "Please select a location" speech for when the
     * user enters to LocationsFragment.
     */
    private void goToLocationFragment() {
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
                goToLocationFragment();
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
