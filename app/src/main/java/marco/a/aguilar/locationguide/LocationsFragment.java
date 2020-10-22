package marco.a.aguilar.locationguide;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.ContentValues.TAG;

public class LocationsFragment extends Fragment
    implements OnRobotReadyListener {

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

        // This will stop working if user rotates the device...need to do more research on this.
        // This is used to make the whole search bar clickable.
        SearchView searchView = (SearchView) view.findViewById(R.id.search_view);
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                searchView.setIconified(false);
            }
        });


        mRecyclerView = view.findViewById(R.id.locations_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView.
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new GridLayoutManager(getActivity(), 2);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mLocations = new ArrayList<>(mRobot.getLocations());



        // specify an adapter (see also next example)
        mAdapter = new LocationsAdapter(mLocations);

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

        mRecyclerView.setAdapter(mAdapter);

        // Inflate the layout for this fragment
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
     *
     * Needed to make onStart and onStop public for this to work. Now the mLocations
     * list has the right amount of items (16), the only issue now is updating the RecyclerView
     * grid items via notifyDataSetChanged.
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

                mAdapter.notifyDataSetChanged();


                Log.d(TAG, "onRobotReady: locations size: " + mLocations.size());

                final ActivityInfo activityInfo = getActivity().getPackageManager().getActivityInfo(getActivity().getComponentName(), PackageManager.GET_META_DATA);
                mRobot.onStart(activityInfo);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // TO-DO Create setupSearchView() and clean up your code.
}
