package com.example.turismo;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.auth.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class MapFragment extends Fragment {

    private static final int FINE_PERMISSION_CODE = 1;
    private GoogleMap myMap;
    public static String GROUP_ID;
    private Location currentLocation;
    private PlacesClient placesClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Polyline currentPolyline;
    private Marker currentMarker;
    private Marker locationMarker;
    private Marker targetMarker;
    private List<Marker> currentMarkers = new ArrayList<>();
    private List<String> currentCategories = new ArrayList<>();
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private List<String> selectedPlaceTypes = new ArrayList<>();


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.maps_api_key));
        }
        placesClient = Places.createClient(requireContext());
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    myMap = googleMap;
                    setupMap();
                    setupSearchView(view);

                    // Check if there are any member locations to display
                    Bundle args = getArguments();
                    if (args != null) {
                        ArrayList<UserLocation> userLocations = args.getParcelableArrayList("userLocations");
                        if (userLocations != null && !userLocations.isEmpty()) {
                            showMembersLocation(userLocations);
                        }
                    }

                    listenForTargetLocationChanges();
                }
            });
        }

        MaterialButton selectPlaceTypesButton = view.findViewById(R.id.select_place_types_button);
        selectPlaceTypesButton.setOnClickListener(v -> {
            SelectPlaceTypesFragment selectPlaceTypesFragment = SelectPlaceTypesFragment.newInstance(new ArrayList<>(selectedPlaceTypes));
            selectPlaceTypesFragment.setPlaceTypesSelectedListener(selectedTypes -> {
                selectedPlaceTypes = selectedTypes;

                    fetchNearbyPlaces(myMap, selectedTypes);
            });
            FragmentManager fragmentManager = getChildFragmentManager();
            selectPlaceTypesFragment.show(fragmentManager, "SelectPlaceTypesFragment");
        });

        startLocationUpdates(); // Start location updates when the view is created
    }

    private void setupMap() {
        if (myMap != null) {
            pinpointCurrentLocation();

            // Set up click listener to show BottomSheet
            myMap.setOnMarkerClickListener(marker -> {
                if (marker.getTag() instanceof UserLocation) {
                    String s = (String) ((UserLocation) marker.getTag()).getUsername();

                    if (s != null) {
                        Toast.makeText(requireContext(), "Username: " , Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                PlaceResult placeResult = (PlaceResult) marker.getTag();
                if (placeResult != null) {
                    LocationBottomSheetFragment bottomSheet = LocationBottomSheetFragment.newInstance(placeResult.location.latitude, placeResult.location.longitude, placeResult, placesClient);
                    bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
                }
                return true; // Return true to indicate that we have consumed the event and no further processing is necessary
            });

            // Set up long press detection for markers
            myMap.setOnMapLongClickListener(latLng -> {
                // Find the closest marker to the long press
                Marker closestMarker = null;
                float[] distance = new float[1];
                for (Marker marker : currentMarkers) {
                    Location.distanceBetween(latLng.latitude, latLng.longitude, marker.getPosition().latitude, marker.getPosition().longitude, distance);
                    if (distance[0] < 100) { // 100 meters threshold for long press detection
                        closestMarker = marker;
                        break;
                    }
                }
                if (closestMarker != null) {
                    Object tag = closestMarker.getTag();
                    LatLng destination = null;
                    if (tag instanceof PlaceResult) {
                        PlaceResult placeResult = (PlaceResult) tag;
                        destination = new LatLng(placeResult.location.latitude, placeResult.location.longitude);
                    } else if (tag instanceof UserLocation) {
                        UserLocation userLocation = (UserLocation) tag;
                        destination = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                    }

                    if (destination != null) {
                        if (currentMarker != null && currentMarker.equals(closestMarker)) {
                            if (currentPolyline != null) {
                                currentPolyline.remove();
                                currentPolyline = null;
                            }
                            currentMarker = null;
                        } else {
                            if (currentLocation != null) {
                                LatLng origin = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                requestDirections(origin, destination);
                                currentMarker = closestMarker;
                            } else {
                                Toast.makeText(requireContext(), "Current location not available", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            });


            myMap.setOnMapClickListener(latLng -> {
                PlaceResult placeResult = new PlaceResult(latLng);
                //CoordonatesBottomSheetFragment bottomSheet = CoordonatesBottomSheetFragment.newInstance(placeResult.location.latitude, placeResult.location.longitude, placeResult, placesClient);
                //bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
            });
        } else {
            Log.e("SetupMap", "Google Map is not initialized");
        }
    }

    private void pinpointCurrentLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        getLastLocation();
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                currentLocation = location;
                addMarkerToCurrentLocation();
            }
        });
    }

    private void addMarkerToCurrentLocation() {
        if (currentLocation != null && myMap != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            if (locationMarker != null) {
                locationMarker.remove();
            }
            locationMarker = myMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));
            locationMarker.setIcon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(requireContext(), R.drawable.current_location)));
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
        } else {
            Toast.makeText(requireContext(), "Unable to fetch current location", Toast.LENGTH_SHORT).show();
        }
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, @DrawableRes int drawableId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, drawableId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private void setupSearchView(View view) {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        if (autocompleteFragment != null && location != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            autocompleteFragment.setLocationBias(RectangularBounds.newInstance(
                    new LatLng(location.getLatitude() - 0.1, location.getLongitude() - 0.1),
                    new LatLng(location.getLatitude() + 0.1, location.getLongitude() + 0.1)
            ));
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    String placeId = place.getId();
                    List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
                    FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);
                    placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
                        Place selectedPlace = response.getPlace();
                        LatLng latLng = selectedPlace.getLatLng();
                        if (latLng != null) {
                            Marker marker = myMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.getName()));
                            getPlaceDetails(placeId, placeResult -> {
                                Marker mrk = myMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(placeResult.location.latitude, placeResult.location.longitude))
                                        .title(placeResult.name)
                                        .snippet(placeResult.address));
                                marker.setTag(placeResult);
                            });
                            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0f));
                            Toast.makeText(requireContext(), "Place: " + selectedPlace.getName() + "\nAddress: " + selectedPlace.getAddress(), Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener((exception) -> {
                        Log.e("Places", "Place not found: " + exception.getMessage());
                    });
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.e("Places", "An error occurred: " + status);
                }
            });
        }
    }

    private void requestDirections(LatLng origin, LatLng destination) {
        String apiKey = getString(R.string.maps_api_key);
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray routes = jsonObject.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                            String points = overviewPolyline.getString("points");
                            drawRoute(points);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("DirectionsAPI", "Error fetching directions: " + error.getMessage())
        );
        queue.add(stringRequest);
    }

    private void drawRoute(String encodedPolyline) {
        if (currentPolyline != null) {
            currentPolyline.remove();
        }
        List<LatLng> points = decodePoly(encodedPolyline);
        PolylineOptions polylineOptions = new PolylineOptions().addAll(points).color(Color.BLUE).width(10);
        currentPolyline = myMap.addPolyline(polylineOptions);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    public void fetchNearbyPlaces(final GoogleMap googleMap, List<String> placeTypes) {
        // Clear current markers before fetching new places
        clearCurrentMarkers();

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        String apiKey = getString(R.string.maps_api_key);

        for (String placeType : placeTypes) {
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                    "?location=" + currentLocation.getLatitude() + "," + currentLocation.getLongitude() +
                    "&radius=" + 1000 +
                    "&type=" + placeType +
                    "&key=" + apiKey;

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    response -> {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONArray results = jsonObject.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject result = results.getJSONObject(i);
                                String placeId = result.getString("place_id");
                                getPlaceDetails(placeId, placeResult -> {
                                    Marker marker = googleMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(placeResult.location.latitude, placeResult.location.longitude))
                                            .title(placeResult.name)
                                            .snippet(placeResult.address));
                                    marker.setTag(placeResult);
                                    if (placeResult.iconUrl != null) {
                                        new DownloadImageTask(marker, googleMap).execute(placeResult.iconUrl);
                                    } else {
                                        if (placeType.equals("restaurant"))
                                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.restaurant));
                                        else if (placeType.equals("atm"))
                                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.atm));
                                    }

                                    // Add marker to the current markers list
                                    currentMarkers.add(marker);
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }, error -> Toast.makeText(requireContext(), "Error fetching places: " + error.getMessage(), Toast.LENGTH_LONG).show());

            queue.add(stringRequest);
        }
    }


    private void clearMarkersByCategory(String category) {
        List<Marker> markersToRemove = new ArrayList<>();
        for (Marker marker : currentMarkers) {
            PlaceResult placeResult = (PlaceResult) marker.getTag();
            if (placeResult != null && placeResult.getTypes() != null && placeResult.getTypes().contains(category)) {
                markersToRemove.add(marker);
            }
        }
        for (Marker marker : markersToRemove) {
            marker.remove();
            currentMarkers.remove(marker);
        }
    }

    public void getPlaceDetails(String placeId, Consumer<PlaceResult> callback) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG,
                Place.Field.TYPES, Place.Field.RATING, Place.Field.PHONE_NUMBER, Place.Field.WEBSITE_URI,
                Place.Field.OPENING_HOURS, Place.Field.PRICE_LEVEL, Place.Field.PHOTO_METADATAS, Place.Field.ICON_URL
        );

        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, fields);
        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();
            PlaceResult result = new PlaceResult(
                    place.getName(),
                    place.getAddress(),
                    place.getLatLng(),
                    place.getTypes(),
                    place.getRating() != null ? place.getRating() : -1,
                    place.getPhoneNumber(),
                    place.getWebsiteUri() != null ? place.getWebsiteUri().toString() : null,
                    place.getOpeningHours(),
                    String.valueOf(place.getPriceLevel()),
                    place.getPhotoMetadatas(),
                    place.getIconUrl() // Added icon URL field
            );
            callback.accept(result);
        }).addOnFailureListener(e -> {
            Log.e("Places", "Failed to fetch place details: " + e.getMessage());
        });
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(60000); // 1 minute
        locationRequest.setFastestInterval(30000); // 30 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = location;
                        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        if (locationMarker != null) {
                            locationMarker.setPosition(currentLatLng);
                        } else {
                            locationMarker = myMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));
                            locationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.atm));
                        }
                        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                        updateLocationInFirestore(currentLocation);
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void updateLocationInFirestore(Location location) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            String locationString = location.getLatitude() + "," + location.getLongitude();
            firestore.collection("users").document(userId)
                    .update("location", locationString)
                    .addOnSuccessListener(aVoid -> Log.d("MapFragment", "Location updated successfully"))
                    .addOnFailureListener(e -> Log.e("MapFragment", "Failed to update location", e));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    // Method to show member locations on the map
    public void showMembersLocation(List<UserLocation> userLocations) {
        if (myMap != null) {
            // Clear current markers
            clearCurrentMarkers();

            // Add markers for each member
            for (UserLocation userLocation : userLocations) {
                LatLng latLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(userLocation.getUsername());

                Marker marker = myMap.addMarker(markerOptions);
                marker.setTag(userLocation);
                currentMarkers.add(marker);

                // Fetch profile picture and set as marker icon
                String profileImageUrl = userLocation.getProfileImageUrl();
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    new DownloadAndSetImageTask(marker).execute(profileImageUrl);
                }
            }

            if (!userLocations.isEmpty()) {
                LatLng firstLocation = new LatLng(userLocations.get(0).getLatitude(), userLocations.get(0).getLongitude());
                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 10));
            }
        } else {
            Toast.makeText(requireContext(), "Google Map is not initialized", Toast.LENGTH_SHORT).show();
        }
    }


    private void listenForTargetLocationChanges() {
        String groupId = GROUP_ID; // Implement this method to retrieve the current group ID
        if (groupId == null) return;

        DocumentReference groupRef = firestore.collection("groups").document(groupId);
        groupRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("MapFragment", "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    String targetLocationString = snapshot.getString("targetLocation");
                    if (targetLocationString != null) {
                        String[] parts = targetLocationString.split(",");
                        if (parts.length == 2) {
                            double latitude = Double.parseDouble(parts[0]);
                            double longitude = Double.parseDouble(parts[1]);
                            updateTargetLocationMarker(latitude, longitude);
                        }
                    }
                } else {
                    Log.d("MapFragment", "Current data: null");
                }
            }
        });
    }

    private void updateTargetLocationMarker(double latitude, double longitude) {
        if (myMap != null) {
            LatLng targetLatLng = new LatLng(latitude, longitude);
            if (targetMarker != null) {
                targetMarker.setPosition(targetLatLng);
            } else {
                targetMarker = myMap.addMarker(new MarkerOptions()
                        .position(targetLatLng)
                        .title("Group Target Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 15));
        }
    }

    public void addTargetLocationMarker(double latitude, double longitude) {
        if (myMap != null) {
            LatLng targetLatLng = new LatLng(latitude, longitude);
            myMap.addMarker(new MarkerOptions()
                    .position(targetLatLng)
                    .title("Group Target Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 15));
        }
    }

    private String getGroupId() {
        // Implement this method to retrieve the current group ID
        return null; // Placeholder, implement as needed
    }

    private void clearCurrentMarkers() {
        // Check if currentMarkers list is not empty
        if (currentMarkers != null && !currentMarkers.isEmpty()) {
            // Iterate through the list of markers
            for (Marker marker : currentMarkers) {
                // Remove each marker from the map
                marker.remove();
            }
            // Clear the list of markers
            currentMarkers.clear();
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private Marker marker;
        private GoogleMap googleMap;

        public DownloadImageTask(Marker marker, GoogleMap googleMap) {
            this.marker = marker;
            this.googleMap = googleMap;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11 != null ? Bitmap.createScaledBitmap(mIcon11, 64, 64, false) : null; // Resize the bitmap to 64x64 pixels
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(result));
            }
        }
    }



    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private class DownloadAndSetImageTask extends AsyncTask<String, Void, Bitmap> {
        private Marker marker;
        private static final int MARKER_IMAGE_SIZE = 100; // Adjust the size as needed

        public DownloadAndSetImageTask(Marker marker) {
            this.marker = marker;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            try {
                InputStream in = new java.net.URL(url).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                if (bitmap != null) {
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, MARKER_IMAGE_SIZE, MARKER_IMAGE_SIZE, false);
                    return getCircularBitmap(resizedBitmap);
                } else {
                    return null;
                }
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(result));
            }
        }
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int x = (bitmap.getWidth() - size) / 2;
        int y = (bitmap.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(bitmap, x, y, size, size);
        if (squaredBitmap != bitmap) {
            bitmap.recycle();
        }

        Bitmap circularBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(circularBitmap);
        Paint paint = new Paint();
        BitmapShader shader = new BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);

        squaredBitmap.recycle();
        return circularBitmap;
    }

}
