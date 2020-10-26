package marco.a.aguilar.locationguide;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


/**
 * This is what I think should happen next.
 * "Can I guide you to where you want to go??"
 *
 * 1) Create onClick listeners for each button, printing
 * out a simple toast showing the name of a location. (DONE)
 *
 * 2) Maybe, pass the robot to the Adapter so that we can call
 * goToLocation(String location) with inside the onClick listener. (DONE)
 *
 * 3) Implement OnGoToLocationStatusChangedListener inside this fragment
 * and print out the "status" value (DONE)
 *
 * 4) Make squares smaller in order to display more items, since Shahin wants there to
 * be more names displayed.
 *
 * 5) Once #3 is done, create the Fragment that will display the 'Yes' and 'No'
 * buttons for the 'Do you need any more assistance?', and the logic. We can
 * add the Speech dialog later and just add it onto our logic. Just make sure
 * you write clean code. Remember that this will toggle autoReturn and will need
 * to prompt the user 3 times (every 10 seconds). So maybe we should only stay inside
 * this NavigationCompleteFragment for 30 seconds before going back to the LocationsFragment.
 *
 * For now, since we don't have the voice dialog implemented, just show a Toast.LONG that
 * says "You still there?" or something like that.
 */

public class LocationsFragment extends Fragment
    implements OnRobotReadyListener, OnGoToLocationStatusChangedListener {

    private static final String TAG = "LocationsFragment";

    // RecyclerView
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<String> mLocations;

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

        mLayoutManager = new GridLayoutManager(getActivity(), 2);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Should return 0 locations at first, until robot is ready.
        mLocations = new ArrayList<>(mRobot.getLocations());


        // specify an adapter (see also next example)
        mAdapter = new LocationsAdapter(mLocations, mRobot);
        mRecyclerView.setAdapter(mAdapter);

        /**
         * Needs to be called AFTER initializing mAdapter
         */
        initSearchView(view);

        // Inflate the layout for this fragment
        return view;
    }


    public void onStart() {
        super.onStart();
        Robot.getInstance().addOnRobotReadyListener(this);
        Robot.getInstance().addOnGoToLocationStatusChangedListener(this);
    }


    public void onStop() {
        super.onStop();
        Robot.getInstance().removeOnRobotReadyListener(this);
        Robot.getInstance().removeOnGoToLocationStatusChangedListener(this);
    }

    /**
     * Needed to make onStart and onStop public for this to work.
     */
    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            try {
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
    public void onGoToLocationStatusChanged(String location, String status, int descriptionId, String description) {
        Log.d(TAG, "onGoToLocationStatusChanged: \n location: " + location + " status: " + status +
                " descriptionId: " + descriptionId + " description: " + description);

        if(status.equals(OnGoToLocationStatusChangedListener.COMPLETE)) {
            // todo: Move this code to next Fragment. NavigationCompleteFragment
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

                        if(counter[0] < 5) {
                            Toast.makeText(getActivity(), "Is there anything else I can help you with?", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "Going back home!", Toast.LENGTH_SHORT).show();
                            timer.cancel();
                        }

                        counter[0]++;

                    }
                });
            }
        };
        timer.schedule(askUserIfFinished, 3000, 12000);

    }


    private void initSearchView(View view) {
        // This will stop working if user rotates the device...need to do more research on this.
        // This is used to make the whole search bar clickable.
        SearchView searchView = (SearchView) view.findViewById(R.id.search_view);
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
