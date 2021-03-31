package marco.a.aguilar.locationguide;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NavigationCompleteFragment extends Fragment
        implements OnRobotReadyListener, Robot.AsrListener {

    private static final String TAG = "NavCompleteFragment";
    private static final String HOME_BASE = "home base";

    // Member variables
    Robot mRobot;

    // RxJava
    private Observable<Long> mIntervalObservable;
    private CompositeDisposable mDisposables;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_navigation_complete, container, false);

        mRobot = Robot.getInstance();

        mDisposables = new CompositeDisposable();

        mIntervalObservable = Observable
                .interval(15, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .take(3)
                .observeOn(AndroidSchedulers.mainThread());

        initButtons(view);

        return view;
    }

    public void onStart() {
        super.onStart();
        Robot.getInstance().addOnRobotReadyListener(this);
        Robot.getInstance().addAsrListener(this);

        navigationCompletePrompt();
    }


    public void onStop() {
        super.onStop();
        Robot.getInstance().removeOnRobotReadyListener(this);
        Robot.getInstance().removeAsrListener(this);

        removeObservers();
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
            mRobot.hideTopBar();

            navigationCompletePrompt();
        }

    }

    @Override
    public void onAsrResult(@NotNull String asrResult) {
        Log.d(TAG, "onAsrResult: " + asrResult);

        /**
         * IMPORTANT!!! Need to call finishConversation() or else
         * Temi will try to interpret the answer itself and say something
         * like "I'm not sure how to help with that". So for future reference,
         * whenever you use the onAsrResult() method with the Robot.askQuestion()
         * method you need to make sure you call finishConversation() right before
         * you make the robot speak again. This is what has worked so far.
         */
        mRobot.finishConversation();

        if(asrResult.toLowerCase().contains("no")) {

            returnHome();

        } else if (asrResult.toLowerCase().contains("yes") || asrResult.toLowerCase().contains("yeah") ||
                asrResult.toLowerCase().contains("sure")) {

            goToLocationsFragment();

        } else {
            TtsRequest request = TtsRequest.create("I'm sorry I couldn't understand. Please respond" +
                    " with a yes or no.", true);

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
     * Uses RxJava to ask user if they need anymore help (onSubscribe). Then
     * waits 15 seconds before asking again (onNext). After Temi has asked a third
     * time, it will call goToHomeBase() (onComplete).
     */
    private void navigationCompletePrompt() {

        mIntervalObservable.subscribe(new Observer<Long>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                mDisposables.add(d);
                // Asks the question the moment we subscribe.
                mRobot.askQuestion("We have arrived to your location. Is there anything else I can help you with?");
            }

            @Override
            public void onNext(@NonNull Long aLong) {
                Log.d(TAG, "onNext: aLong" + aLong);
                mRobot.askQuestion("We have arrived to your location. Is there anything else I can help you with?");
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Log.d(TAG, "onError: " + e);
            }

            @Override
            public void onComplete() {
                returnHome();
            }
        });

    }

    private void returnHome() {
        TtsRequest request = TtsRequest.create("Goodbye, have a nice day.", false);
        mRobot.speak(request);

        removeObservers();

        Intent intent = new Intent(getActivity(), ReturnHomeActivity.class);
        startActivity(intent);
    }


    private void goToLocationsFragment() {
        removeObservers();

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
                returnHome();
            }
        });
    }

    private void removeObservers() {
        mDisposables.clear();
    }

}
