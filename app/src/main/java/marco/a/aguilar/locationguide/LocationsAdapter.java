package marco.a.aguilar.locationguide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.robotemi.sdk.Robot;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;

public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.LocationsViewHolder> {

    private ArrayList<String> mLocations;
    // Used for filter()
    private ArrayList<String> mLocationsCopy;

    /**
     * I feel like making this static and public probably isn't a good idea
     * or "good practice". Going to fix this later but for now I need this to work.
     */
    public static Robot mRobot;


    // Provide a suitable constructor (depends on the kind of dataset)
    public LocationsAdapter(ArrayList<String> locations, Robot robot) {

        mLocations = locations;

        // Copy that's going to be used for filter()
        mLocationsCopy = new ArrayList<>();
        mLocationsCopy.addAll(mLocations);

        mRobot = robot;
    }

    public void setLocationsCopy(ArrayList<String> locations) {
        mLocationsCopy.clear();
        mLocationsCopy.addAll(locations);
    }

    // Code taken from:
    // https://stackoverflow.com/questions/30398247/how-to-filter-a-recyclerview-with-a-searchview
    // However, onQueryTextSubmit() should return false, or else keyboard won't close when clicking the
    // search button.
    public void filter(String text) {
        mLocations.clear();
        if(text.isEmpty()){
            mLocations.addAll(mLocationsCopy);
        } else{
            text = text.toLowerCase();
            for(String location: mLocationsCopy){
                if(location.toLowerCase().contains(text)){
                    mLocations.add(location);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public LocationsAdapter.LocationsViewHolder onCreateViewHolder(ViewGroup parent,
                                                            int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.location_item, parent, false);

        LocationsViewHolder vh = new LocationsViewHolder(view);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(LocationsViewHolder holder, int position) {
        holder.textView.setText(mLocations.get(position));
    }

    @Override
    public int getItemCount() {
        return mLocations.size();
    }


    public static class LocationsViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case

        public TextView textView;
        public Button buttonStartNavigation;

        public LocationsViewHolder(View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.location_text);
            buttonStartNavigation = itemView.findViewById(R.id.button_start_navigation);


            buttonStartNavigation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String location = textView.getText().toString();

                    mRobot.goTo(location);

                    Toast.makeText(itemView.getContext(), "Navigating to " + location, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
