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

/**
 * RecyclerView adapter for displaying dog profiles in a list.
 * Handles the binding of DogProfile data to view holders, manages current dog selection,
 * and updates profile information in SharedPreferences and UI.
 *
 * @author Ofek Levi
 */
public class DogProfileAdapter extends RecyclerView.Adapter<DogProfileViewHolder> {

    private static final String TAG = "DogProfileAdapter";
    private List<DogProfile> dogList;
    private Context context;
    private DogProfile currentDog;
    private OnDogClickListener onDogClickListener;
    private int baseIndex = 0;

    /**
     * Interface for handling dog profile click events.
     */
    public interface OnDogClickListener {
        /**
         * Called when a dog profile is clicked.
         *
         * @param realIndex The real index of the clicked dog profile
         */
        void onDogClick(int realIndex);
    }

    /**
     * Constructor for creating the adapter with dog profiles and click listener.
     *
     * @param context The context for the adapter
     * @param dogList List of DogProfile objects to display
     * @param listener Listener for handling dog profile clicks
     * @param baseIndex Base index for calculating real positions
     */
    public DogProfileAdapter(Context context, List<DogProfile> dogList, OnDogClickListener listener, int baseIndex) {
        this.context = context;
        this.dogList = dogList != null ? dogList : new ArrayList<>();
        this.onDogClickListener = listener;
        this.baseIndex = baseIndex;
    }

    /**
     * Sets the current dog profile and updates the visual indication.
     * Marks the specified dog as current and updates the UI accordingly.
     *
     * @param dog The DogProfile to set as current
     */
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

    /**
     * Creates a new ViewHolder by inflating the dog item layout.
     *
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return A new DogProfileViewHolder that holds a View for the dog item
     */
    @NonNull
    @Override
    public DogProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.dog_item, parent, false);
        return new DogProfileViewHolder(view);
    }

    /**
     * Binds dog profile data to the ViewHolder at the specified position.
     * Sets up the dog's name, age, image, and visual indication for current selection.
     *
     * @param holder The ViewHolder which should be updated
     * @param position The position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull DogProfileViewHolder holder, int position) {
        DogProfile dog = dogList.get(position);
        Log.d(TAG, "Binding dog: " + dog.name + ", id: " + dog.dogId + ", isCurrent: " + dog.isCurrent);

        holder.dogName.setText(dog.name);

        String ageText = "Age: ";
        if (dog.age != null) {
            ageText += dog.age;
        } else {
            ageText += "Unknown";
        }
        holder.dogAge.setText(ageText);

        if (holder.dogBio != null) {
            holder.dogBio.setVisibility(View.GONE);
        }

        if (dog.profileImageUrl != null && !dog.profileImageUrl.isEmpty()) {
            Glide.with(context)
                    .load(dog.profileImageUrl)
                    .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                    .circleCrop()
                    .into(holder.dogImage);
        } else {
            holder.dogImage.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
        }

        if (dog.isCurrent) {
            holder.itemView.setBackgroundResource(R.drawable.selected_dog_background);
            Log.d(TAG, "Applying background to: " + dog.name);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Dog card clicked: " + dog.name);
            if (onDogClickListener != null) {
                onDogClickListener.onDogClick(position + baseIndex);
            } else {
                updateProfile(dog);
            }
        });
    }

    /**
     * Updates the profile information in SharedPreferences and UI when a dog is selected.
     * Saves dog details to SharedPreferences and updates the activity's UI components.
     *
     * @param dog The DogProfile to update the profile with
     */
    private void updateProfile(DogProfile dog) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("name", dog.name);
        editor.putString("age", dog.age != null ? dog.age : "");
        editor.putString("race", dog.race);
        editor.putString("birthday", dog.birthday);
        editor.putString("weight", dog.weight);
        editor.putString("allergies", dog.allergies);
        editor.putString("vaccines", dog.vaccines);

        if (dog.dogId != null) {
            editor.putString("dogId", dog.dogId);
        } else {
            editor.putString("currentDogName", dog.name);
        }

        StringBuilder bioBuilder = new StringBuilder();

        if (dog.weight != null && !dog.weight.isEmpty()) {
            bioBuilder.append("Weight: ").append(dog.weight).append(" kg\n");
        }

        if (dog.race != null && !dog.race.isEmpty()) {
            bioBuilder.append("Race: ").append(dog.race).append("\n");
        }

        if (dog.allergies != null && !dog.allergies.isEmpty()) {
            bioBuilder.append("Allergies: ").append(dog.allergies).append("\n");
        }

        if (dog.vaccines != null && !dog.vaccines.isEmpty()) {
            bioBuilder.append("Vaccines: ").append(dog.vaccines);
        }

        editor.putString("bio", bioBuilder.toString().trim());

        if (dog.profileImageUrl != null && !dog.profileImageUrl.isEmpty()) {
            editor.putString("profileImageUrl", dog.profileImageUrl);
        }

        editor.apply();

        if (context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;

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

        setCurrentDog(dog);
    }

    /**
     * Updates the dog list with new data and refreshes the RecyclerView.
     *
     * @param newList The new list of DogProfile objects to display
     */
    public void updateDogList(List<DogProfile> newList) {
        dogList.clear();
        dogList.addAll(newList);
        notifyDataSetChanged();
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of dog profiles
     */
    @Override
    public int getItemCount() {
        return dogList.size();
    }
}