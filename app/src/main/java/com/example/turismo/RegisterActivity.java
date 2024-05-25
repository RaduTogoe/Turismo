package com.example.turismo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private EditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Find views by ID
        usernameEditText = findViewById(R.id.registerNameField);
        emailEditText = findViewById(R.id.registerEmailField);
        passwordEditText = findViewById(R.id.registerPasswordField);
        confirmPasswordEditText = findViewById(R.id.registerConfirmPasswordField);
        registerButton = findViewById(R.id.registerButton);

        // Set click listener for the register button
        registerButton.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                getCurrentLocation();
            } else {
                requestLocationPermission();
            }
        });
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocation = location;
                    registerUser();
                } else {
                    Toast.makeText(RegisterActivity.this, "Unable to get current location. Try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            requestLocationPermission();
        }
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validate input fields
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null && currentLocation != null) {
                            String userId = user.getUid();

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", username);
                            userData.put("email", email);
                            userData.put("location", String.valueOf(currentLocation.getLatitude()) + "," + String.valueOf(currentLocation.getLongitude()));

                            firestore.collection("users").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(RegisterActivity.this, "Error registering user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });

                            user.sendEmailVerification()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                startActivity(new Intent(RegisterActivity.this, AuthentificationMenuActivity.class));
                                                finish();
                                                Toast.makeText(RegisterActivity.this, "Registration successful. Please check your email for verification.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(RegisterActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error registering user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission is required to register", Toast.LENGTH_SHORT).show();
            }
        }
    }
}