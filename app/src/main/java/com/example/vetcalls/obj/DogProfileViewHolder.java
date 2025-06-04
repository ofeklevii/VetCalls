package com.example.vetcalls.obj;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vetcalls.R;

public class DogProfileViewHolder extends RecyclerView.ViewHolder {
    ImageView dogImage;
    TextView dogName, dogAge, dogBio;
    private static final String VIEW_HOLDER_TAG = "DogViewHolder";

    public DogProfileViewHolder(@NonNull View itemView) {
        super(itemView);
        // מציאת כל ה-Views
        dogImage = itemView.findViewById(R.id.dogImage);
        dogName = itemView.findViewById(R.id.dogName);
        dogAge = itemView.findViewById(R.id.dogAge);
        dogBio = itemView.findViewById(R.id.dogBio);

        // לוג לבדיקה
        Log.d(VIEW_HOLDER_TAG, "DogViewHolder initialized. Views found: " +
                "dogImage=" + (dogImage != null) +
                ", dogName=" + (dogName != null) +
                ", dogAge=" + (dogAge != null) +
                ", dogBio=" + (dogBio != null));
    }
}
