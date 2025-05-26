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

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private RecyclerView recyclerView;
    private FloatingActionButton startChatFab;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ChatPreviewAdapter adapter;
    private List<ChatPreview> chatList = new ArrayList<>();

    private boolean isVet = false; // יקבע לפי סוג המשתמש

    public ChatFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // בדיקה אם המשתמש הוא וטרינר
        String currentUserId = auth.getCurrentUser().getUid();
        db.collection("Veterinarians").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    isVet = documentSnapshot.exists();
                    Log.d(TAG, "User type checked - isVet: " + isVet);
                    // לאחר קביעת סוג המשתמש, רענן את רשימת הצ'אטים
                    if (recyclerView != null && adapter != null) {
                        updateAdapterUserType();
                        loadChatList();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user type", e);
                    // במקרה של שגיאה, נניח שהמשתמש הוא לא וטרינר
                    isVet = false;
                    if (recyclerView != null && adapter != null) {
                        updateAdapterUserType();
                        loadChatList();
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewChats);
        startChatFab = view.findViewById(R.id.startChatFab);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // אתחול האדפטר עם רשימת צ'אטים ריקה תחילה
        adapter = new ChatPreviewAdapter(chatList, chat -> openChatFragment(chat));
        recyclerView.setAdapter(adapter);

        startChatFab.setOnClickListener(v -> openNewChatDialog());

        // טען את רשימת הצ'אטים רק אם אנחנו כבר יודעים את סוג המשתמש
        if (isVet || isVet == false) {
            loadChatList();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // רענן את רשימת הצ'אטים בכל פעם שהפרגמנט מתחדש
        loadChatList();
    }

    private void updateAdapterUserType() {
        adapter = new ChatPreviewAdapter(chatList, chat -> openChatFragment(chat));
        recyclerView.setAdapter(adapter);
    }

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
                            if (displayName == null || displayName.isEmpty()) displayName = "כלב";
                            if (imageUrl == null || imageUrl.isEmpty()) imageUrl = "https://example.com/default_dog_image.png";
                        } else {
                            displayName = doc.getString("vetName");
                            imageUrl = doc.getString("vetImageUrl");
                            if (displayName == null || displayName.isEmpty()) displayName = "וטרינר";
                            if (imageUrl == null || imageUrl.isEmpty()) imageUrl = "https://example.com/default_vet_image.png";
                        }
                        Log.d(TAG, "ChatPreview: chatId=" + chatId + ", displayName=" + displayName + ", imageUrl=" + imageUrl);

                        ChatPreview chatPreview = new ChatPreview(chatId, displayName, imageUrl, lastMessage, lastMessageTime);
                        chatList.add(chatPreview);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        // מטפל במקרה של שגיאה שלא ניתן לקרוא לצ'אטים
                    }
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, "אין לך עדיין צ'אטים, צרי שיחה חדשה!", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openNewChatDialog() {
        String currentUserId = auth.getCurrentUser().getUid();
        if (isVet) {
            db.collection("DogProfiles").whereEqualTo("vetId", currentUserId).get()
                    .addOnSuccessListener(query -> {
                        List<String> dogNames = new ArrayList<>();
                        Map<String, String> dogIdMap = new HashMap<>();

                        for (DocumentSnapshot doc : query.getDocuments()) {
                            DogProfile dog = doc.toObject(DogProfile.class);
                            String name = dog != null ? dog.name : null;
                            if (name != null) {
                                dogNames.add(name);
                                dogIdMap.put(name, doc.getId());
                            }
                        }

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

        // חפש צ'אט קיים עם אותם participants
        db.collection("Chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
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
                        return;
                    }
                    // לא נמצא צ'אט - צור חדש
                    proceedWithChatCreation(currentUserId, selectedName, selectedId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "שגיאה בטעינת צ'אט", e);
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, "שגיאה בטעינת צ'אט: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void proceedWithChatCreation(String currentUserId, String selectedName, String selectedId) {
        if (isVet) {
            // שלוף את פרטי הכלב
            db.collection("DogProfiles").document(selectedId).get()
                    .addOnSuccessListener(dogDoc -> {
                        if (dogDoc.exists()) {
                            String ownerId = dogDoc.getString("ownerId");
                            String dogImageUrl = dogDoc.getString("profileImageUrl");
                            if (dogImageUrl == null || dogImageUrl.isEmpty()) {
                                dogImageUrl = "https://example.com/default_dog_image.png";
                            }
                            String dogName = dogDoc.getString("name");
                            if (dogName == null || dogName.isEmpty()) {
                                dogName = "כלב";
                            }
                            String chatId = selectedId + "_" + currentUserId;
                            saveChatToFirestore(chatId, currentUserId, ownerId, dogName, dogImageUrl);
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
            // שלוף את פרטי הווטרינר
            db.collection("Veterinarians").document(selectedId).get()
                    .addOnSuccessListener(vetDoc -> {
                        if (vetDoc.exists()) {
                            Veterinarian vet = vetDoc.toObject(Veterinarian.class);
                            String vetImageUrl = vet != null ? vet.profileImageUrl : null;
                            if (vetImageUrl == null || vetImageUrl.isEmpty()) {
                                vetImageUrl = "https://example.com/default_vet_image.png";
                            }
                            String vetName = vet != null ? vet.fullName : null;
                            if (vetName == null || vetName.isEmpty()) {
                                vetName = vet != null ? vet.email : null;
                                if (vetName == null || vetName.isEmpty()) {
                                    vetName = "וטרינר";
                                }
                            }
                            String chatId = currentUserId + "_" + selectedId;
                            saveChatToFirestore(chatId, currentUserId, selectedId, vetName, vetImageUrl);
                        } else {
                            Context context = getContext();
                            if (context != null) {
                                Toast.makeText(context, "לא נמצא וטרינר", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading veterinarian", e);
                        Context context = getContext();
                        if (context != null) {
                            Toast.makeText(context, "שגיאה בטעינת וטרינר: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void saveChatToFirestore(String chatId, String currentUserId, String otherId,
                                     String displayName, String imageUrl) {
        // וידוא שיש לנו את כל המידע הנדרש
        if (currentUserId == null || otherId == null) {
            Log.e(TAG, "saveChatToFirestore: חסרים מזההי משתמשים");
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "שגיאה: חסרים פרטי משתמש", Toast.LENGTH_LONG).show();
            }
            return;
        }

        // וידוא שהמשתמש הנוכחי מחובר
        if (auth.getCurrentUser() == null || !auth.getCurrentUser().getUid().equals(currentUserId)) {
            Log.e(TAG, "saveChatToFirestore: המשתמש לא מחובר או המזהה לא תואם");
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "שגיאה: המשתמש לא מחובר", Toast.LENGTH_LONG).show();
            }
            return;
        }

        Map<String, Object> data = new HashMap<>();
        // יצירת מערך participants עם שני המשתתפים
        List<String> participants = Arrays.asList(currentUserId, otherId);
        data.put("participants", participants);
        data.put("lastMessage", "התחל שיחה...");
        data.put("lastMessageTime", new Date());

        // הוספת פרטים אמיתיים לפי סוג המשתמש
        if (isVet) {
            data.put("dogName", displayName != null && !displayName.isEmpty() ? displayName : "כלב");
            data.put("dogImageUrl", imageUrl != null && !imageUrl.isEmpty() ? imageUrl : "https://example.com/default_dog_image.png");
            data.put("vetName", "וטרינר");
            data.put("vetImageUrl", "https://example.com/default_vet_image.png");
            data.put("dogId", chatId.split("_")[0]);
            data.put("vetId", currentUserId);
            data.put("ownerId", otherId);
        } else {
            data.put("vetName", displayName != null && !displayName.isEmpty() ? displayName : "וטרינר");
            data.put("vetImageUrl", imageUrl != null && !imageUrl.isEmpty() ? imageUrl : "https://example.com/default_vet_image.png");
            data.put("dogName", "כלב");
            data.put("dogImageUrl", "https://example.com/default_dog_image.png");
            data.put("dogId", chatId.split("_")[0]);
            data.put("vetId", otherId);
            data.put("ownerId", currentUserId);
        }

        // הוספת לוגים לדיבוג
        Log.d(TAG, "Creating chat with ID: " + chatId);
        Log.d(TAG, "Current user ID: " + currentUserId);
        Log.d(TAG, "Other user ID: " + otherId);
        Log.d(TAG, "Participants: " + participants);
        Log.d(TAG, "Data: " + data);

        createChatDocument(chatId, data);
    }

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
                    // פתיחת הצ'אט החדש
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

    // מתודה לקריאה כאשר המשתמש מתחבר לאתחול קולקשיין הצ'אטים שלו
    public static void initializeUserChats(String userId, boolean isVet, FirebaseFirestore db) {
        // יש לקרוא למתודה זו מפעילות ההתחברות או הפעילות הראשית שלך
        // היא מבטיחה שלמשתמש יש צומת צ'אט ב-Firestore

        db.collection("UserChats").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // יצירת מסמך צ'אט משתמש חדש
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