package com.example.vetcalls.obj;

public class VetItem {
    private String id, name;
    public VetItem(String id, String name) { this.id = id; this.name = name; }
    public String getId() { return id; }
    public String getName() { return name; }
    @Override public String toString() { return name; }
} 