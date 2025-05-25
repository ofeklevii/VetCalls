package com.example.vetcalls.obj;
import com.example.vetcalls.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import com.example.vetcalls.obj.DogProfileViewHolder;

public class DogProfileAdapter extends RecyclerView.Adapter<DogProfileViewHolder> {

    private static final String TAG = "DogProfileAdapter";
    private List<DogProfile> dogList;
    private Context context;
    private DogProfile currentDog; // פרופיל הכלב הנוכחי המוצג למעלה
    private OnDogClickListener onDogClickListener; // ממשק מאזין לחיצות
    private int baseIndex = 0;

    // ממשק לטיפול באירועי לחיצה על פרופיל כלב
    public interface OnDogClickListener {
        void onDogClick(int realIndex);
    }

    public DogProfileAdapter(Context context, List<DogProfile> dogList, OnDogClickListener listener, int baseIndex) {
        this.context = context;
        this.dogList = dogList != null ? dogList : new ArrayList<>();
        this.onDogClickListener = listener;
        this.baseIndex = baseIndex;
    }

    // הגדרת הכלב הנוכחי המוצג למעלה - כעת רק מסמן את הכלב אבל לא מוציא אותו מהרשימה
    public void setCurrentDog(DogProfile dog) {
        if (currentDog != null) {
            currentDog.isCurrent = false;
            int oldPosition = dogList.indexOf(currentDog);
            if (oldPosition != -1) {
                notifyItemChanged(oldPosition);
            }
        }

        currentDog = dog;
        if (currentDog != null) {
            currentDog.isCurrent = true;
            int newPosition = dogList.indexOf(currentDog);
            if (newPosition != -1) {
                notifyItemChanged(newPosition);
            }
        }
    }

    @NonNull
    @Override
    public DogProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.dog_item, parent, false);
        return new DogProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DogProfileViewHolder holder, int position) {
        DogProfile dog = dogList.get(position);
        Log.d(TAG, "Binding dog: " + dog.name + ", id: " + dog.dogId + ", isCurrent: " + dog.isCurrent);

        // הצגת שם הכלב
        holder.dogName.setText(dog.name);

        // הצגת גיל הכלב
        String ageText = "Age: ";
        if (dog.age != null) {
            ageText += dog.age;
        } else {
            ageText += "Unknown";
        }
        holder.dogAge.setText(ageText);

        // הסתרת הביו ברשימה (רק שם ותמונה וגיל)
        if (holder.dogBio != null) {
            holder.dogBio.setVisibility(View.GONE);
        }

        // טעינת תמונה
        if (dog.profileImageUrl != null && !dog.profileImageUrl.isEmpty()) {
            Glide.with(context)
                    .load(dog.profileImageUrl)
                    .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                    .circleCrop()
                    .into(holder.dogImage);
        } else {
            holder.dogImage.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
        }

        // הוספת הדגשה לכלב הנוכחי המוצג
        if (dog.isCurrent) {
            holder.itemView.setBackgroundResource(R.drawable.selected_dog_background);
            Log.d(TAG, "Applying background to: " + dog.name);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        // הוספת מאזין לחיצה על כרטיס
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Dog card clicked: " + dog.name);
            if (onDogClickListener != null) {
                onDogClickListener.onDogClick(position + baseIndex);
            } else {
                updateProfile(dog);
            }
        });
    }

    private void updateProfile(DogProfile dog) {
        // שימוש ב-SharedPreferences ישירות לעדכון הפרופיל
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // שמירת פרטי הכלב המוצג בפרופיל העליון
        editor.putString("name", dog.name);
        editor.putString("age", dog.age != null ? dog.age : "");
        editor.putString("race", dog.race);
        editor.putString("birthday", dog.birthday);
        editor.putString("weight", dog.weight);
        editor.putString("allergies", dog.allergies);
        editor.putString("vaccines", dog.vaccines);

        // שמירת המזהה של הכלב הנוכחי המוצג (חשוב לעריכה)
        if (dog.dogId != null) {
            editor.putString("dogId", dog.dogId);
        } else {
            editor.putString("currentDogName", dog.name);
        }

        // בניית הביו המורחב שיוצג למעלה
        StringBuilder bioBuilder = new StringBuilder();

        // הוספת משקל
        if (dog.weight != null && !dog.weight.isEmpty()) {
            bioBuilder.append("Weight: ").append(dog.weight).append(" kg\n");
        }

        // הוספת גזע
        if (dog.race != null && !dog.race.isEmpty()) {
            bioBuilder.append("Race: ").append(dog.race).append("\n");
        }

        // הוספת אלרגיות
        if (dog.allergies != null && !dog.allergies.isEmpty()) {
            bioBuilder.append("Allergies: ").append(dog.allergies).append("\n");
        }

        // הוספת חיסונים
        if (dog.vaccines != null && !dog.vaccines.isEmpty()) {
            bioBuilder.append("Vaccines: ").append(dog.vaccines);
        }

        editor.putString("bio", bioBuilder.toString().trim());

        // שמירת תמונה
        if (dog.profileImageUrl != null && !dog.profileImageUrl.isEmpty()) {
            editor.putString("profileImageUrl", dog.profileImageUrl);
        }

        editor.apply();

        // עדכון ה-UI בפעילות
        if (context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;

            // מציאת ה-views ועדכון שלהם
            TextView userName = activity.findViewById(R.id.userName);
            TextView dogAge = activity.findViewById(R.id.dogAge);
            TextView bioTextView = activity.findViewById(R.id.bioText);
            ImageView profilePic = activity.findViewById(R.id.profilePic);

            if (userName != null) userName.setText(dog.name);
            if (dogAge != null) dogAge.setText("Age: " + (dog.age != null ? dog.age : ""));
            if (bioTextView != null) bioTextView.setText(bioBuilder.toString().trim());

            if (profilePic != null && dog.profileImageUrl != null && !dog.profileImageUrl.isEmpty()) {
                try {
                    Glide.with(context)
                            .load(dog.profileImageUrl)
                            .circleCrop()
                            .into(profilePic);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading profile image: " + e.getMessage());
                }
            }
        }

        // סימון הכלב הנוכחי ברשימה
        setCurrentDog(dog);
    }

    // עדכון רשימת הכלבים
    public void updateDogList(List<DogProfile> newList) {
        dogList.clear();
        dogList.addAll(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return dogList.size();
    }
}