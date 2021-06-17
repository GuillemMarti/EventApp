package com.example.eventapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;

public class EventMainActivity extends AppCompatActivity {

    //Initialize variables
    ListView listView;
    Button addEventBtn, createEventBtn, cancelBtn;
    FirebaseFirestore db;
    AlertDialog.Builder dialogBuilder;
    AlertDialog dialog;
    EditText nameEvent, dateEvent, nameTelegramGroup;
    String name, date, nameGroup;
    Map<String, Object> map = new HashMap<>();
    String id;
    CollectionReference eventsRef;
    Intent previous;
    List<HashMap<String, String>> listEvents = new ArrayList<>();
    SimpleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_main);

        //Assign variables
        listView = findViewById(R.id.listEvents);
        addEventBtn = findViewById(R.id.eventButton);
        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");
        previous = getIntent();



        //Get all the events from the database
        db.collection("users").document(previous.getStringExtra("User")).collection("assigned_events").addSnapshotListener((documentSnapshots, error)  ->{
            listEvents.clear();

            for(DocumentSnapshot snapshot:documentSnapshots){
                db.collection("events").document(snapshot.getId()).get().addOnSuccessListener(documentSnapshot -> {
                    HashMap<String, String> events = new HashMap<>();
                    events.put("Id", documentSnapshot.getId());
                    events.put("Name", documentSnapshot.getString("Name"));
                    events.put("Date", documentSnapshot.getString("Date"));
                    listEvents.add(events);
                    adapter = new SimpleAdapter(getApplicationContext(), listEvents, R.layout.list_item,
                            new String[]{"Name", "Date"},
                            new int[]{R.id.list_event, R.id.list_date});
                    adapter.notifyDataSetChanged();
                    listView.setAdapter(adapter);
                });
            }

        });

        //Make the list clickable
        listView.setClickable(true);

        //Listener to go to the specific event
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent goEvent = new Intent(this, InfoEventActivity.class);
            goEvent.putExtra("Id", listEvents.get(position).get("Id"));
            goEvent.putExtra("User", previous.getStringExtra("User"));
            startActivity(goEvent);
        });


    }

    public void onClickCreateEvent(View v){
        createPopup();
        Toast.makeText(getApplicationContext(), "Debes crear primero el grupo de telegram y añadir los participantes y el bot EventAppSuperbot como admin", Toast.LENGTH_LONG).show();
    }

    //Method that creates the popup
    public void createPopup() {
        dialogBuilder = new AlertDialog.Builder(this);
        final View createEventPopup = getLayoutInflater().inflate(R.layout.popup, null);

        //Assign popup variables
        nameEvent = createEventPopup.findViewById(R.id.name_Event);
        dateEvent = createEventPopup.findViewById(R.id.date_Event);
        createEventBtn = createEventPopup.findViewById(R.id.createEventButton);
        cancelBtn = createEventPopup.findViewById(R.id.cancel);
        nameTelegramGroup = createEventPopup.findViewById(R.id.name_group);

        Calendar calendar = Calendar.getInstance();
        final int year = calendar.get(Calendar.YEAR);
        final int month = calendar.get(Calendar.MONTH);
        final int day = calendar.get(Calendar.DAY_OF_MONTH);

        //Create popup
        dialogBuilder.setView(createEventPopup);
        dialog = dialogBuilder.create();
        dialog.show();

        dateEvent.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(EventMainActivity.this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, (view, year1, month1, dayOfMonth) -> {
                month1 = month1 + 1;
                String date = day + "/" + month1 + "/" + year1;
                dateEvent.setText(date);
            }, year, month, day);
            datePickerDialog.show();
        });



        //Create and save event
        createEventBtn.setOnClickListener(v -> {
            name = nameEvent.getText().toString().trim();
            date = dateEvent.getText().toString().trim();
            nameGroup = nameTelegramGroup.getText().toString().trim();

            //Check if there is name and date
            if (TextUtils.isEmpty(name)){
                nameEvent.setError("Introduce un evento");
                return;
            }

            if (TextUtils.isEmpty(date)){
                dateEvent.setError("Introduce una fecha");
                return;
            }

            if (TextUtils.isEmpty(nameGroup)){
                nameTelegramGroup.setError("Introduce un grupo");
                return;
            }

            //Save data to database
            id = UUID.randomUUID().toString();
            map.put("Name", name);
            map.put("Date", date);
            map.put("Organizator", previous.getStringExtra("User"));
            map.put("Name group", nameGroup);
            db.collection("events").document(id).set(map);


            db.collection("users").document(previous.getStringExtra("User")).get().addOnSuccessListener(documentSnapshot -> {
                map = new HashMap<>();
                map.put("Name", documentSnapshot.getString("Name"));
                db.collection("events").document(id).collection("participants").document(previous.getStringExtra("User")).set(map);
            });


            map = new HashMap<>();
            map.put("Name", name);
            db.collection("users").document(previous.getStringExtra("User")).collection("assigned_events").document(id).set(map);

            dialog.dismiss();
        });

        //Cancel operation
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
    }
}