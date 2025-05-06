package com.example.vetcalls.obj;
import com.example.vetcalls.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.util.List;


public class DogProfileAdapter extends RecyclerView.Adapter<DogProfileAdapter.DogViewHolder> {

    private List<DogProfile> dogList;
    private Context context;

    public DogProfileAdapter(Context context, List<DogProfile> dogList) {
        this.context = context;
        this.dogList = dogList;
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
        holder.dogName.setText(dog.getName());
        holder.dogAge.setText("Age: " + dog.getAge());
        holder.dogBio.setText(dog.getBio());

        Glide.with(context)
                .load(dog.getImageUrl())
                .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                .circleCrop()
                .into(holder.dogImage);
    }

    @Override
    public int getItemCount() {
        return dogList.size();
    }

    public static class DogViewHolder extends RecyclerView.ViewHolder {
        ImageView dogImage;
        TextView dogName, dogAge, dogBio;

        public DogViewHolder(@NonNull View itemView) {
            super(itemView);
            dogImage = itemView.findViewById(R.id.dogImage);
            dogName = itemView.findViewById(R.id.dogName);
            dogAge = itemView.findViewById(R.id.dogAge);
            dogBio = itemView.findViewById(R.id.dogBio);
        }
    }
}
