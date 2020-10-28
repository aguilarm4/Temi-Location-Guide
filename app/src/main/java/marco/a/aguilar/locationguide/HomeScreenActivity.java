package marco.a.aguilar.locationguide;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

public class HomeScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        displayWelcomeFragment();
    }

    private void displayWelcomeFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        WelcomeFragment welcomeFragment = new WelcomeFragment();
        fragmentTransaction.add(R.id.home_screen_fragment_container, welcomeFragment);
        fragmentTransaction.commit();
    }
}