import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ViewHolder class for chat preview items in the RecyclerView.
 * Holds references to the UI components of each chat preview item
 * to enable efficient data binding and view recycling.
 *
 * @author Ofek Levi
 */
public class ChatViewHolder extends RecyclerView.ViewHolder {

    /** ImageView for displaying the chat participant's profile image */
    public ImageView image;

    /** TextView for displaying the chat participant's name */
    public TextView name;

    /**
     * Constructor that initializes the ViewHolder with the given item view.
     * Finds and stores references to the UI components within the item layout.
     *
     * @param itemView The root view of the chat preview item layout
     */
    public ChatViewHolder(@NonNull View itemView) {
        super(itemView);
        image = itemView.findViewById(R.id.imageProfile);
        name = itemView.findViewById(R.id.textName);
    }
}