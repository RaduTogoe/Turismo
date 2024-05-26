package com.example.turismo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountFragment extends Fragment {

    private EditText usernameEditText, emailEditText, passwordEditText, confirmPassword;
    private Button saveChangesButton, disconnectButton, deleteAccountButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private SharedPreferences sharedPreferences;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference();
        DatabaseReference imgref = dbref.child("image");
        // Find views by ID
        usernameEditText = view.findViewById(R.id.nameField);
        emailEditText = view.findViewById(R.id.emailField);
        passwordEditText = view.findViewById(R.id.passwordField);
        saveChangesButton = view.findViewById(R.id.saveButton);
        disconnectButton = view.findViewById(R.id.disconnectButton);
        deleteAccountButton = view.findViewById(R.id.deleteButton);
        confirmPassword = view.findViewById(R.id.confirmPasswordField);
        /*
            TO DO profile picture thing

        ImageView profilepic = view.findViewById(R.id.profilePhotoImageView);
        imgref.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot dataSnapshot) {
                        // getting a DataSnapshot for the
                        // location at the specified relative
                        // path and getting in the link variable
                        String link = dataSnapshot.getValue(
                                String.class);

                        // loading that data into rImage
                        // variable which is ImageView
                        Glide.with(AccountFragment.this).load(link).into(profilepic);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }

                    // this will called when any problem
                    // occurs in getting data

                });
 Fetch and populate user data in EditText fields
*/
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String email = documentSnapshot.getString("email");

                            usernameEditText.setText(username);
                            emailEditText.setText(email);

                        }
                    });
        }

        // Set click listeners for buttons
        saveChangesButton.setOnClickListener(v -> saveChanges());
        disconnectButton.setOnClickListener(v -> disconnect());
        deleteAccountButton.setOnClickListener(v -> deleteAccount());

        return view;
    }

    private void saveChanges() {
        String newUsername = usernameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();
        String newPassword = passwordEditText.getText().toString().trim();
        firestore.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            String username = documentSnapshot.getString("username");
            String email = documentSnapshot.getString("email");
            if (!newPassword.isEmpty() && newPassword.equals(confirmPassword.getText().toString())) {
                currentUser.updatePassword(newPassword)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                passwordEditText.setText("");
                                confirmPassword.setText("");
                                Toast.makeText(getContext(), "Passwords changed succesfully: ", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Password change failed.", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else if (!newPassword.isEmpty()) {
                Toast.makeText(getContext(), "Passwords don't match", Toast.LENGTH_SHORT).show();
            }
            if (!username.equals(newUsername))
                firestore.collection("users").document(currentUser.getUid())
                        .update("username", newUsername)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Username changed succesfully", Toast.LENGTH_SHORT).show();

                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error changing username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            if (!email.equals(newEmail)) {
                if (Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    firestore.collection("users").document(currentUser.getUid())
                            .update("email", newEmail)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Email changed succesfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error changing email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });


    }


    private void disconnect() {

        firebaseAuth.signOut();
        Intent intent = new Intent(getContext(), AuthentificationMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("email", "1")
                .putString("password", "0")
                .apply();
        ;

        startActivity(intent);
    }


    private void deleteAccount() {
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.getUid()).delete();
            currentUser.delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Account deleted successfully
                            // Redirect to login screen
                            Intent intent = new Intent(getContext(), AuthentificationMenuActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            startActivity(intent);
                        } else {
                            Toast.makeText(getContext(), "Error deleting account: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
