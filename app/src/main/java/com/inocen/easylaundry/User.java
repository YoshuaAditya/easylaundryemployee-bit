package com.inocen.easylaundry;

public class User {

    public String username;
    public String kota;
    public String posisi;
    public String kode;
    public String email;
    public String password;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String kota, String posisi, String kode, String email, String password) {
        this.username = username;
        this.kota = kota;
        this.posisi = posisi;
        this.kode = kode;
        this.email = email;
        this.password= password;
    }

}