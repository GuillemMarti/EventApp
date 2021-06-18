package com.example.eventapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class InfoEventActivity extends AppCompatActivity {

    //Initialize variables
    Intent previous;
    Button createTareaBtn, notificationBtn;
    ListView listView;
    TextView titleEvent, dateEvent, titleTarea, statusTarea, personTarea;
    FirebaseFirestore db;
    List<HashMap<String, String>> listTareas = new ArrayList<>();
    AlertDialog.Builder dialogBuilder;
    AlertDialog dialog;
    EditText nameTarea;
    Button createTareaBtn2, cancelTareaBtn, addParticipantBtn, assignParticipantBtn, confirmParticipantBtn, cancelAssignBtn;
    String name, id, nameGroup, idTask, organizator;
    Map<String, Object> map = new HashMap<>();


    String urlString;
    String urlGetUpdates = "https://api.telegram.org/botAPItoken/getUpdates?offset=-1";
    String apiToken = "APItoken";
    String chatId;
    JSONObject chatjson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_event);

        //Assign variables
        previous = getIntent();
        createTareaBtn = findViewById(R.id.create_tarea);
        listView = findViewById(R.id.listTareas);
        titleEvent = findViewById(R.id.title_event);
        dateEvent = findViewById(R.id.date_Event);
        db = FirebaseFirestore.getInstance();
        notificationBtn = findViewById(R.id.notificationBtn);
        addParticipantBtn = findViewById(R.id.addParticipantBtn);



        //Get the event
        db.collection("events").document(previous.getStringExtra("Id")).get().addOnSuccessListener(documentSnapshot -> {
                titleEvent.setText(documentSnapshot.getString("Name"));
                dateEvent.setText(documentSnapshot.getString("Date"));
                nameGroup = documentSnapshot.getString("Name group");
                organizator = documentSnapshot.getString("Organizator");
                //If the user is not the organizator, make the buttons invisible
                if(previous.getStringExtra("User").equals(organizator)){
                    createTareaBtn.setVisibility(View.VISIBLE);
                    notificationBtn.setVisibility(View.VISIBLE);
                    addParticipantBtn.setVisibility(View.VISIBLE);
                }else{
                    createTareaBtn.setVisibility(View.INVISIBLE);
                    notificationBtn.setVisibility(View.INVISIBLE);
                    addParticipantBtn.setVisibility(View.INVISIBLE);
                }
        });

        //Get tasks of event
        db.collection("events").document(previous.getStringExtra("Id")).collection("tareas").addSnapshotListener((documentSnapshots, error) -> {
            listTareas.clear();
            
            for (DocumentSnapshot snapshot: documentSnapshots){
                HashMap<String, String> tarea = new HashMap<>();
                tarea.put("Id", snapshot.getId());
                tarea.put("Name", snapshot.getString("Name"));
                tarea.put("Status", snapshot.getString("Status"));
                tarea.put("Person", snapshot.getString("Person"));
                listTareas.add(tarea);
            }
            SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), listTareas, R.layout.list_tareas,
                    new String[]{"Name", "Status", "Person"},
                    new int[]{R.id.list_tarea, R.id.list_status, R.id.list_person});
            adapter.notifyDataSetChanged();
            listView.setAdapter(adapter);
        });

        //Make list clickable
        listView.setClickable(true);

        //Create popup for assigning a task
        listView.setOnItemClickListener((parent, view, position, id) -> {
            idTask = listTareas.get(position).get("Id");
            createPopupAssignTask();
        });

        //Send notification to telegram chat
        notificationBtn.setOnClickListener(v -> {
            try {
                getChatId();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

    }

    //First we get the updates of the bot
    private void getChatId() throws InterruptedException {
        Thread thread = new Thread(() -> {
            HttpURLConnection urlConn = null;
            BufferedReader bufferedReader;
            try {
                URL url = new URL(urlGetUpdates);
                urlConn = (HttpURLConnection) url.openConnection();
                bufferedReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

                StringBuilder stringBuffer = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuffer.append(line);
                }

                //We get the id of the chatgroup
                chatjson =  new JSONObject(stringBuffer.toString());
               for (int i=0; i < chatjson.getJSONArray("result").length(); i++){
                    JSONObject jsonObject= chatjson.getJSONArray("result").getJSONObject(i);
                   Log.d("JsonObject", jsonObject.toString());
                    if (jsonObject.getJSONObject("message").getJSONObject("chat").getString("type").equals("group")) {
                        if (jsonObject.getJSONObject("message").getJSONObject("chat").getString("title").equals(nameGroup)) {
                            chatId = jsonObject.getJSONObject("message").getJSONObject("chat").getString("id");
                        }
                    }
                }
            }catch (IOException | JSONException e){
                e.printStackTrace();
            }

            //We get the tasks that aren't assigned yet
            db.collection("events").document(previous.getStringExtra("Id")).collection("tareas").get().addOnSuccessListener(documentSnapshots -> {
                String text = "Nuevas tareas sin asignar: ";
                for (DocumentSnapshot snapshot:documentSnapshots){
                    if (Objects.equals(snapshot.getString("Status"), "Sin asignar")){
                        text = text.concat(", "+snapshot.getString("Name"));
                    }
                }

                text = text.replace(": ,", ": ");
                try {
                    //Send a message to the telegram group to notify the assistants
                    sendNotifications(text);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            urlConn.disconnect();
        });
        thread.start();
        thread.join();
    }

    private void sendNotifications(String text) throws InterruptedException {
        Thread thread = new Thread(() -> {
            urlString = "https://api.telegram.org/bot"+apiToken+"/sendMessage?chat_id="+chatId+"&text="+ text;
            HttpURLConnection conn = null;

            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            try{
                conn = (HttpURLConnection) url.openConnection();

                StringBuilder sb = new StringBuilder();
                InputStream is = new BufferedInputStream(conn.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                //The bot needs a recent message in the group in order to know where to send the notficiation without making the group public
            }catch (FileNotFoundException e){
                Looper.prepare();
                Toast.makeText(this,"Envia un mensaje por el chat para que el bot pueda enviar notificación", Toast.LENGTH_LONG).show();
            }catch (IOException e){
                e.printStackTrace();
            }
            conn.disconnect();
        });
        thread.start();
        thread.join();
    }

    //Add tasks to the event
    public void onClickCreateTarea(View v){
        createPopup();
    }

    //Add participants to the event
    public void goParticipants(View v){
        Intent goParticipants = new Intent(this, ParticipantsActivity.class);
        goParticipants.putExtra("Id", previous.getStringExtra("Id"));
        goParticipants.putExtra("User", previous.getStringExtra("User"));
        startActivity(goParticipants);
    }


    //Popup for the creation of the task
    private void createPopup() {
        dialogBuilder = new AlertDialog.Builder(this);
        final View createEventPopup = getLayoutInflater().inflate(R.layout.popup_tarea, null);

        //Assign popup variables
        nameTarea = createEventPopup.findViewById(R.id.name_Tarea);
        createTareaBtn2 = createEventPopup.findViewById(R.id.createTareaButton);
        cancelTareaBtn = createEventPopup.findViewById(R.id.cancelTareaButton);

        //Create popup
        dialogBuilder.setView(createEventPopup);
        dialog = dialogBuilder.create();
        dialog.show();

        //Create and save tarea
        createTareaBtn2.setOnClickListener(v -> {
            name = nameTarea.getText().toString().trim();

            //Check if there is name
            if (TextUtils.isEmpty(name)){
                nameTarea.setError("Introduce una tarea");
                return;
            }

            //Save data in database
            id = UUID.randomUUID().toString();
            map.put("Name", name);
            map.put("Status", "Sin asignar");
            map.put("Person", "");
            db.collection("events").document(previous.getStringExtra("Id")).collection("tareas").document(id).set(map);
            dialog.dismiss();
        });

        //Cancel operation
        cancelTareaBtn.setOnClickListener(v -> dialog.dismiss());
    }

    //This popup serves the purpose of changing the status of the tasks
    public void createPopupAssignTask(){
        dialogBuilder = new AlertDialog.Builder(this);
        final View createEventPopup = getLayoutInflater().inflate(R.layout.assign_task_popup, null);

        //Assign popup variables
        titleTarea = createEventPopup.findViewById(R.id.name_task);
        statusTarea = createEventPopup.findViewById(R.id.assigned_task);
        personTarea = createEventPopup.findViewById(R.id.person_task);
        assignParticipantBtn = createEventPopup.findViewById(R.id.assignPersonBtn);
        confirmParticipantBtn = createEventPopup.findViewById(R.id.confirmTaskBtn);
        cancelAssignBtn = createEventPopup.findViewById(R.id.cancelAssignBtn);

        //Get data from database for the task and set the corresponding button to visible according to the status
        db.collection("events").document(previous.getStringExtra("Id")).collection("tareas").document(idTask).get().addOnSuccessListener(documentSnapshot -> {
            titleTarea.setText(documentSnapshot.getString("Name"));
            statusTarea.setText(documentSnapshot.getString("Status"));
            personTarea.setText(documentSnapshot.getString("Person"));

            if (Objects.equals(documentSnapshot.getString("Status"), "Sin asignar")) {
                confirmParticipantBtn.setVisibility(View.INVISIBLE);
                assignParticipantBtn.setVisibility(View.VISIBLE);
            }else if(Objects.equals(documentSnapshot.getString("Status"), "Asignada") && Objects.equals(documentSnapshot.getString("Person"), previous.getStringExtra("User"))){
                confirmParticipantBtn.setVisibility(View.VISIBLE);
                assignParticipantBtn.setVisibility(View.INVISIBLE);
            }else{
                confirmParticipantBtn.setVisibility(View.INVISIBLE);
                assignParticipantBtn.setVisibility(View.INVISIBLE);
            }
        });

        //Create popup
        dialogBuilder.setView(createEventPopup);
        dialog = dialogBuilder.create();
        dialog.show();

        //Assign person
        assignParticipantBtn.setOnClickListener(v -> {
            db.collection("events").document(previous.getStringExtra("Id")).collection("tareas").document(idTask).update("Status","Asignada");
            db.collection("events").document(previous.getStringExtra("Id")).collection("tareas").document(idTask).update("Person", previous.getStringExtra("User"));
            dialog.dismiss();
        });

        //Confirm task
        confirmParticipantBtn.setOnClickListener(v -> {
            db.collection("events").document(previous.getStringExtra("Id")).collection("tareas").document(idTask).update("Status","Completada");
            dialog.dismiss();
        });

        //Cancel operation
        cancelAssignBtn.setOnClickListener(v -> dialog.dismiss());
    }

}
