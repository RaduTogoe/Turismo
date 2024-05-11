package com.example.turismo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.libraries.places.api.model.OpeningHours;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class LocationBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_LAT = "latitude";
    private static final String ARG_LNG = "longitude";
    private PlacesClient placesClient;
    private PlaceResult place;

    public static LocationBottomSheetFragment newInstance(double lat, double lng, PlaceResult place, PlacesClient client) {
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, lat);
        args.putDouble(ARG_LNG, lng);
       // Log.d(place.name, place.name);
        LocationBottomSheetFragment fragment = new LocationBottomSheetFragment(place,client);
        fragment.setArguments(args);
        return fragment;
    }

    public LocationBottomSheetFragment(PlaceResult place, PlacesClient client)
    {
        this.place = place;
        this.placesClient = client;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bottom_sheet_dialog, container, false);
        TextView latitudeText = v.findViewById(R.id.latitudeText);
        TextView longitudeText = v.findViewById(R.id.longitudeText);
        Bundle args = getArguments();
        if (args != null) {
            double lat = args.getDouble(ARG_LAT);
            double lng = args.getDouble(ARG_LNG);
            latitudeText.setText("Latitude: " + lat);
            longitudeText.setText("Longitude: " + lng);
        }
        updateUI(v);

        return v;
    }

    private void setupImageSlider(ViewPager2 viewPager, List<PhotoMetadata> photoMetadataList) {
        PhotoAdapter adapter = new PhotoAdapter(photoMetadataList, placesClient);
        viewPager.setAdapter(adapter);
    }

    public static String formatOpeningHours(OpeningHours openingHours) {
        if (openingHours == null) {
            return "Opening hours not available";
        }
        StringBuilder sb = new StringBuilder();
        for (String day : openingHours.getWeekdayText()) {
            sb.append(day).append("\n");
        }
        return sb.toString().trim(); // Trim to remove the last newline character
    }
    private void updateUI(View view) {
        ((TextView) view.findViewById(R.id.nameText)).setText(place.name);
        ((TextView) view.findViewById(R.id.addressText)).setText(place.address);
        ((TextView) view.findViewById(R.id.phoneText)).setText(place.phoneNumber);
        ((TextView) view.findViewById(R.id.websiteText)).setText(place.websiteUri != null ? place.websiteUri.toString() : "N/A");
        ((TextView) view.findViewById(R.id.openingHoursText)).setText(formatOpeningHours(place.openingHours));
        ((TextView) view.findViewById(R.id.latitudeText)).setText(String.valueOf(place.location.latitude));
        ((TextView) view.findViewById(R.id.longitudeText)).setText(String.valueOf(place.location.longitude));
        if (place.photoMetadatas != null) {
            ViewPager2 imageSlider = view.findViewById(R.id.imageSlider);
            setupImageSlider(imageSlider, place.photoMetadatas);
        }
    }
}
