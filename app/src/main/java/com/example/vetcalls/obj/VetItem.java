package com.example.vetcalls.obj;

/**
 * Simple model class representing a veterinarian item for display purposes.
 * Contains basic identification information including ID and name.
 * Commonly used in lists, spinners, and selection components.
 *
 * @author Ofek Levi
 */
public class VetItem {

    /** Unique identifier for the veterinarian */
    private String id;

    /** Display name of the veterinarian */
    private String name;

    /**
     * Constructor for creating a veterinarian item with ID and name.
     *
     * @param id Unique identifier for the veterinarian
     * @param name Display name of the veterinarian
     */
    public VetItem(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the unique identifier of the veterinarian.
     *
     * @return The veterinarian's unique identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the display name of the veterinarian.
     *
     * @return The veterinarian's display name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the string representation of this VetItem.
     * Used for display in UI components like spinners and lists.
     *
     * @return The veterinarian's name as the string representation
     */
    @Override
    public String toString() {
        return name;
    }
}