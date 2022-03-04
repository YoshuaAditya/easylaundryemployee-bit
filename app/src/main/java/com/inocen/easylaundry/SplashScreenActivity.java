package com.inocen.easylaundry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class SplashScreenActivity extends AppCompatActivity {

    String token;
    ArrayList<String> listKota=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        loginCheck();
//        loginCheckAPI();


    }

    private void loginCheckAPI() {
//        String uid=sharedPref.getString(getString(R.string.userid_preference),"null");
//        if(uid.equals("null"))startApp();
//        else checkUserTypeAPI(uid);

    }

    private void checkUserTypeAPI(String uid) {
        if (uid.equals(getString(R.string.adminid))) loginIntoAdmin();
        else {
//            loginIntoEmployee(uid);
        }
    }

    private void loginCheck() {
        /**
         * checks if user have login in before
         */
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // User is signed in
//            Toast.makeText(LoginActivity.this, getString(R.string.login_with) +" "+ user.getEmail(),
//                    Toast.LENGTH_SHORT).show();
            checkUserType(user);
        }
        else startApp();
    }

    public void startApp(){
        int secondsDelayed = 1;
        new Handler().postDelayed(new Runnable() {
            public void run() {
                startActivity(new Intent(SplashScreenActivity.this, LoginActivity.class));
//                startActivity(new Intent(WelcomeScreenActivity.this, WelcomeScreenActivity.class));
                finish();
            }
        }, secondsDelayed * 2000);
    }

    private void checkUserType(final FirebaseUser user) {
        user.getIdToken(false).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
            @Override
            public void onSuccess(GetTokenResult result) {
                boolean isUserEmployee=false;
                try {
                    isUserEmployee= (boolean)result.getClaims().get("userEmployee");
                }catch (NullPointerException e){

                }
                if (isUserEmployee) {
                    if (user.getUid().equals(getString(R.string.adminid))) loginIntoAdmin();
                    else loginIntoEmployee(user);
                } else {
                    Toast.makeText(SplashScreenActivity.this, "???",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loginIntoAdmin() {
        getKotaOutletInfo();
    }

    private void getKotaOutletInfo() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference("Kota/");
        databaseReference
                .addValueEventListener
                        (new ValueEventListener() {
                             @Override
                             public void onDataChange(final DataSnapshot dataSnapshot) {
                                 if (dataSnapshot.exists()) {
                                     for(DataSnapshot child:dataSnapshot.getChildren()){
                                         listKota.add(dataSnapshot.child(child.getKey()).child("nama").getValue().toString());
                                     }
                                     Bundle bundle=new Bundle();
                                     bundle.putStringArrayList("list",listKota);
                                     Intent i = new Intent(SplashScreenActivity.this, RegisterActivity.class);
                                     i.putExtra("bundle",bundle);
                                     startActivity(i);
                                     finish();
                                 }
                             }

                             @Override
                             public void onCancelled(DatabaseError databaseError) {
                                 findViewById(R.id.loading_circle_bar_login).setVisibility(View.INVISIBLE);
                             }
                         }
                        );
    }

    private void loginIntoEmployee(final FirebaseUser user) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference("users/" + user.getUid());
        databaseReference
                .addValueEventListener
                        (new ValueEventListener() {
                             @Override
                             public void onDataChange(DataSnapshot dataSnapshot) {
                                 if (dataSnapshot.exists()) {
                                     user.getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                                         @Override
                                         public void onSuccess(GetTokenResult result) {
                                             EmployeeActivity.token = result.getToken();
                                         }
                                     });;
                                     EmployeeActivity.nama= dataSnapshot.child("username").getValue().toString();
                                     EmployeeActivity.posisi= dataSnapshot.child("posisi").getValue().toString();
                                     EmployeeActivity.kode= dataSnapshot.child("kode").getValue().toString();

                                     startActivity(new Intent(SplashScreenActivity.this,EmployeeActivity.class));
                                     finish();
                                 }
                             }

                             @Override
                             public void onCancelled(DatabaseError databaseError) {
                                 System.out.println("The read failed: " + databaseError.getCode());
                             }
                         }
                        );
    }

//    private void loginIntoEmployee(String uid) {
//        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        DatabaseReference databaseReference = database.getReference("users/" + uid);
//        databaseReference
//                .addValueEventListener
//                        (new ValueEventListener() {
//                             @Override
//                             public void onDataChange(DataSnapshot dataSnapshot) {
//                                 if (dataSnapshot.exists()) {
//                                     String nama= dataSnapshot.child("username").getValue().toString();
//                                     String posisi= dataSnapshot.child("posisi").getValue().toString();
//                                     String kode= dataSnapshot.child("kode").getValue().toString();
//                                     Intent i=EmployeeActivity.createActivity(SplashScreenActivity.this,nama, posisi, kode);
//                                     startActivity(i);
//                                     finish();
//                                 }
//                             }
//
//                             @Override
//                             public void onCancelled(DatabaseError databaseError) {
//                                 System.out.println("The read failed: " + databaseError.getCode());
//                             }
//                         }
//                        );
//    }
}
