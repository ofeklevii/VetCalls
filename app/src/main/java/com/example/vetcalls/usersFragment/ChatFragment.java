package com.example.vetcalls.usersFragment;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vetcalls.R;
import com.example.vetcalls.usersFragment.ChatMessageFragment;
import com.example.vetcalls.obj.ChatPreview;
import com.example.vetcalls.obj.ChatPreviewAdapter;
import com.example.vetcalls.obj.DogProfile;
import com.example.vetcalls.obj.Veterinarian;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.*;
import java.util.Map;

/**
 * Fragment for managing chat functionality in the VetCalls application.
 * Handles chat list display, new chat creation, and user type detection.
 * Provides comprehensive chat management for both veterinarians and dog owners.
 *
 * @author Ofek Levi
 */
public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private RecyclerView recyclerView;
    private FloatingActionButton startChatFab;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ChatPreviewAdapter adapter;
    private List<ChatPreview> chatList = new ArrayList<>();
    private TextView emptyChatsText;

    private boolean isVet = false;

    /**
     * Default constructor for ChatFragment.
     */
    public ChatFragment() {}

    /**
     * Called when the fragment is first created.
     * Initializes Firebase instances and determines user type.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        String currentUserId = auth.getCurrentUser().getUid();
        db.collection("Veterinarians").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    isVet = documentSnapshot.exists();
                    Log.d(TAG, "User type checked - isVet: " + isVet);
                    if (recyclerView != null && adapter != null) {
                        updateAdapterUserType();
                        loadChatList();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user type", e);
                    isVet = false;
                    if (recyclerView != null && adapter != null) {
                        updateAdapterUserType();
                        loadChatList();
                    }
                });
    }

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views
     * @param container The parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     * @return The View for the fragment's UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewChats);
        startChatFab = view.findViewById(R.id.startChatFab);
        emptyChatsText = view.findViewById(R.id.emptyChatsText);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ChatPreviewAdapter(chatList, chat -> openChatFragment(chat));
        recyclerView.setAdapter(adapter);

        startChatFab.setOnClickListener(v -> openNewChatDialog());

        if (isVet || isVet == false) {
            loadChatList();
        }

        return view;
    }

    /**
     * Called when the fragment becomes visible to the user.
     * Refreshes the chat list each time the fragment resumes.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadChatList();
    }

    /**
     * Updates the adapter with the current user type.
     */
    private void updateAdapterUserType() {
        adapter = new ChatPreviewAdapter(chatList, chat -> openChatFragment(chat));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Opens the chat fragment for a specific chat.
     *
     * @param chat The ChatPreview object containing chat information
     */
    private void openChatFragment(ChatPreview chat) {
        ChatMessageFragment chatFragment = ChatMessageFragment.newInstance(
                chat.chatId,
                chat.displayName,
                chat.imageUrl,
                isVet
        );

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Loads the chat list for the current user from Firestore.
     * Displays different information based on whether the user is a veterinarian or dog owner.
     */
    private void loadChatList() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;
        Log.d(TAG, "Loading chats for user: " + currentUserId);
        db.collection("Chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    chatList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String chatId = doc.getId();
                        String imageUrl, displayName;
                        String lastMessage = doc.getString("lastMessage");
                        Date lastMessageTime = doc.getTimestamp("lastMessageTime") != null ?
                                doc.getTimestamp("lastMessageTime").toDate() : new Date();
                        if (isVet) {
                            displayName = doc.getString("dogName");
                            imageUrl = doc.getString("dogImageUrl");
                            String dogId = doc.getString("dogId");
                            if ((displayName == null || displayName.isEmpty() || imageUrl == null || imageUrl.isEmpty()) && dogId != null && !dogId.isEmpty()) {
                                db.collection("DogProfiles").document(dogId).get()
                                        .addOnSuccessListener(dogDoc -> {
                                            String name = dogDoc.getString("name");
                                            String img = dogDoc.getString("profileImageUrl");
                                            String finalName = (name != null && !name.isEmpty()) ? name : "כלב";
                                            String finalImg = (img != null && !img.isEmpty()) ? img : "https://example.com/default_dog_image.png";
                                            updateOrAddChatPreview(chatId, finalName, finalImg, lastMessage, lastMessageTime);
                                        });
                                continue;
                            }
                            if (displayName == null || displayName.isEmpty()) displayName = "כלב";
                            if (imageUrl == null || imageUrl.isEmpty()) imageUrl = "https://example.com/default_dog_image.png";
                        } else {
                            String vetId = doc.getString("vetId");
                            if (vetId != null && !vetId.isEmpty()) {
                                db.collection("Veterinarians").document(vetId).get()
                                        .addOnSuccessListener(vetDoc -> {
                                            String name = vetDoc.getString("fullName");
                                            String img = vetDoc.getString("profileImageUrl");
                                            String finalName = (name != null && !name.isEmpty()) ? name : "וטרינר";
                                            String finalImg = (img != null && !img.isEmpty()) ? img : "https://example.com/default_vet_image.png";
                                            updateOrAddChatPreview(chatId, finalName, finalImg, lastMessage, lastMessageTime);
                                        });
                                continue;
                            } else {
                                displayName = "וטרינר";
                                imageUrl = "https://example.com/default_vet_image.png";
                                updateOrAddChatPreview(chatId, displayName, imageUrl, lastMessage, lastMessageTime);
                            }
                        }
                        updateOrAddChatPreview(chatId, displayName, imageUrl, lastMessage, lastMessageTime);
                    }
                })
                .addOnFailureListener(e -> {
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                    }
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, "אין לך עדיין צ'אטים, צרי שיחה חדשה!", Toast.LENGTH_LONG).show();
                    }
                    if (emptyChatsText != null && recyclerView != null) {
                        recyclerView.setVisibility(View.GONE);
                        emptyChatsText.setVisibility(View.VISIBLE);
                    }
                });
    }

    /**
     * Updates an existing chat preview or adds a new one to the list.
     *
     * @param chatId The unique identifier for the chat
     * @param displayName The name to display for the chat
     * @param imageUrl The profile image URL for the chat
     * @param lastMessage The last message content
     * @param lastMessageTime The timestamp of the last message
     */
    private void updateOrAddChatPreview(String chatId, String displayName, String imageUrl, String lastMessage, Date lastMessageTime) {
        for (int i = 0; i < chatList.size(); i++) {
            if (chatList.get(i).chatId.equals(chatId)) {
                chatList.set(i, new ChatPreview(chatId, displayName, imageUrl, lastMessage, lastMessageTime));
                adapter.notifyDataSetChanged();
                updateEmptyView();
                return;
            }
        }
        chatList.add(new ChatPreview(chatId, displayName, imageUrl, lastMessage, lastMessageTime));
        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    /**
     * Updates the visibility of the empty view based on chat list status.
     */
    private void updateEmptyView() {
        if (emptyChatsText != null && recyclerView != null) {
            if (chatList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyChatsText.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyChatsText.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Opens a dialog for creating a new chat.
     * Shows different options based on user type (veterinarian or dog owner).
     */
    private void openNewChatDialog() {
        String currentUserId = auth.getCurrentUser().getUid();
        if (isVet) {
            db.collection("DogProfiles").whereEqualTo("vetId", currentUserId).get()
                    .addOnSuccessListener(query -> {
                        List<String> dogNames = new ArrayList<>();
                        Map<String, String> dogIdMap = new HashMap<>();

                        for (DocumentSnapshot doc : query.getDocuments()) {
                            String name = doc.getString("name");
                            String dogId = doc.getId();
                            String imageUrl = doc.getString("profileImageUrl");
                            Object ageObj = doc.get("age");
                            String age = "";
                            if (ageObj != null) {
                                age = ageObj.toString();
                            }

                            if (name != null && !name.isEmpty()) {
                                dogNames.add(name);
                                dogIdMap.put(name, dogId);
                            }
                        }

                        Log.d(TAG, "Loaded dogs for vet: " + dogNames);
                        if (!dogNames.isEmpty()) {
                            showSelectionDialog(dogNames, dogIdMap);
                        } else {
                            Context context = getContext();
                            if (context != null) {
                                Toast.makeText(context, "לא נמצאו כלבים משויכים", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "שגיאה בטעינת כלבים", e);
                        Context context = getContext();
                        if (context != null) {
                            Toast.makeText(context, "שגיאה בטעינת כלבים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            db.collection("Veterinarians").get()
                    .addOnSuccessListener(query -> {
                        List<String> vetNames = new ArrayList<>();
                        Map<String, String> vetIdMap = new HashMap<>();

                        for (DocumentSnapshot doc : query.getDocuments()) {
                            Veterinarian vet = doc.toObject(Veterinarian.class);
                            String name = vet != null ? vet.fullName : null;
                            if (name != null) {
                                vetNames.add(name);
                                vetIdMap.put(name, doc.getId());
                            }
                        }

                        if (!vetNames.isEmpty()) {
                            showSelectionDialog(vetNames, vetIdMap);
                        } else {
                            Context context = getContext();
                            if (context != null) {
                                Toast.makeText(context, "לא נמצאו וטרינרים", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "שגיאה בטעינת וטרינרים", e);
                        Context context = getContext();
                        if (context != null) {
                            Toast.makeText(context, "שגיאה בטעינת וטרינרים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /**
     * Shows a selection dialog with available options for chat creation.
     *
     * @param optionsList List of options to display in the dialog
     * @param idMap Map connecting option names to their IDs
     */
    private void showSelectionDialog(List<String> optionsList, Map<String, String> idMap) {
        if (optionsList.isEmpty()) return;

        String[] options = optionsList.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("בחר עם מי להתחיל שיחה");
        builder.setItems(options, (dialog, which) -> {
            String selected = options[which];
            String selectedId = idMap.get(selected);
            createNewChat(selected, selectedId);
        });
        builder.show();
    }

    /**
     * Creates a new chat between the current user and the selected participant.
     * First checks if a chat already exists between the participants.
     *
     * @param selectedName The display name of the selected participant
     * @param selectedId The unique ID of the selected participant
     */
    private void createNewChat(String selectedName, String selectedId) {
        String currentUserId = auth.getCurrentUser().getUid();
        if (currentUserId == null || selectedId == null || selectedId.isEmpty()) {
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "שגיאה: משתמש לא תקין (חסר מזהה משתמש או מזהה יעד)", Toast.LENGTH_LONG).show();
            }
            Log.e(TAG, "createNewChat: currentUserId או selectedId ריקים");
            return;
        }

        db.collection("Chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
                    boolean found = false;
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        List<String> participants = (List<String>) doc.get("participants");
                        if (participants != null && participants.contains(selectedId) && participants.contains(currentUserId) && participants.size() == 2) {
                            String displayName, imageUrl;
                            if (isVet) {
                                displayName = doc.getString("dogName");
                                imageUrl = doc.getString("dogImageUrl");
                            } else {
                                displayName = doc.getString("vetName");
                                imageUrl = doc.getString("vetImageUrl");
                            }
                            ChatPreview chatPreview = new ChatPreview(doc.getId(), displayName, imageUrl);
                            openChatFragment(chatPreview);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        proceedWithChatCreation(currentUserId, selectedName, selectedId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "שגיאה בטעינת צ'אט", e);
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, "שגיאה בטעינת צ'אט: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Proceeds with creating a new chat after confirming no existing chat exists.
     * Fetches necessary participant details based on user type.
     *
     * @param currentUserId The current user's ID
     * @param selectedName The selected participant's display name
     * @param selectedId The selected participant's ID
     */
    private void proceedWithChatCreation(String currentUserId, String selectedName, String selectedId) {
        if (isVet) {
            db.collection("DogProfiles").document(selectedId).get()
                    .addOnSuccessListener(dogDoc -> {
                        if (dogDoc.exists()) {
                            String ownerId = dogDoc.getString("ownerId");
                            String dogImageUrl = dogDoc.getString("profileImageUrl");
                            String dogName = dogDoc.getString("name");
                            if (dogImageUrl == null || dogImageUrl.isEmpty()) {
                                dogImageUrl = "https://example.com/default_dog_image.png";
                            }
                            if (dogName == null || dogName.isEmpty()) {
                                dogName = "כלב";
                            }
                            Log.d(TAG, "Dog details - Name: " + dogName + ", Image: " + dogImageUrl);
                            String chatId = selectedId + "_" + currentUserId;
                            saveChatToFirestore(chatId, currentUserId, selectedId, dogName, dogImageUrl, "וטרינר", "https://example.com/default_vet_image.png");
                        } else {
                            Context context = getContext();
                            if (context != null) {
                                Toast.makeText(context, "לא נמצא כלב", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading dog", e);
                        Context context = getContext();
                        if (context != null) {
                            Toast.makeText(context, "שגיאה בטעינת כלב: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            db.collection("Veterinarians").document(selectedId).get()
                    .addOnSuccessListener(vetDoc -> {
                        if (vetDoc.exists()) {
                            String vetImageUrl = vetDoc.getString("profileImageUrl");
                            if (vetImageUrl != null) vetImageUrl = vetImageUrl.trim();
                            if (vetImageUrl == null || vetImageUrl.isEmpty()) {
                                vetImageUrl = "https://example.com/default_vet_image.png";
                            }
                            String vetName = vetDoc.getString("fullName");
                            if (vetName == null || vetName.trim().isEmpty()) {
                                vetName = vetDoc.getString("email");
                                if (vetName == null || vetName.trim().isEmpty()) {
                                    vetName = "וטרינר";
                                }
                            }
                            final String finalVetName = vetName;
                            final String finalVetImageUrl = vetImageUrl;
                            db.collection("DogProfiles")
                                    .whereEqualTo("ownerId", currentUserId)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(dogQuery -> {
                                        String dogName = "כלב";
                                        String dogImageUrl = "https://example.com/default_dog_image.png";
                                        String dogId = "";
                                        if (!dogQuery.isEmpty()) {
                                            DocumentSnapshot dogDoc = dogQuery.getDocuments().get(0);
                                            dogName = dogDoc.getString("name");
                                            dogImageUrl = dogDoc.getString("profileImageUrl");
                                            dogId = dogDoc.getId();
                                            if (dogName == null || dogName.isEmpty()) dogName = "כלב";
                                            if (dogImageUrl == null || dogImageUrl.isEmpty()) dogImageUrl = "https://example.com/default_dog_image.png";
                                        }
                                        String chatId = (dogId.isEmpty() ? currentUserId : dogId) + "_" + selectedId;
                                        saveChatToFirestore(chatId, currentUserId, selectedId, dogName, dogImageUrl, finalVetName, finalVetImageUrl);
                                    })
                                    .addOnFailureListener(e -> {
                                        String chatId = currentUserId + "_" + selectedId;
                                        saveChatToFirestore(chatId, currentUserId, selectedId, "כלב", "https://example.com/default_dog_image.png", finalVetName, finalVetImageUrl);
                                    });
                        } else {
                            Context context = getContext();
                            if (context != null) {
                                Toast.makeText(context, "לא נמצא וטרינר", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "שגיאה בטעינת וטרינר", e);
                        Context context = getContext();
                        if (context != null) {
                            Toast.makeText(context, "שגיאה בטעינת וטרינר: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /**
     * Saves the chat data to Firestore with all necessary participant information.
     *
     * @param chatId The unique chat identifier
     * @param currentUserId The current user's ID
     * @param otherId The other participant's ID
     * @param dogName The dog's name
     * @param dogImageUrl The dog's profile image URL
     * @param vetName The veterinarian's name
     * @param vetImageUrl The veterinarian's profile image URL
     */
    private void saveChatToFirestore(String chatId, String currentUserId, String otherId,
                                     String dogName, String dogImageUrl, String vetName, String vetImageUrl) {
        if (currentUserId == null || otherId == null) {
            Log.e(TAG, "saveChatToFirestore: חסרים מזההי משתמשים");
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "שגיאה: חסרים פרטי משתמש", Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (auth.getCurrentUser() == null || !auth.getCurrentUser().getUid().equals(currentUserId)) {
            Log.e(TAG, "saveChatToFirestore: המשתמש לא מחובר או המזהה לא תואם");
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "שגיאה: המשתמש לא מחובר", Toast.LENGTH_LONG).show();
            }
            return;
        }

        Map<String, Object> data = new HashMap<>();
        List<String> participants = Arrays.asList(currentUserId, otherId);
        data.put("participants", participants);
        data.put("lastMessage", "התחל שיחה...");
        data.put("lastMessageTime", new Date());

        if (isVet) {
            data.put("dogName", dogName != null && !dogName.isEmpty() ? dogName : "כלב");
            data.put("dogImageUrl", dogImageUrl != null && !dogImageUrl.isEmpty() ? dogImageUrl : "https://example.com/default_dog_image.png");
            data.put("vetName", vetName != null && !vetName.trim().isEmpty() ? vetName : "וטרינר");
            data.put("vetImageUrl", vetImageUrl != null && !vetImageUrl.trim().isEmpty() ? vetImageUrl : "https://example.com/default_vet_image.png");
            data.put("dogId", chatId.split("_")[0]);
            data.put("vetId", currentUserId);
            data.put("ownerId", otherId);

            Log.d(TAG, "Saving chat as vet - Dog details:");
            Log.d(TAG, "dogName: " + data.get("dogName"));
            Log.d(TAG, "dogImageUrl: " + data.get("dogImageUrl"));
            Log.d(TAG, "dogId: " + data.get("dogId"));
        } else {
            data.put("vetName", vetName != null && !vetName.trim().isEmpty() ? vetName : "וטרינר");
            data.put("vetImageUrl", vetImageUrl != null && !vetImageUrl.trim().isEmpty() ? vetImageUrl : "https://example.com/default_vet_image.png");
            data.put("dogName", dogName != null && !dogName.isEmpty() ? dogName : "כלב");
            data.put("dogImageUrl", dogImageUrl != null && !dogImageUrl.isEmpty() ? dogImageUrl : "https://example.com/default_dog_image.png");
            data.put("dogId", chatId.split("_")[0]);
            data.put("vetId", otherId);
            data.put("ownerId", currentUserId);

            Log.d(TAG, "Saving chat as owner - Vet details:");
            Log.d(TAG, "vetName: " + data.get("vetName"));
            Log.d(TAG, "vetImageUrl: " + data.get("vetImageUrl"));
            Log.d(TAG, "vetId: " + data.get("vetId"));
        }

        Log.d(TAG, "Creating chat with ID: " + chatId);
        Log.d(TAG, "Current user ID: " + currentUserId);
        Log.d(TAG, "Other user ID: " + otherId);
        Log.d(TAG, "Participants: " + participants);
        Log.d(TAG, "Data: " + data);

        createChatDocument(chatId, data);
    }

    /**
     * Creates the chat document in Firestore and handles the response.
     *
     * @param chatId The unique chat identifier
     * @param data The chat data to be saved
     */
    private void createChatDocument(String chatId, Map<String, Object> data) {
        if (data == null || !data.containsKey("participants")) {
            Log.e(TAG, "createChatDocument: data או participants חסרים");
            return;
        }

        DocumentReference chatRef = db.collection("Chats").document(chatId);
        chatRef.set(data)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "צ'אט חדש נוצר בהצלחה: " + chatId);
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, "נוצר צ'אט חדש", Toast.LENGTH_SHORT).show();
                    }
                    String displayName = isVet ? (String) data.get("dogName") : (String) data.get("vetName");
                    String imageUrl = isVet ? (String) data.get("dogImageUrl") : (String) data.get("vetImageUrl");
                    ChatPreview chatPreview = new ChatPreview(chatId, displayName, imageUrl);
                    openChatFragment(chatPreview);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "שגיאה ביצירת צ'אט", e);
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        Context context = getContext();
                        if (context != null) {
                            Toast.makeText(context, "שגיאה ביצירת צ'אט: אין הרשאות. בדקי את כללי האבטחה של Firestore.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Context context = getContext();
                        if (context != null) {
                            Toast.makeText(context, "שגיאה ביצירת צ'אט: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Initializes user chats collection in Firestore for a new user.
     * This method should be called from the login activity or main activity.
     *
     * @param userId The user's unique identifier
     * @param isVet Whether the user is a veterinarian
     * @param db The Firestore database instance
     */
    public static void initializeUserChats(String userId, boolean isVet, FirebaseFirestore db) {
        db.collection("UserChats").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("isVet", isVet);
                        userData.put("lastSeen", new Date());

                        db.collection("UserChats").document(userId)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "קולקשיין צ'אט משתמש אותחל"))
                                .addOnFailureListener(e -> Log.e(TAG, "שגיאה באתחול קולקשיין צ'אט משתמש", e));
                    }
                });
    }
}