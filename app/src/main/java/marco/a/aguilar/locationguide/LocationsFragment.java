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
import java.util.Collections;
import java.util.Comparator;
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

    // Temi
    private Robot mRobot;

    // RxJava
    private Observable<Long> mLocationFragmentIntervalObservable;
    private CompositeDisposable mLocationFragmentDisposables;

    private Boolean mIsCompletingTrip = false;
    private SharedPreferences mSharedPreferences;
    private SearchView searchView;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        mSharedPreferences = context.getSharedPreferences(
                getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE
        );

        mLocationFragmentDisposables = new CompositeDisposable();

        /**
         * Executes after 3 minutes. If Temi is not completing a trip (is idle),
         * then onComplete() is called and the application goes through ReturnHomeActivity
         * and then HomeScreenActivity.
         *
         * Go the idea from these posts:
         *      https://stackoverflow.com/questions/39323167/how-to-stop-interval-from-observable
         *      https://stackoverflow.com/questions/41070443/rxjava-how-to-stop-and-resume-a-hot-observable-interval
         */
        mLocationFragmentIntervalObservable = Observable
                .interval(3, TimeUnit.MINUTES)
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
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Log.d(TAG, "onError: " + e);
            }

            @Override
            public void onComplete() {
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

        initButtons(view);

        // Needs to be called AFTER initializing mAdapter
        initSearchView(view);

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

    // Need to make onStart() and onStop() public for this to work.
    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            try {
                // Tilt Temi's head has an easy time pressing buttons.
                mRobot.tiltAngle(55);

                boolean wasInterrupted = mSharedPreferences.getBoolean(
                        getString(R.string.locations_fragment_was_interrupted),
                        false);

                if(!wasInterrupted) {
                    TtsRequest request = TtsRequest.create("Scroll down to select a location. You may also" +
                            " enter a search if you'd like.", true);
                    mRobot.speak(request);
                }

                mLocations.clear();
                mLocations.addAll(mRobot.getLocations());

                // Sort locations before notifying adapter and setting the copy.
                Collections.sort(mLocations, new Comparator<String>() {
                    @Override
                    public int compare(String location1, String location2) {
                        return location1.compareTo(location2);
                    }
                });

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


    @Override
    public void onGoToLocationStatusChanged(String location, String status, int descriptionId, String description) {

        /**
         * CALCULATING/START are always the first statuses printed when the user selects
         * a location to go to or when it starts up again (When user selects retry)
         * So this is where we should set mIsCompletingTrip to true"
         *
         * Our Observable will determine whether to go to ReturnHomeActivity based on mIsCompletingTrip
         * Don't set mIsCompletingTrip to "false" anywhere bc there are 2 cases when it'll hold this value:
         *
         *      1) LocationFragment opens for the first time, thus mIsCompletingTrip is initialized as "false"
         *
         *      2) The user selects "Yes" or "No" after the Temi OS interrupts the application. The fragment will
         *      start up again and mIsCompletingTrip will be initialized again as "false"
         */
        if(status.equals(OnGoToLocationStatusChangedListener.CALCULATING) || status.equals(OnGoToLocationStatusChangedListener.START))
            mIsCompletingTrip = true;

        // Reset wasInterrupted for the next trip
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
         * Using wasInterrupted to determine whether Temi should speak when
         * starting a trip. In the case where it couldn't find its destination,
         * we don't want Temi to speak again when Temi returns to this Fragment.
         */
        if(status.equals(OnGoToLocationStatusChangedListener.ABORT)) {
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
        // This is used to make the whole search bar clickable.
        searchView = (SearchView) view.findViewById(R.id.search_view);
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setIconified(false);
            }
        });


        // mAdapter must be initialized before calling initSearchView()
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                ((LocationsAdapter) mAdapter).filter(query);
                // Return "false" to close keyboard when clicking the search button.
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                ((LocationsAdapter) mAdapter).filter(newText);
                return true;
            }
        });
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
