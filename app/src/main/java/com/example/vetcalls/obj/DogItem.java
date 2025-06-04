package com.example.vetcalls.obj;

/**
 * DogItem represents a simplified data container for dog information in UI components.
 * This class is primarily used in spinners, adapters, and other UI elements where
 * only basic dog identification (ID and name) is required for display and selection purposes.
 *
 * <p>This lightweight data model is optimized for:</p>
 * <ul>
 *   <li>Spinner adapters in forms and selection dialogs</li>
 *   <li>List adapters for simple dog selection interfaces</li>
 *   <li>Quick reference objects where full DogProfile data is unnecessary</li>
 *   <li>Performance-critical scenarios requiring minimal memory footprint</li>
 * </ul>
 *
 * <p>The class implements a custom toString() method that returns the dog's name,
 * making it suitable for direct use in Android UI components that rely on string
 * representation for display purposes.</p>
 *
 * @author Ofek Levi
 * @version 1.0
 * @since 1.0
 * @see DogProfile
 */
public class DogItem {
    private String id, name;

    /**
     * Constructs a new DogItem with the specified ID and name.
     *
     * @param id The unique identifier for the dog
     * @param name The display name of the dog
     */
    public DogItem(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the unique identifier of the dog.
     *
     * @return The dog's unique ID as a String
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the display name of the dog.
     *
     * @return The dog's name as a String
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the string representation of this DogItem.
     * This method returns the dog's name, making it suitable for direct use
     * in UI components such as spinners and list adapters.
     *
     * @return The dog's name as the string representation
     */
    @Override
    public String toString() {
        return name;
    }
}
