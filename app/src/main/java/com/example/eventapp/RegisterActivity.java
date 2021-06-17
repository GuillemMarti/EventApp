package com.example.eventapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    //Initialize variables
    TextInputEditText emailView, password1View, password2View, telefonoView, nombreView;
    String errorMsg = "Las contraseñas no coinciden";
    String errorMsg2 = "Se ha producido un error al intentar registrarte";
    String email, password,password2, telefono, nombre;
    FirebaseAuth fAuth;
    FirebaseFirestore db;
    Map<String, Object> map = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Assign variables
        emailView = findViewById(R.id.emailRegisterEditText);
        password1View = findViewById(R.id.passwordRegisterEditText);
        password2View = findViewById(R.id.repeatPassword);
        telefonoView = findViewById(R.id.telefono);
        nombreView = findViewById(R.id.nombre);

        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

    }


    //This method checks if the required fields are completed and registers the new user in the autenthication and db
    public void onClickRegister(View v){
        email = emailView.getText().toString().trim();
        password = password1View.getText().toString().trim();
        password2 = password2View.getText().toString().trim();
        telefono = telefonoView.getText().toString().trim();
        nombre = nombreView.getText().toString().trim();

        if (TextUtils.isEmpty(email)){
            emailView.setError("Introduce un email.");
            return;
        }

        if (TextUtils.isEmpty(password)){
            password1View.setError("Introduce una contraseña.");
            return;
        }

        if (password.length() < 6){
            password1View.setError("La contraseña debe contener al menos 6 caracteres.");
            return;
        }

        if (TextUtils.isEmpty(password2)){
            password2View.setError("Introduce una contraseña.");
            return;
        }

        if (TextUtils.isEmpty(nombre)){
            nombreView.setError("Introduce un nombre y apellidos.");
            return;
        }

        if (TextUtils.isEmpty(telefono)){
            telefonoView.setError("Introduce un número de teléfono.");
            return;
        }

        if (password.equals(password2)){

            fAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()){
                            map.put("Name", nombre);
                            map.put("Telephone", telefono);
                            db.collection("users").document(email).set(map);
                            Toast.makeText(getApplicationContext(),"Usuario registrado", Toast.LENGTH_LONG).show();
                            goLogin();
                        }else{
                            Toast.makeText(getApplicationContext(), errorMsg2, Toast.LENGTH_LONG).show();
                        }
                    });
        }else{
            Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
        }
    }


    // Intent to loginActivity
    public void goLogin(){
        Intent goLogin = new Intent(this, LoginActivity.class);
        startActivity(goLogin);
    }
}