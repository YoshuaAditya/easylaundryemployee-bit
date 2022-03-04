package com.inocen.easylaundry;

public class Note {

    public String status;

    public Note() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Note(String status) {
        this.status = status;
    }
}
