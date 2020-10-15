package marco.a.aguilar.locationguide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.LocationsViewHolder> {

    private String[] mLocations;

    // Provide a suitable constructor (depends on the kind of dataset)
    public LocationsAdapter(String[] locations) {
        mLocations = locations;
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
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.textView.setText(mLocations[position]);

    }

    @Override
    public int getItemCount() {
        return mLocations.length;
    }


    public static class LocationsViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case

        public TextView textView;

        public LocationsViewHolder(View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.location_text);
        }
    }
}
