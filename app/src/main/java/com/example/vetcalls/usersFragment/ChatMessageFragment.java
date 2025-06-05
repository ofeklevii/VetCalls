package com.example.vetcalls.usersFragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vetcalls.R;
import com.example.vetcalls.obj.MessageAdapter;
import com.example.vetcalls.obj.Message;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Fragment for displaying and managing chat messages between users.
 * Provides comprehensive messaging functionality including text messages, media sharing,
 * real-time message updates, and notification handling for both veterinarians and dog owners.
 *
 * @author Ofek Levi
 */
public class ChatMessageFragment extends Fragment {

    private static final String TAG = "ChatMessageFragment";
    private static final String ARG_CHAT_ID = "chatId";
    private static final String ARG_RECIPIENT_NAME = "recipientName";
    private static final String ARG_RECIPIENT_IMAGE = "recipientImage";
    private static final String ARG_IS_VET = "isVet";

    private ImageView recipientImage;
    private TextView recipientName;
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private FloatingActionButton sendButton;
    private ImageButton attachButton, backButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String chatId;
    private String recipientDisplayName;
    private String recipientImageUrl;
    private String currentUserId;
    private ArrayList<Message> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private boolean isVet;

    private FrameLayout mediaPreviewLayout;
    private ImageView imagePreview;
    private VideoView videoPreview;
    private ImageButton closeMediaButton;
    private Uri selectedMediaUri = null;
    private String selectedMediaType = null;

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_VIDEO_PICK = 1002;
    private static final int REQUEST_CAMERA = 1003;

