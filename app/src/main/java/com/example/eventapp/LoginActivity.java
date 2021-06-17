package com.example.eventapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    //Initalize variables
    private TextInputEditText emailView, passwordView;
    FirebaseFirestore db;
    ImageView logoView;
    String email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Assign variables
        emailView = findViewById(R.id.emailEditText);
        passwordView = findViewById(R.id.passwordEditText);
        db = FirebaseFirestore.getInstance();
        logoView = findViewById(R.id.logo_imageView);

    }

    // Intent to go to RegisterActivity
    public void onClickRegistrar(View v){
        Intent registerIntent = new Intent(this, RegisterActivity.class);
        startActivity(registerIntent);
    }

    //When the button is pressed, checks the type of the user and logs in a different activity according to it
    public void onClickLogin(View v){
        email = emailView.getText().toString().trim();
        password = passwordView.getText().toString().trim();


        if (TextUtils.isEmpty(email)){
            emailView.setError("Introduce un email.");
            return;
        }

        if (TextUtils.isEmpty(password)){
            passwordView.setError("Introduce una contraseña.");
            return;
        }

        if ((email.length()>0) && (password.length()>0)){
            FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()){
                            db.collection("users").document(email).get()
                                    .addOnSuccessListener(documentSnapshot -> onClickLogin());
                        }else{
                            Toast.makeText(getApplicationContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                        }
                    });
        }else{
            Toast.makeText(getApplicationContext(), "Introduce las credenciales", Toast.LENGTH_SHORT).show();
        }
    }

    //Intent to go to the Main Activity
    private void onClickLogin() {
        Intent loginIntent = new Intent(this, EventMainActivity.class);
        loginIntent.putExtra("User", email);
        startActivity(loginIntent);
    }
}