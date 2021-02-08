package marco.a.aguilar.locationguide;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


/**
 * todo: Create a timer that makes application go back to WelcomeFragment
 * after a minute or two, if the user hasn't done anything yet.
 *
 * todo: Create a simple screen that says "Are you ready to go to [Location]"
 * just in case the user accidentally clicks on a location while scrolling.
 *
 */

public class LocationsFragment extends Fragment
    implements OnRobotReadyListener, OnGoToLocationStatusChangedListener, Robot.AsrListener {

    private static final String TAG = "LocationsFragment";
    private static final String HOME_BASE = "urbes";

    // RecyclerView
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<String> mLocations;
    private String searchViewVoiceQuery;

    private SearchView searchView;

    // Temi
    private Robot mRobot;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        mAdapter = new LocationsAdapter(mLocations, mRobot);
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
        Robot.getInstance().addOnRobotReadyListener(this);
        Robot.getInstance().addOnGoToLocationStatusChangedListener(this);
        Robot.getInstance().addAsrListener(this);
    }


    public void onStop() {
        super.onStop();
        Robot.getInstance().removeOnRobotReadyListener(this);
        Robot.getInstance().removeOnGoToLocationStatusChangedListener(this);
        Robot.getInstance().removeAsrListener(this);
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
                mRobot.speak(request);

                /**
                 * Had to make mLocations an ArrayList or else clear() and
                 * addAll() didn't work properly and the app would crash.
                 */
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


    @Override
    public void onGoToLocationStatusChanged(String location, String status, int descriptionId, String description) {
        // todo: Check what other "description" values there are. Shahin wants to know if
        // Temi can detect moving obstacles.
        Log.d(TAG, "onGoToLocationStatusChanged: \n location: " + location + " status: " + status +
                " descriptionId: " + descriptionId + " description: " + description);

        /**
         * Todo: *** Remove home base as an option on the application. ****
         * Because this if statement will be entered if we are somewhere else, and
         * someone decides to select "home base" as the destination.
         *
         * This will all occur after we change HOME_BASE to "home base" instead of "urbes"
         */
        if(status.equals(OnGoToLocationStatusChangedListener.COMPLETE) && !location.equals(HOME_BASE)) {
            // Only go to NavigationCompleteFragment if we are not arriving at HOME_BASE
            goToNavigationCompleteFragment();
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
}
