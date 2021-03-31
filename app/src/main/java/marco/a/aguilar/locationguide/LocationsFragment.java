package marco.a.aguilar.locationguide;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;


/**
 * todo: Create a timer that makes application go back to WelcomeFragment
 * after a minute or two, if the user hasn't done anything yet.
 *
 */

public class LocationsFragment extends Fragment
    implements OnRobotReadyListener, OnGoToLocationStatusChangedListener, Robot.AsrListener,
        LocationsAdapter.OnLocationItemClickedListener {

    private static final String TAG = "LocationsFragment";
    private static final String HOME_BASE = "home base";

    // RecyclerView
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<String> mLocations;
    private String searchViewVoiceQuery;

    private SearchView searchView;

    // Temi
    private Robot mRobot;

    private Boolean mIsCompletingTrip = false;
    private boolean mWasInterrupted = false;

    private SharedPreferences mSharedPreferences;

    // RxJava
    private Observable<Long> mLocationFragmentIntervalObservable;
    private CompositeDisposable mLocationFragmentDisposables;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        mSharedPreferences = context.getSharedPreferences(
                getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE
        );

        mLocationFragmentDisposables = new CompositeDisposable();
        /**
         * Executes once after 2 minutes, if Temi is not currently
         * completing a trip, then it will execute the code inside
         * onNext().
         */
        mLocationFragmentIntervalObservable = Observable
                .interval(2, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .take(1)
                .takeWhile((t) -> !mIsCompletingTrip)
                .observeOn(AndroidSchedulers.mainThread());


        mLocationFragmentIntervalObservable.subscribe(new Observer<Long>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                mLocationFragmentDisposables.add(d);
            }

            @Override
            public void onNext(@NonNull Long aLong) {
                Log.d(TAG, "onNext: RxJava onNext() called...");
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Log.d(TAG, "onError: " + e);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete: RxJava onComplete() called...");
                // Return Temi Home.
                Intent intent = new Intent(getActivity(), ReturnHomeActivity.class);
                startActivity(intent);
            }

        });
    }


    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_locations, container, false);

        mRobot = Robot.getInstance();

        mRecyclerView = view.findViewById(R.id.locations_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView.
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new GridLayoutManager(getActivity(), 4);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Should return 0 locations at first, until robot is ready.
        mLocations = new ArrayList<>(mRobot.getLocations());

        // specify an adapter (see also next example)
        mAdapter = new LocationsAdapter(mLocations, this);
        mRecyclerView.setAdapter(mAdapter);

        /**
         * Needs to be called AFTER initializing mAdapter
         */
        initButtons(view);
        initSearchView(view);


        // Inflate the layout for this fragment
        return view;
    }


    public void onStart() {
        super.onStart();
        /**
         * Calls onRobotReady(), even after the Temi robot is disturbed.
         *
         * When blocked, it will give the user 2 options
         *  1)
         */
        Robot.getInstance().addOnGoToLocationStatusChangedListener(this);
        Robot.getInstance().addOnRobotReadyListener(this);
        Robot.getInstance().addAsrListener(this);
    }


    public void onStop() {
        super.onStop();
        Robot.getInstance().removeOnRobotReadyListener(this);
        Robot.getInstance().removeOnGoToLocationStatusChangedListener(this);
        Robot.getInstance().removeAsrListener(this);

        removeObservers();
    }

    private void removeObservers() {
        mLocationFragmentDisposables.clear();
    }


    private void initButtons(View view) {
        Button voiceSearchButton = view.findViewById(R.id.voice_search_button);

        voiceSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRobot.askQuestion("What is your location?");
            }
        });

    }

    /**
     * Needed to make onStart and onStop public for this to work.
     */
    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            try {
                // Tilt Robot head so the user doesn't have a hard time pressing buttons.
                mRobot.tiltAngle(55);

                // Temi will say this every time the user goes to LocationsFragment
                TtsRequest request = TtsRequest.create("Scroll down to select a location. You may also" +
                        " enter a search if you'd like.", true);

                mWasInterrupted = mSharedPreferences.getBoolean(
                        getString(R.string.locations_fragment_was_interrupted),
                        false);

                Log.d(TAG, "onRobotReady: mWasInterrupted: " + mWasInterrupted);

                if(!mWasInterrupted)
                    mRobot.speak(request);

                mLocations.clear();
                mLocations.addAll(mRobot.getLocations());

                /**
                 * Update mLocationsCopy for RecyclerView adapter. When adapter
                 * is initialized, mLocations and the copy won't have any items
                 * until the onRobotReady is called.
                 */
                ((LocationsAdapter) mAdapter).setLocationsCopy(mLocations);
                mAdapter.notifyDataSetChanged();

                final ActivityInfo activityInfo = getActivity().getPackageManager().getActivityInfo(getActivity().getComponentName(), PackageManager.GET_META_DATA);
                mRobot.onStart(activityInfo);

            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onAsrResult(String asrResult) {

        mRobot.finishConversation();

        /**
         * We're assuming that the only time we are receiving an ASR result is
         * because the user clicked the voice search button, that's why we're doing
         * the filter operation here.
         *
         * I also noticed that the Robot has trouble writing in the voice search. Might
         * need to find a better way to get around this issue instead of hard coding the
         * names.
         */
        if(asrResult.length() > 0) {

            if(asrResult.toLowerCase().contains("shaheen")) {
                asrResult = "Shahin";
            }

            // Set the text for searchview. setIconified() is set to false so that
            // asrResult text shows up on the searchView
            searchView.setIconified(false);
            searchView.setQuery(asrResult,true);
            ((LocationsAdapter) mAdapter).filter(asrResult);
        }

    }


    /**
     * We're going to try this approach with our RxJava Observable.
     * Using takeWhile() with a Boolean value.
     * https://stackoverflow.com/questions/39323167/how-to-stop-interval-from-observable
     * https://stackoverflow.com/questions/41070443/rxjava-how-to-stop-and-resume-a-hot-observable-interval
     *
     * Todo: Figure out if you can use this method to toggle mIsCompletingTrip.
     *   Check if OnGoToLocationStatusChangedListener.START is being printed
     *   if Temi is blocked, and the user selects "YES" when prompted to retry.
     *   In this situation, setting mIsCompletingTrip to "true" would work in our favor.
     *
     * Todo: Next figure out what to do if the user selects "NO". If the user selects "NO",
     *   then we know our Fragment's onStart() method will be called and thus the onRobotReady()
     *   So, if OnGoToLocationStatusChangedListener.ABORT, then we toggle mIsCompletingTrip to "false".
     *
     * Todo: Now, we don't want the Robot to speak if mIsCompletingTrip is "true", due to the user
     *  selecting the retry option. So we'll wrap the speak() method around an if-statement
     *  (if !mIsCompletingTrip, then we speak)
     *
     * Todo: Test out your hypothesis by writing the appropriate logs, once you see that your app
     *  working the way you envisioned, then add the RxJava stuff.
     *
     *  If mWasInturrputed, then we don't speak
     *
     */
    @Override
    public void onGoToLocationStatusChanged(String location, String status, int descriptionId, String description) {
        Log.d(TAG, "onGoToLocationStatusChanged: \n location: " + location + " status: " + status +
                " descriptionId: " + descriptionId + " description: " + description);

        Log.d(TAG, "onGoToLocationStatusChanged: mIsCompletingTrip: " + mIsCompletingTrip);

        /**
         * Calculating/START are always the first statuses printed when the user selects
         * a location to go to or when it starts up again (When user selects retry)
         * so, this is where we should add the mIsCompletingTrip change
         *
         * We want to use this so that our Observable can determine whether to go
         * to Home Base or not, we don't need to toggle this value to "false" anywhere
         * because there are only 2 cases when it's false:
         *      1) The user comes to this view for the first time, in which case mIsCompletingTrip
         *      is already initialized as "false"
         *
         *      2) The user selects "Yes" or "No" when Temi is blocked, and the activity will
         *      start up again, which means mIsCompletingTrip will be initialized again as "false"
         */
        if(status.equals(OnGoToLocationStatusChangedListener.CALCULATING) || status.equals(OnGoToLocationStatusChangedListener.START))
            Log.d(TAG, "onGoToLocationStatusChanged: Switching mIsCompletingTrip to true...");
            mIsCompletingTrip = true;

        /**
         * In both situation of completing a trip, Home Base or not, we want to reset
         * the value for mWasInterrupted.
         */
        if(status.equals(OnGoToLocationStatusChangedListener.COMPLETE)) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(getString(R.string.locations_fragment_was_interrupted), false);
            editor.apply();
        }

        if(status.equals(OnGoToLocationStatusChangedListener.COMPLETE) && !location.equals(HOME_BASE)) {
            // Only go to NavigationCompleteFragment if we are not arriving at HOME_BASE
            goToNavigationCompleteFragment();
        }

        if(status.equals(OnGoToLocationStatusChangedListener.COMPLETE) && location.equals(HOME_BASE)) {
            goToWelcomeScreen();
        }

        /**
         * Save mWasInterrupted value to true
         */
        if(status.equals(OnGoToLocationStatusChangedListener.ABORT)) {
            Log.d(TAG, "onGoToLocationStatusChanged: Temi was interrupted");

            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(getString(R.string.locations_fragment_was_interrupted), true);
            editor.apply();
        }

        // In case a human is in the way.
        if(description.equals("Height Obstacle")) {
            TtsRequest request = TtsRequest.create("Excuse me.", true);
            mRobot.speak(request);
        }

    }

    private void goToNavigationCompleteFragment() {
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new NavigationCompleteFragment());
        transaction.commit();
    }

    private void goToWelcomeScreen() {
        Intent intent = new Intent(getActivity(), HomeScreenActivity.class);
        startActivity(intent);
    }


    private void initSearchView(View view) {
        // This will stop working if user rotates the device...need to do more research on this.
        // This is used to make the whole search bar clickable.
        searchView = (SearchView) view.findViewById(R.id.search_view);

        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setIconified(false);
            }
        });


        // Make sure mAdapter is initialized before calling initSearchView()
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                ((LocationsAdapter) mAdapter).filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                ((LocationsAdapter) mAdapter).filter(newText);
                return true;
            }
        });
    }

    @Override
    public void onLocationClicked(String location) {
        AlertDialog alertDialog = new AlertDialog.Builder(Objects.requireNonNull(getActivity())).create();

        alertDialog.setMessage("Ready to go to " + location + "?");

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Start",
                (dialogInterface, i) -> {
                    // Robot asks users to follow
                    TtsRequest request = TtsRequest.create("Please follow me. I am going to " + location, true);
                    mRobot.speak(request);
                    mRobot.goTo(location);
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialogInterface, i) -> dialogInterface.dismiss());

        alertDialog.show();

        // Need to place this after show()
        TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
        textView.setTextSize(40);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(10,10,10,10);
        textView.setLayoutParams(params);

        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setTextSize(28);

        Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        negativeButton.setTextSize(28);

    }
}
