package marco.a.aguilar.locationguide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import androidx.recyclerview.widget.RecyclerView;

public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.LocationsViewHolder> {

    private ArrayList<String> mLocations;
    private ArrayList<String> mLocationsCopy;
    private static OnLocationItemClickedListener mOnLocationItemClickedListener;

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

    // https://stackoverflow.com/questions/30398247/how-to-filter-a-recyclerview-with-a-searchview
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

    @Override
    public LocationsAdapter.LocationsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.location_item, parent, false);

        return new LocationsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LocationsViewHolder holder, int position) {
        holder.textView.setText(mLocations.get(position));
    }

    @Override
    public int getItemCount() {
        return mLocations.size();
    }

    public static class LocationsViewHolder extends RecyclerView.ViewHolder {

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
