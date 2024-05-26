package com.example.turismo;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity implements GroupSettingsDialogFragment.GroupSettingsListener {

    private RecyclerView messagesRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private String groupName;
    private String groupId;
    private List<String> members;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Map<String, String> userIdToNameMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        groupName = getIntent().getStringExtra("GROUP_NAME");
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        members = new ArrayList<>(); // Initialize members list
        userIdToNameMap = new HashMap<>(); // Initialize user ID to name map

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(groupName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setOnClickListener(v -> {
            List<String> memberNames = new ArrayList<>();
            for (String memberId : members) {
                memberNames.add(userIdToNameMap.get(memberId));
            }
            DialogFragment dialog = GroupSettingsDialogFragment.newInstance(groupName, members, groupId, memberNames);
            dialog.show(getSupportFragmentManager(), "GroupSettingsDialogFragment");
        });

        // Initialize views
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        // Initialize message list and adapter
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);

        // Set up RecyclerView
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);

        // Handle send button click
        sendButton.setOnClickListener(v -> {
            String messageText = messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
                messageEditText.setText("");
            } else {
                Toast.makeText(ChatActivity.this, "Cannot send an empty message", Toast.LENGTH_SHORT).show();
            }
        });
        messageEditText.setOnKeyListener(new View.OnKeyListener(){
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)){
                    sendMessage(messageEditText.getText().toString().trim());
                return true;}
                return false;
            }
        });
        loadGroupDetails();
    }

    private void loadGroupDetails() {
        db.collection("groups")
                .whereEqualTo("groupName", groupName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String documentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        groupId = documentId; // Ensure groupId is set
                        db.collection("groups").document(documentId).get().addOnSuccessListener(documentSnapshot -> {
                            Group group = documentSnapshot.toObject(Group.class);
                            if (group != null) {
                                members = group.getMembers();
                                if (members == null) {
                                    members = new ArrayList<>();
                                }
                                fetchMemberNames();
                            }
                        });

                        db.collection("groups").document(documentId).collection("messages")
                                .orderBy("timestamp", Query.Direction.ASCENDING)
                                .addSnapshotListener((queryDocumentSnapshots1, e) -> {
                                    if (e != null) {
                                        return;
                                    }
                                    messageList.clear();
                                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots1) {
                                        Message message = doc.toObject(Message.class);
                                        fetchSenderName(message);
                                    }
                                });
                    }
                });
    }

    private void fetchMemberNames() {
        for (String memberId : members) {
            db.collection("users").document(memberId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("username");
                    userIdToNameMap.put(memberId, name != null ? name : memberId);
                } else {
                    userIdToNameMap.put(memberId, memberId);
                }
            });
        }
    }

    private void fetchSenderName(Message message) {
        db.collection("users").document(message.getSenderId()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("username");
                message.setSenderName(name != null ? name : message.getSenderId());
            } else {
                message.setSenderName(message.getSenderId());
            }
            messageList.add(message);
            messageAdapter.notifyDataSetChanged();
            messagesRecyclerView.scrollToPosition(messageList.size() - 1);  // Scroll to the most recent message
        });
    }

    private void sendMessage(String text) {
        String senderEmail = currentUser.getEmail();
        String senderName = userIdToNameMap.get(currentUser.getUid());
        Message message = new Message(text, currentUser.getUid(), senderEmail, senderName, System.currentTimeMillis());
        db.collection("groups").document(groupId).collection("messages").add(message);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onGroupNameChanged(String newName) {
        db.collection("groups").document(groupId).update("groupName", newName)
                .addOnSuccessListener(aVoid -> {
                    groupName = newName;
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(groupName);
                    }
                    Toast.makeText(this, "Group name changed to: " + newName, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onLeaveGroup() {
        db.collection("groups").document(groupId).update("members", FieldValue.arrayRemove(currentUser.getUid()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Left the group", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    @Override
    public void onShowMembersLocation(List<String> memberIds) {
        Log.d("ChatActivity", "onShowMembersLocation called with member IDs: " + memberIds);
        db.collection("users").whereIn("id", memberIds).get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<UserLocation> userLocations = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                String username = document.getString("username");
                GeoPoint location = document.getGeoPoint("location");
                if (location != null) {
                    userLocations.add(new UserLocation(username, location.getLatitude(), location.getLongitude()));
                }
            }
            showMembersOnMap(userLocations);
        });
    }

    private void showMembersOnMap(List<UserLocation> userLocations) {
        Log.d("ChatActivity", "showMembersOnMap called with user locations: " + userLocations);
        Fragment mapFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentMap);
        if (mapFragment instanceof MapFragment) {
            ((MapFragment) mapFragment).showMembersLocation(userLocations);
        } else {
            Log.e("ChatActivity", "MapFragment not found or not an instance of MapFragment");
        }
    }
}
