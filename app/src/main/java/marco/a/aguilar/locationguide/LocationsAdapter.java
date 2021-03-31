package marco.a.aguilar.locationguide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;

/**
 *
 * todo
 * Create onClickListener implementation inside LocationsFragment, it needs to take in a String location.
 * So override OnCLick inside LocationsFragment. Then inside this adapter you create a constructor
 * LocationsAdapter(locations, robot, onClickListner) (on LocationsFragment side you just initialize
 * by doing LocationsAdapter(locations, robot, this)).
 *
 * Then in here you simply set it to the static member variable mOnClickListener.
 * and in LocationsViewHolder you set the listener on the itemView and pass
 * the String location value.
 *
 * Then on the LocationsFragment side, all you gotta do is use the "location" string
 * to show the overlay, make the robot speak, and go to the location.
 *
 */

public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.LocationsViewHolder> {

    private ArrayList<String> mLocations;
    // Used for filter()
    private ArrayList<String> mLocationsCopy;

    private static OnLocationItemClickedListener mOnLocationItemClickedListener;


    // Provide a suitable constructor (depends on the kind of dataset)
    public LocationsAdapter(ArrayList<String> locations,
                            OnLocationItemClickedListener onLocationItemClickedListener) {

        mLocations = locations;

        // Copy that's going to be used for filter()
        mLocationsCopy = new ArrayList<>();
        mLocationsCopy.addAll(mLocations);

        mOnLocationItemClickedListener = onLocationItemClickedListener;
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
            /**
             * Might have to sort here too.
             */
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

        public LocationsViewHolder(View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.location_text);

            itemView.setOnClickListener(view -> {
                String location = textView.getText().toString();
                mOnLocationItemClickedListener.onLocationClicked(location);
            });
        }
    }

    public interface OnLocationItemClickedListener {

        void onLocationClicked(String location);
    }
}