    /**
     * Creates a new instance of ChatMessageFragment with the specified parameters.
     *
     * @param chatId Unique identifier for the chat
     * @param recipientName Display name of the message recipient
     * @param recipientImage Profile image URL of the recipient
     * @param isVet Whether the current user is a veterinarian
     * @return New ChatMessageFragment instance
     */
    public static ChatMessageFragment newInstance(String chatId, String recipientName,
                                                  String recipientImage, boolean isVet) {
        ChatMessageFragment fragment = new ChatMessageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_ID, chatId);
        args.putString(ARG_RECIPIENT_NAME, recipientName);
        args.putString(ARG_RECIPIENT_IMAGE, recipientImage);
        args.putBoolean(ARG_IS_VET, isVet);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called when the fragment is first created.
     * Initializes Firebase instances and retrieves fragment arguments.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        if (getArguments() != null) {
            chatId = getArguments().getString(ARG_CHAT_ID);
            recipientDisplayName = getArguments().getString(ARG_RECIPIENT_NAME);
            recipientImageUrl = getArguments().getString(ARG_RECIPIENT_IMAGE);
            isVet = getArguments().getBoolean(ARG_IS_VET);
        }
    }

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views
     * @param container The parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     * @return The View for the fragment's UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_message, container, false);

        recipientImage = view.findViewById(R.id.recipientImage);
        recipientName = view.findViewById(R.id.recipientName);
        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        attachButton = view.findViewById(R.id.attachButton);
        backButton = view.findViewById(R.id.backButton);

        mediaPreviewLayout = view.findViewById(R.id.mediaPreviewLayout);
        imagePreview = view.findViewById(R.id.imagePreview);
        videoPreview = view.findViewById(R.id.videoPreview);
        closeMediaButton = view.findViewById(R.id.closeMediaButton);

        return view;
    }

    /**
     * Called immediately after onCreateView has returned.
     * Sets up UI components, event listeners, and starts listening for messages.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recipientName.setText(recipientDisplayName);
        if (recipientImageUrl != null && !recipientImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(recipientImageUrl)
                    .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                    .circleCrop()
                    .into(recipientImage);
        }

        messageAdapter = new MessageAdapter(requireContext(), messageList, currentUserId);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesRecyclerView.setAdapter(messageAdapter);

        sendButton.setOnClickListener(v -> {
            if (selectedMediaUri != null && selectedMediaType != null) {
                sendMediaMessage();
            } else {
                sendMessage();
            }
        });
        attachButton.setOnClickListener(v -> showAttachmentOptions());
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        closeMediaButton.setOnClickListener(v -> {
            selectedMediaUri = null;
            selectedMediaType = null;
            mediaPreviewLayout.setVisibility(View.GONE);
            imagePreview.setVisibility(View.GONE);
            videoPreview.setVisibility(View.GONE);
            closeMediaButton.setVisibility(View.GONE);
        });

        listenForMessages();
    }

    /**
     * Sets up real-time listener for chat messages from Firestore.
     * Updates the message list and UI automatically when new messages arrive.
     */
    private void listenForMessages() {
        if (chatId == null || chatId.isEmpty()) {
            Log.e(TAG, "מזהה צ'אט הוא null או ריק");
            Toast.makeText(getContext(), "שגיאה: צ'אט לא תקין", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Chats").document(chatId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "שגיאה בהאזנה להודעות", e);
                        return;
                    }

                    if (queryDocumentSnapshots == null) {
                        return;
                    }

                    messageList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Message message = doc.toObject(Message.class);
                        messageList.add(message);
                    }

                    messageAdapter.notifyDataSetChanged();

                    if (messageList.size() > 0) {
                        messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    /**
     * Sends a text message to the chat.
     * Updates the last message information and triggers notification sending.
     */
    private void sendMessage() {
        String text = messageInput.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        Message message = new Message(
                currentUserId,
                new Date(),
                "text",
                text
        );

        db.collection("Chats").document(chatId)
                .collection("Messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    messageInput.setText("");

                    Map<String, Object> lastMessageData = new HashMap<>();
                    lastMessageData.put("lastMessage", text);
                    lastMessageData.put("lastMessageTime", new Date());

                    db.collection("Chats").document(chatId)
                            .update(lastMessageData)
                            .addOnSuccessListener(aVoid -> {
                                sendNotification(text);
                            })
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "שגיאה בעדכון ההודעה האחרונה", e));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "שגיאה בשליחת הודעה", e);
                    Toast.makeText(getContext(), "שגיאה בשליחת הודעה", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Sends a notification to the message recipient.
     * Creates notification data and stores it in Firestore for processing.
     *
     * @param messageText The text content of the message to include in the notification
     */
    private void sendNotification(String messageText) {
        db.collection("Chats").document(chatId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String receiverId;
                    String senderName;
                    String senderImage;

                    if (isVet) {
                        receiverId = documentSnapshot.getString("ownerId");
                        senderName = documentSnapshot.getString("vetName");
                        senderImage = documentSnapshot.getString("vetImageUrl");
                    } else {
                        receiverId = documentSnapshot.getString("vetId");
                        senderName = documentSnapshot.getString("dogName");
                        senderImage = documentSnapshot.getString("dogImageUrl");
                    }

                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("receiverId", receiverId);
                    notificationData.put("title", senderName);
                    notificationData.put("message", messageText);
                    notificationData.put("senderImage", senderImage);
                    notificationData.put("chatId", chatId);

                    db.collection("Notifications")
                            .add(notificationData)
                            .addOnSuccessListener(documentReference ->
                                    Log.d(TAG, "התראה נשלחה בהצלחה"))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "שגיאה בשליחת התראה", e));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "שגיאה בקבלת פרטי צ'אט", e));
    }

    /**
     * Shows attachment options popup menu for selecting media type.
     * Provides options for camera, gallery images, and videos.
     */
    private void showAttachmentOptions() {
        PopupMenu popup = new PopupMenu(requireContext(), attachButton);
        popup.getMenuInflater().inflate(R.menu.chat_attachment_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_camera) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
                return true;
            } else if (id == R.id.menu_gallery) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, REQUEST_IMAGE_PICK);
                return true;
            } else if (id == R.id.menu_video) {
                Intent videoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                videoIntent.setType("video/*");
                startActivityForResult(videoIntent, REQUEST_VIDEO_PICK);
                return true;
            }
            return false;
        });
        popup.show();
    }

    /**
     * Handles the result from media selection activities.
     * Processes selected images, videos, or camera captures for media preview.
     *
     * @param requestCode The request code used to start the activity
     * @param resultCode The result code returned by the activity
     * @param data The intent data containing the result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                selectedMediaUri = data.getData();
                selectedMediaType = "image";
                showMediaPreview();
            } else if (requestCode == REQUEST_VIDEO_PICK) {
                selectedMediaUri = data.getData();
                selectedMediaType = "video";
                showMediaPreview();
            } else if (requestCode == REQUEST_CAMERA) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                selectedMediaUri = getImageUri(requireContext(), imageBitmap);
                selectedMediaType = "image";
                showMediaPreview();
            }
        }
    }

    /**
     * Displays a preview of the selected media before sending.
     * Shows appropriate preview based on media type (image or video).
     */
    private void showMediaPreview() {
        mediaPreviewLayout.setVisibility(View.VISIBLE);
        closeMediaButton.setVisibility(View.VISIBLE);
        if ("image".equals(selectedMediaType)) {
            imagePreview.setVisibility(View.VISIBLE);
            videoPreview.setVisibility(View.GONE);
            imagePreview.setImageURI(selectedMediaUri);
        } else if ("video".equals(selectedMediaType)) {
            imagePreview.setVisibility(View.GONE);
            videoPreview.setVisibility(View.VISIBLE);
            videoPreview.setVideoURI(selectedMediaUri);
            videoPreview.start();
        }
    }

    /**
     * Converts a bitmap to a URI for storage and messaging purposes.
     *
     * @param context The application context
     * @param bitmap The bitmap to convert
     * @return URI representation of the bitmap
     */
    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "CameraImage", null);
        return Uri.parse(path);
    }

    /**
     * Uploads and sends a media message (image or video) to the chat.
     * Handles Firebase Storage upload and creates message with media URL.
     */
    private void sendMediaMessage() {
        if (selectedMediaUri == null || selectedMediaType == null) return;
        String fileName = UUID.randomUUID().toString();
        String path = "chat_media/" + chatId + "/" + fileName;
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(path);

        storageRef.putFile(selectedMediaUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Message message = new Message(
                            currentUserId,
                            new Date(),
                            selectedMediaType,
                            uri.toString()
                    );
                    db.collection("Chats").document(chatId)
                            .collection("Messages")
                            .add(message)
                            .addOnSuccessListener(documentReference -> {
                                selectedMediaUri = null;
                                selectedMediaType = null;
                                mediaPreviewLayout.setVisibility(View.GONE);
                                imagePreview.setVisibility(View.GONE);
                                videoPreview.setVisibility(View.GONE);
                                closeMediaButton.setVisibility(View.GONE);
                            });
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "שגיאה בשליחת מדיה", Toast.LENGTH_SHORT).show();
                });
    }
}