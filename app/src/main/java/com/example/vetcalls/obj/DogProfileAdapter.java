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

public class DogProfileAdapter extends RecyclerView.Adapter<DogProfileAdapter.DogViewHolder> {

    private static final String TAG = "DogProfileAdapter";
    private List<DogProfile> dogList;
    private Context context;
    private DogProfile currentDog; // פרופיל הכלב הנוכחי המוצג למעלה
    private OnDogClickListener onDogClickListener; // ממשק מאזין לחיצות

    // ממשק לטיפול באירועי לחיצה על פרופיל כלב
    public interface OnDogClickListener {
        void onDogClick(DogProfile dog);
    }

    public DogProfileAdapter(Context context, List<DogProfile> dogList) {
        this.context = context;
        this.dogList = dogList != null ? dogList : new ArrayList<>();
    }

    public DogProfileAdapter(Context context, List<DogProfile> dogList, OnDogClickListener listener) {
        this.context = context;
        this.dogList = dogList != null ? dogList : new ArrayList<>();
        this.onDogClickListener = listener;
    }

    // הגדרת הכלב הנוכחי המוצג למעלה - כעת רק מסמן את הכלב אבל לא מוציא אותו מהרשימה
    public void setCurrentDog(DogProfile dog) {
        this.currentDog = dog;
        Log.d(TAG, "Setting current dog: " + (dog != null ? dog.getName() : "null"));

        // סימון הכלב הנוכחי בתוך הרשימה - לא מסיר אותו מהרשימה
        for (DogProfile profile : dogList) {
            if (profile.getId() != null && dog != null && dog.getId() != null && profile.getId().equals(dog.getId())) {
                profile.setCurrent(true);
                Log.d(TAG, "Marked dog as current: " + profile.getName());
            } else {
                profile.setCurrent(false);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.dog_item, parent, false);
        return new DogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DogViewHolder holder, int position) {
        DogProfile dog = dogList.get(position);
        Log.d(TAG, "Binding dog: " + dog.getName() + ", id: " + dog.getId() + ", isCurrent: " + dog.isCurrent());

        holder.dogName.setText(dog.getName());

        // בטוח יותר - טיפול בכל המקרים של ה-age
        String ageText = "Age: ";
        if (dog.getAge() != null) {
            ageText += dog.getAge();
        } else {
            ageText += "Unknown";
        }
        holder.dogAge.setText(ageText);

        // שינוי בהצגת הביו - נציג רק את הגזע בכרטיס
        StringBuilder shortBio = new StringBuilder();
        if (dog.getRace() != null && !dog.getRace().isEmpty()) {
            shortBio.append("Race: ").append(dog.getRace());
        }
        if (holder.dogBio != null) {
            holder.dogBio.setText(shortBio.toString());
        }

        // טעינת תמונה
        if (dog.getImageUrl() != null && !dog.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(dog.getImageUrl())
                    .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                    .circleCrop()
                    .into(holder.dogImage);
        } else {
            holder.dogImage.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
        }

        // הוספת הדגשה לכלב הנוכחי המוצג (אופציונלי)
        if (dog.isCurrent()) {
            holder.itemView.setBackgroundResource(R.drawable.selected_dog_background);
            Log.d(TAG, "Applying background to: " + dog.getName());
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        // הוספת מאזין לחיצה על כרטיס - שני מסלולים אפשריים
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Dog card clicked: " + dog.getName());
            // אם הממשק הוגדר, נשתמש בו
            if (onDogClickListener != null) {
                onDogClickListener.onDogClick(dog);
            } else {
                // אחרת, נשתמש בפונקציה המקורית
                updateProfile(dog);
            }
        });
    }

    private void updateProfile(DogProfile dog) {
        // שימוש ב-SharedPreferences ישירות לעדכון הפרופיל
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // שמירת פרטי הכלב המוצג בפרופיל העליון
        editor.putString("name", dog.getName());
        // טיפול בטוח יותר ב-age
        editor.putString("age", dog.getAge() != null ? dog.getAge() : "");
        editor.putString("race", dog.getRace());
        editor.putString("birthday", dog.getBirthday());

        // שמירת המזהה של הכלב הנוכחי המוצג (חשוב לעריכה)
        if (dog.getId() != null) {
            editor.putString("dogId", dog.getId());
        } else {
            editor.putString("currentDogName", dog.getName());
        }

        // בניית הביו
        StringBuilder bioBuilder = new StringBuilder();
        if (dog.getRace() != null && !dog.getRace().isEmpty()) {
            bioBuilder.append("Race: ").append(dog.getRace()).append("\n");
        }
        if (dog.getBirthday() != null && !dog.getBirthday().isEmpty()) {
            bioBuilder.append("Birthday: ").append(dog.getBirthday()).append("\n");
        }

        // הוספת פרטים מהביו המקורי
        String originalBio = dog.getBio();
        if (originalBio != null && !originalBio.isEmpty()) {
            String[] lines = originalBio.split("\n");
            for (String line : lines) {
                if (line.startsWith("Weight:") || line.startsWith("Allergies:") || line.startsWith("Vaccines:")) {
                    bioBuilder.append(line).append("\n");
                }
            }
        }

        editor.putString("bio", bioBuilder.toString().trim());

        // שמירת תמונה
        if (dog.getImageUrl() != null && !dog.getImageUrl().isEmpty()) {
            editor.putString("profileImageUrl", dog.getImageUrl());
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

            if (userName != null) userName.setText(dog.getName());
            if (dogAge != null) dogAge.setText("Age: " + (dog.getAge() != null ? dog.getAge() : ""));
            if (bioTextView != null) bioTextView.setText(bioBuilder.toString().trim());

            if (profilePic != null && dog.getImageUrl() != null && !dog.getImageUrl().isEmpty()) {
                try {
                    Glide.with(context)
                            .load(dog.getImageUrl())
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
    public void updateDogList(List<DogProfile> newDogList) {
        this.dogList = newDogList != null ? newDogList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return dogList.size();
    }

    public static class DogViewHolder extends RecyclerView.ViewHolder {
        ImageView dogImage;
        TextView dogName, dogAge, dogBio;
        private static final String VIEW_HOLDER_TAG = "DogViewHolder";

        public DogViewHolder(@NonNull View itemView) {
            super(itemView);
            // שינוי כאן - בדיקה אם ה-ID קיים לפני מציאת ה-View
            dogImage = itemView.findViewById(R.id.dogImage); // וודא שזה ה-ID הנכון בקובץ dog_item.xml
            dogName = itemView.findViewById(R.id.dogName);   // וודא שזה ה-ID הנכון בקובץ dog_item.xml
            dogAge = itemView.findViewById(R.id.dogAge);     // וודא שזה ה-ID הנכון בקובץ dog_item.xml
            dogBio = itemView.findViewById(R.id.dogBio);     // וודא שזה ה-ID הנכון בקובץ dog_item.xml

            // לוג לבדיקה - עם TAG ייעודי למחלקה הפנימית
            Log.d(VIEW_HOLDER_TAG, "DogViewHolder initialized. Views found: " +
                    "dogImage=" + (dogImage != null) +
                    ", dogName=" + (dogName != null) +
                    ", dogAge=" + (dogAge != null) +
                    ", dogBio=" + (dogBio != null));
        }
    }
}