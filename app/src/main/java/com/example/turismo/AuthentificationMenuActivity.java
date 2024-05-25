package com.example.turismo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class AuthentificationMenuActivity extends AppCompatActivity {

    Button loginButton;
    TextView register;
    TextView forgotPassword;

    private EditText emailOrUsernameField;
    private EditText passwordField;
    private CheckBox rememberMeCheckbox;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authentification_menu);


        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);


        emailOrUsernameField = findViewById(R.id.emailOrUsernameField);
        passwordField = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.loginButton);
        forgotPassword = findViewById(R.id.forgotPassword);
        register = findViewById(R.id.signup);
        rememberMeCheckbox = findViewById(R.id.rememberMe);


        String savedEmailOrUsername = sharedPreferences.getString("email", "");
        String savedPassword = sharedPreferences.getString("password", "");
        if (!savedEmailOrUsername.isEmpty()) {
            emailOrUsernameField.setText(savedEmailOrUsername);
            passwordField.setText(savedPassword);
            loginUser(savedEmailOrUsername, savedPassword);
        }



        loginButton.setOnClickListener(v -> loginUser(emailOrUsernameField.getText().toString().trim(), passwordField.getText().toString().trim()));

        forgotPassword.setOnClickListener(v -> startActivity(new Intent(AuthentificationMenuActivity.this, SendEmailForgotPassword.class)));

        register.setOnClickListener(v -> startActivity(new Intent(AuthentificationMenuActivity.this, RegisterActivity.class)));
    }

    private void loginUser(String emailOrUsername, String password) {
        if (emailOrUsername != null && !emailOrUsername.isEmpty() && password != null && !password.isEmpty()){
            firebaseAuth.signInWithEmailAndPassword(emailOrUsername, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            handleLoginResult(task);
                            if(rememberMeCheckbox.isChecked()){
                                getSharedPreferences("loginPrefs",MODE_PRIVATE)
                                        .edit()
                                        .putString("email",emailOrUsername)
                                        .putString("password", password)
                                        .apply();
                            }
                        }
                    });}else{
            Toast.makeText(getApplicationContext(),"Please fill both fields",Toast.LENGTH_SHORT).show();
        }
    }

    private void loginUser() {
        String emailOrUsername = emailOrUsernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        // Validate input fields
        if (TextUtils.isEmpty(emailOrUsername) || TextUtils.isEmpty(password)) {
            Toast.makeText(AuthentificationMenuActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the user is logging in with email or username
        if (Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()) {
            // Login with email
            firebaseAuth.signInWithEmailAndPassword(emailOrUsername, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            handleLoginResult(task);
                        }
                    });
        } else {

            firestore.collection("users")
                    .whereEqualTo("username", emailOrUsername)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                String email = document.getString("email");
                                assert email != null;
                                firebaseAuth.signInWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(AuthentificationMenuActivity.this, new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {

                                                handleLoginResult(task);
                                                if(rememberMeCheckbox.isChecked()){
                                                    getSharedPreferences("loginPrefs",MODE_PRIVATE)
                                                            .edit()
                                                            .putString("email",email)
                                                            .putString("password", password)
                                                            .apply();
                                                }

                                            }
                                        });
                            } else {
                                Toast.makeText(AuthentificationMenuActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void handleLoginResult(Task<AuthResult> task) {
        if (task.isSuccessful()) {
            // User login successful
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null && user.isEmailVerified()) {
                // User is verified, proceed to main activity

                Toast.makeText(AuthentificationMenuActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(AuthentificationMenuActivity.this, MapActivity.class));
                finish();
            } else {
                // User is not verified
                Toast.makeText(AuthentificationMenuActivity.this, "Please verify your email first.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // User login failed
            Toast.makeText(AuthentificationMenuActivity.this, "Login failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
        }
    }
}
