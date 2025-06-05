package com.example.vetcalls.obj;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vetcalls.R;

/**
 * ViewHolder class for dog profile items in the RecyclerView.
 * Holds references to the UI components of each dog profile item
 * to enable efficient data binding and view recycling.
 *
 * @author Ofek Levi
 */
public class DogProfileViewHolder extends RecyclerView.ViewHolder {

    /** ImageView for displaying the dog's profile image */
    ImageView dogImage;

    /** TextView for displaying the dog's name */
    TextView dogName;

    /** TextView for displaying the dog's age */
    TextView dogAge;

    /** TextView for displaying the dog's biography */
    TextView dogBio;

    private static final String VIEW_HOLDER_TAG = "DogViewHolder";

    /**
     * Constructor that initializes the ViewHolder with the given item view.
     * Finds and stores references to the UI components within the item layout.
     * Logs the initialization status for debugging purposes.
     *
     * @param itemView The root view of the dog profile item layout
     */
    public DogProfileViewHolder(@NonNull View itemView) {
        super(itemView);
        dogImage = itemView.findViewById(R.id.dogImage);
        dogName = itemView.findViewById(R.id.dogName);
        dogAge = itemView.findViewById(R.id.dogAge);
        dogBio = itemView.findViewById(R.id.dogBio);

        Log.d(VIEW_HOLDER_TAG, "DogViewHolder initialized. Views found: " +
                "dogImage=" + (dogImage != null) +
                ", dogName=" + (dogName != null) +
                ", dogAge=" + (dogAge != null) +
                ", dogBio=" + (dogBio != null));
    }
}