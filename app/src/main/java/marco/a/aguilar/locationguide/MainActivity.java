package marco.a.aguilar.locationguide;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        displayLocations();
        displayNavigationCompletePrompt();
    }

    // Use Fragment Transaction to go to LocationsFragment
    private void displayLocations() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        LocationsFragment locationsFragment = new LocationsFragment();
        fragmentTransaction.add(R.id.fragment_container, locationsFragment);
        fragmentTransaction.commit();
    }

    // Delete later, using for testing purposes only.
    private void displayNavigationCompletePrompt() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        NavigationCompleteFragment navigationCompleteFragment = new NavigationCompleteFragment();
        fragmentTransaction.add(R.id.fragment_container, navigationCompleteFragment);
        fragmentTransaction.commit();
    }
}