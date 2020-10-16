package marco.a.aguilar.locationguide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LocationsFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = layoutInflater.inflate(R.layout.fragment_locations, container, false);

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

        ArrayList<String> locations = new ArrayList<>();
        locations.add("Shahin Mastian");
        locations.add("Bryan Sastokas");
        locations.add("Doug Anderson");
        locations.add("Printer");
        locations.add("Chandel Buelna");
        locations.add("Tiffany Gaw");
        locations.add("Katie Lau");
        locations.add("Bathroom");


        // specify an adapter (see also next example)
        mAdapter = new LocationsAdapter(locations);

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

    // TO-DO Create setupSearchView() and clean up your code.
}
