package com.example.eventapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticipantsActivity extends AppCompatActivity {

    //Initialize variables
    Intent previous;
    Button addBtn, removeBtn, addParticipantBtn, cancelAddBtn, removeParticipantBtn, cancelRemoveBtn;
    ListView listViewParticipantes;
    TextView titleEventView;
    String emailParticipant;
    AlertDialog.Builder dialogBuilder;
    AlertDialog dialog;
    EditText emailParticipantAdd, emailParticipantRemove;
    FirebaseFirestore db;
    List<HashMap<String, String>> listParticipants = new ArrayList<>();
    HashMap<String, String> participant;
    Map<String, Object> map;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participants);


        //Assign variables
        addBtn = findViewById(R.id.addParticipantBtn);
        removeBtn = findViewById(R.id.removeParticipantBtn);
        listViewParticipantes = findViewById(R.id.listParticipants);
        previous = getIntent();
        titleEventView = findViewById(R.id.title_participants);
        db = FirebaseFirestore.getInstance();

        //Get the event
        db.collection("events").document(previous.getStringExtra("Id")).get().addOnSuccessListener(documentSnapshot -> titleEventView.setText(documentSnapshot.getString("Name")));


        //Get the participants of the event
        db.collection("events").document(previous.getStringExtra("Id")).collection("participants").addSnapshotListener((documentSnapshots, error) -> {
            listParticipants.clear();

            for (DocumentSnapshot snapshot: documentSnapshots){
                participant = new HashMap<>();
                participant.put("Email", snapshot.getId());
                participant.put("Name", snapshot.getString("Name"));
                listParticipants.add(participant);
            }
            SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), listParticipants, R.layout.list_participants,
                    new String[]{"Name", "Email"},
                    new int[]{R.id.list_name, R.id.list_email});
            adapter.notifyDataSetChanged();
            listViewParticipantes.setAdapter(adapter);
        });

    }

    public void onClickAddParticipant(View v){
        createAddPopup();
    }

    public void onClickRemoveParticipant(View v){
        createRemovePopup();
    }

    //Popup for the addition of participants
    public void createAddPopup(){
        dialogBuilder = new AlertDialog.Builder(this);
        final View createEventPopup = getLayoutInflater().inflate(R.layout.popup_add_participant, null);

        //Assign popup variables
        emailParticipantAdd = createEventPopup.findViewById(R.id.emailAddParticipant);
        addParticipantBtn = createEventPopup.findViewById(R.id.addParticipantBtn);
        cancelAddBtn = createEventPopup.findViewById(R.id.cancelAddParticipantBtn);

        //Create popup
        dialogBuilder.setView(createEventPopup);
        dialog = dialogBuilder.create();
        dialog.show();

        //Add participant to event
        addParticipantBtn.setOnClickListener(v -> {
            emailParticipant = emailParticipantAdd.getText().toString().trim();

            //Check if there is email
            if (TextUtils.isEmpty(emailParticipant)){
                emailParticipantAdd.setError("Introduce un email");
                return;
            }

            //Check if the email exists in the database and add it if it exists
            db.collection("users").addSnapshotListener((documentSnapshots, error) -> {
                for(DocumentSnapshot snapshot: documentSnapshots){
                    if (snapshot.getId().equals(emailParticipant)){
                        map = new HashMap<>();
                        map.put("Name", snapshot.getString("Name"));
                        db.collection("events").document(previous.getStringExtra("Id")).collection("participants").document(emailParticipant).set(map);
                        db.collection("events").document(previous.getStringExtra("Id")).get().addOnSuccessListener(v2->{
                            map = new HashMap<>();
                            map.put("Name",v2.getString("Name"));
                            db.collection("users").document(emailParticipant).collection("assigned_events").document(previous.getStringExtra("Id")).set(map);
                        });
                        dialog.dismiss();
                    }
                }
            });
        });

        //Cancel operation
        cancelAddBtn.setOnClickListener(v -> dialog.dismiss());
    }

    //Popup for the removal of participants
    public void createRemovePopup(){
        dialogBuilder = new AlertDialog.Builder(this);
        final View createEventPopup = getLayoutInflater().inflate(R.layout.popup_remove_participant, null);

        //Assign popup variables
        emailParticipantRemove = createEventPopup.findViewById(R.id.emailRemoveParticipant);
        removeParticipantBtn = createEventPopup.findViewById(R.id.removeParticipantBtn);
        cancelRemoveBtn = createEventPopup.findViewById(R.id.cancelRemoveParticipantBtn);


        //Create popup
        dialogBuilder.setView(createEventPopup);
        dialog = dialogBuilder.create();
        dialog.show();

        //Remove participant from event
        removeParticipantBtn.setOnClickListener(v -> {
            emailParticipant = emailParticipantRemove.getText().toString().trim();

            //Check if there is email
            if (TextUtils.isEmpty(emailParticipant)){
                emailParticipantRemove.setError("Introduce una tarea");
                return;
            }

            //Check if the participant is added in the event and then delete it
            db.collection("events").document(previous.getStringExtra("Id")).collection("participants").addSnapshotListener((documentSnapshots, error) -> {
                for (DocumentSnapshot snapshot : documentSnapshots){
                    if (snapshot.getId().equals(emailParticipant)){
                        db.collection("events").document(previous.getStringExtra("Id")).collection("participants").document(emailParticipant).delete();
                        db.collection("users").document(emailParticipant).collection("assigned_events").document(previous.getStringExtra("Id")).delete();
                        dialog.dismiss();
                    }
                }
            });

        });

        //Cancel operation
        cancelRemoveBtn.setOnClickListener(v -> dialog.dismiss());
    }
}