package com.example.turismo;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class LocationDataStore {
    private static LocationDataStore instance;
    private List<UserLocation> userLocations;

    private LocationDataStore() {
        userLocations = new ArrayList<>();
    }

    public static synchronized LocationDataStore getInstance() {
        if (instance == null) {
            instance = new LocationDataStore();
        }
        return instance;
    }

    public List<UserLocation> getUserLocations() {
        return new ArrayList<>(userLocations);
    }

    public void setUserLocations(List<UserLocation> userLocations) {
        this.userLocations = new ArrayList<>(userLocations);
        if (this.userLocations != null)
        {
            Log.d("DATASTORE", "DATASTORE");

        }
    }
}
