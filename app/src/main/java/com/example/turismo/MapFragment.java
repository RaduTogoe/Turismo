package com.example.turismo;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class MapFragment extends Fragment {

    private static final int FINE_PERMISSION_CODE = 1;
    private GoogleMap myMap;
    private Location currentLocation;
    private PlacesClient placesClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Polyline currentPolyline;
    private Marker currentMarker;
    private Marker locationMarker;
    private List<Marker> currentMarkers = new ArrayList<>();
    private String currentCategory = null;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

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
                        Log.d("COKE", "COKE");
                        ArrayList<UserLocation> userLocations = args.getParcelableArrayList("userLocations");
                        if (userLocations != null && !userLocations.isEmpty()) {
                            Log.d("HOPE", "HOPE");
                            showMembersLocation(userLocations);
                        }
                        else
                        {
                            Log.d("COKE", "COKE");
                        }
                    }
                }
            });
        }

        FloatingActionMenu fabMenu = view.findViewById(R.id.fab_menu);
        FloatingActionButton fabRestaurant = view.findViewById(R.id.fab_restaurant);
        FloatingActionButton fabATM = view.findViewById(R.id.fab_atm);

        fabRestaurant.setOnClickListener(v -> {
            fetchNearbyPlaces(myMap, "restaurant");
            fabMenu.close(true);
        });

        fabATM.setOnClickListener(v -> {
            fetchNearbyPlaces(myMap, "atm");
            fabMenu.close(true);
        });

        // Close the menu when an item is clicked
        fabMenu.setOnMenuToggleListener(opened -> {
            if (!opened) fabMenu.close(true);
        });

        startLocationUpdates(); // Start location updates when the view is created
    }

    private void setupMap() {
        if (myMap != null) {
            pinpointCurrentLocation();

            // Set up click listener to show BottomSheet
            myMap.setOnMarkerClickListener(marker -> {
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
                    PlaceResult placeResult = (PlaceResult) closestMarker.getTag();
                    if (placeResult != null) {
                        LatLng destination = new LatLng(placeResult.location.latitude, placeResult.location.longitude);
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
                CoordonatesBottomSheetFragment bottomSheet = CoordonatesBottomSheetFragment.newInstance(placeResult.location.latitude, placeResult.location.longitude, placeResult, placesClient);
                bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
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

    public void fetchNearbyPlaces(final GoogleMap googleMap, String placeType) {
        // Check if the selected category is the same as the current category
        if (placeType.equals(currentCategory)) {
            // Clear current markers and reset the current category
            clearCurrentMarkers();
            currentCategory = null;
            return;
        }

        // Clear current markers before fetching new places
        clearCurrentMarkers();
        currentCategory = placeType;

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        String apiKey = getString(R.string.maps_api_key);
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
                                if (placeType.equals("restaurant"))
                                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.restaurant));
                                else if (placeType.equals("atm"))
                                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.atm));

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

    private void clearCurrentMarkers() {
        for (Marker marker : currentMarkers) {
            marker.remove();
        }
        currentMarkers.clear();
    }

    public void getPlaceDetails(String placeId, Consumer<PlaceResult> callback) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG,
                Place.Field.TYPES, Place.Field.RATING, Place.Field.PHONE_NUMBER, Place.Field.WEBSITE_URI,
                Place.Field.OPENING_HOURS, Place.Field.PRICE_LEVEL, Place.Field.PHOTO_METADATAS
        );

        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, fields);
        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();
            PlaceResult result = new PlaceResult(
                    place.getName(),
                    place.getAddress(),
                    place.getLatLng(),
                    place.getTypes(),
                    place.getRating(),
                    place.getPhoneNumber(),
                    place.getWebsiteUri() != null ? place.getWebsiteUri().toString() : null,
                    place.getOpeningHours(),
                    String.valueOf(place.getPriceLevel()),
                    place.getPhotoMetadatas()
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
                Marker marker = myMap.addMarker(new MarkerOptions().position(latLng).title(userLocation.getUsername()));
                currentMarkers.add(marker);
            }

            if (!userLocations.isEmpty()) {
                LatLng firstLocation = new LatLng(userLocations.get(0).getLatitude(), userLocations.get(0).getLongitude());
                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 10));
            }
        } else {
            Toast.makeText(requireContext(), "Google Map is not initialized", Toast.LENGTH_SHORT).show();
        }
    }
}
