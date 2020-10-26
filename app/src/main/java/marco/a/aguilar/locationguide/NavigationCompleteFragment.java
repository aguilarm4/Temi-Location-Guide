package marco.a.aguilar.locationguide;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class NavigationCompleteFragment extends Fragment implements OnRobotReadyListener {

    Robot mRobot;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_navigation_complete, container, false);

        mRobot = Robot.getInstance();

        initButtons(view);

        return view;
    }

    public void onStart() {
        super.onStart();
        Robot.getInstance().addOnRobotReadyListener(this);
    }


    public void onStop() {
        super.onStop();
        Robot.getInstance().removeOnRobotReadyListener(this);
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

    /**
     * Wait 3 seconds after saying "We have arrived" to say "Is there anything else I can help you with"
     * for the FIRST time only, then after we wait 10 seconds before asking again and so on.
     */
    private void navigationCompletePrompt() {
        Toast.makeText(getActivity(), "We have arrived", Toast.LENGTH_SHORT).show();

        final Handler handler = new Handler(Looper.getMainLooper());

        final int[] counter = {0};

        Timer timer = new Timer();
        TimerTask askUserIfFinished = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {

                        if(counter[0] < 3) {
                            Toast.makeText(getActivity(), "Is there anything else I can help you with?", Toast.LENGTH_SHORT).show();
                        } else {
                            goHome();
                            timer.cancel();
                        }

                        counter[0]++;

                    }
                });
            }
        };
        timer.schedule(askUserIfFinished, 3000, 12000);

    }

    private void goHome() {
        Toast.makeText(getActivity(), "Going back to Home Base!", Toast.LENGTH_SHORT).show();

        // Go to Home Base and only on arrival go back to Locations Fragment.
    }

    private void initButtons(View view) {
        Button yesButton = view.findViewById(R.id.button_nav_complete_yes);
        Button noButton = view.findViewById(R.id.button_nav_complete_no);

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Going back to Locations", Toast.LENGTH_SHORT).show();
            }
        });

        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goHome();
            }
        });
    }
}
