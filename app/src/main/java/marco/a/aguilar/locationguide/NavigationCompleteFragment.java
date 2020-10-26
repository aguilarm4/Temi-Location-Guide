package marco.a.aguilar.locationguide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
        }

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
                Toast.makeText(getActivity(), "Going back to Home Base!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
