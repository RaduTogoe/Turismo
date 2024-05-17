package com.example.turismo;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.turismo.databinding.ActivityMapBinding;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements LocationBottomSheetFragment.OnLocationAddedListener {
    private ActivityMapBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check if the activity was started with member locations to display
        List<UserLocation> userLocations = LocationDataStore.getInstance().getUserLocations();
        if (userLocations != null && !userLocations.isEmpty()) {
            MapFragment mapFragment = new MapFragment();
            Bundle args = new Bundle();
            Log.d("ACTIVITY", "ACTIVITY");
            args.putParcelableArrayList("userLocations", new ArrayList<>(userLocations));
            mapFragment.setArguments(args);
            replaceFragment(mapFragment);
        } else {
            replaceFragment(new MapFragment());
        }

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.mapItem) {
                replaceFragment(new MapFragment());
            } else if (item.getItemId() == R.id.accountItem) {
                replaceFragment(new AccountFragment());
            } else if (item.getItemId() == R.id.groupItem) {
                replaceFragment(new GroupFragment());
            } else if (item.getItemId() == R.id.calendarItem) {
                replaceFragment(new CalendarFragment());
            }
            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_frame, fragment)
                .addToBackStack("fragmentTransaction")
                .commit();
    }

    @Override
    public void onLocationAdded(String name, double lat, double lng) {
        CalendarFragment calendarFragment = (CalendarFragment) getSupportFragmentManager().findFragmentByTag("CALENDAR_FRAGMENT_TAG");
        if (calendarFragment != null) {
            calendarFragment.addLocation(name, lat, lng);
        } else {
            calendarFragment = CalendarFragment.newInstance(name, lat, lng);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame, calendarFragment, "CALENDAR_FRAGMENT_TAG")
                    .addToBackStack(null)
                    .commit();
        }
    }
}
