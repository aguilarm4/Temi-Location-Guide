package marco.a.aguilar.locationguide;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import marco.a.aguilar.locationguide.utils.RobotUtils;

public class WelcomeFragment extends Fragment implements
        OnDetectionStateChangedListener, OnRobotReadyListener, Robot.AsrListener {

    private static final String TAG = "WelcomeFragment";

    Robot mRobot;

    // Used to turn on Detection Mode after a 15 second delay.
    private Observable<Long> mTimeDelayObservable;
    // Object used to remove Observers if the Android OS kills the Activity
    private CompositeDisposable mDisposables;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = layoutInflater.inflate(R.layout.fragment_welcome, container, false);

        mRobot = Robot.getInstance();

        mDisposables = new CompositeDisposable();

        mTimeDelayObservable = Observable
                .timer(15, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

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

            if(RobotUtils.checkDetectionModeRequirements(mRobot)) {
                // No delay the first time detection mode is turned on.
                mRobot.setDetectionModeOn(true);
            } else {
                /**
                 * Check for Kiosk and Settings requirements, if not then present toast to
                 * let Developers know.
                 */
                if(!(mRobot.isSelectedKioskApp())) {
                    Toast.makeText(getActivity(), "Must be selected as Kiosk app" +
                            " to start detecting", Toast.LENGTH_LONG).show();
                }

                if(!(mRobot.checkSelfPermission(Permission.SETTINGS) == Permission.GRANTED)) {
                    Toast.makeText(getActivity(), "Must enable Settings permission" +
                            " to start detecting", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    @Override
    public void onDetectionStateChanged(int state) {

        if(state == OnDetectionStateChangedListener.DETECTED) {
            /**
             * Turn off detection mode right after user detected.
             * This will also prevent Temi from following the User around.
             */
            mRobot.setDetectionModeOn(false);

            mRobot.askQuestion("Hello. May I help you find your location today?");

            restartDetectionMode();
        }

    }

    private void removeObservers() {
        mDisposables.clear();
    }

    private void restartDetectionMode() {

        mTimeDelayObservable.subscribe(new Observer<Long>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                mDisposables.add(d);
            }

            @Override
            public void onNext(@NonNull Long aLong) {
                if(RobotUtils.checkDetectionModeRequirements(mRobot)) {
                    mRobot.setDetectionModeOn(true);
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Log.d(TAG, "onError: " + e);
            }

            @Override
            public void onComplete() {}
        });
    }

    @Override
    public void onAsrResult(String asrResult) {

        mRobot.finishConversation();

        removeObservers();

        if(asrResult.toLowerCase().contains("no")) {
            restartDetectionMode();

            TtsRequest request = TtsRequest.create("Have a nice day!", true);
            mRobot.speak(request);

        } else if (asrResult.toLowerCase().contains("yes") || asrResult.toLowerCase().contains("yeah") ||
                asrResult.toLowerCase().contains("sure")) {
            // Turn off detection mode before entering next state.
            mRobot.setDetectionModeOn(false);

            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);

        } else {
            restartDetectionMode();

            // Creates a loop for onAsrResult()
            mRobot.askQuestion("I'm sorry I couldn't understand. Please respond" +
                    " with a yes or no. Can I help you find your location?");
        }
    }

    public void onStart() {
        super.onStart();
        Robot.getInstance().addOnDetectionStateChangedListener(this);
        Robot.getInstance().addOnRobotReadyListener(this);
        Robot.getInstance().addAsrListener(this);

        restartDetectionMode();
    }


    public void onStop() {
        super.onStop();
        Robot.getInstance().removeOnDetectionStateChangedListener(this);
        Robot.getInstance().removeOnRobotReadyListener(this);
        Robot.getInstance().removeAsrListener(this);

        removeObservers();

        // Turn off Detection Mode if User taps robot on the Head or tries
        // to go somewhere else besides the app.
        mRobot.setDetectionModeOn(false);
    }

}
