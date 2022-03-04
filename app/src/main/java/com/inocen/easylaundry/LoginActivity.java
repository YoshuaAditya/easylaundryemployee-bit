package com.inocen.easylaundry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    String url = "https://easylaundry-2c69e.firebaseapp.com/api/login";
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private FirebaseAuth mAuth;
    private EditText editTextEmail, editTextPassword;
    private final String TAG = "Login";
    private Button login;
    String token;
    ArrayList<String> listKota=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        loginFunction();
//        createAccount();

    }

    private void loginFunctionWithAPI() {
        //first getting the values
        final String email = editTextEmail.getText().toString();
        final String password = editTextPassword.getText().toString();

        //validating inputs
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Please enter your username");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Please enter your password");
            editTextPassword.requestFocus();
            return;
        }

        login.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password);
        //if everything is fine
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
//                        progressBar.setVisibility(View.GONE);
                        login.setEnabled(true);
                        try {
                            //converting response to json object
                            JSONObject obj = new JSONObject(response);
                            User user=new User(
                                    obj.getString("username"),
                                    obj.getString("kota"),
                                    obj.getString("posisi"),
                                    obj.getString("kode"),
                                    obj.getString("email"),
                                    obj.getString("uid")//TODO sementara param password khusus in tak isi uid
                                    );

                            checkUserTypeAPI(user);

                        } catch (JSONException e) {
                            Toast.makeText(LoginActivity.this, "Email/Password tidak cocok", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(LoginActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        login.setEnabled(true);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                params.put("password", password);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(stringRequest);
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
                    Toast.makeText(LoginActivity.this, "???",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkUserTypeAPI(User user) {
        editor = sharedPref.edit();
        editor.putString(getString(R.string.userid_preference), user.password);
        editor.apply();
        if (user.password.equals(getString(R.string.adminid))) loginIntoAdmin();
        else {
//            Intent i = EmployeeActivity.createActivity(LoginActivity.this, user.username, user.posisi, user.kode);
//            startActivity(i);
//            finish();
        }
    }

    private void loginIntoAdmin() {
        getKotaOutletInfo();
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
                                     EmployeeActivity.nama = dataSnapshot.child("username").getValue().toString();
                                     EmployeeActivity.posisi = dataSnapshot.child("posisi").getValue().toString();
                                     EmployeeActivity.kode = dataSnapshot.child("kode").getValue().toString();

                                     startActivity(new Intent(LoginActivity.this, EmployeeActivity.class));
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


    /**
     * Login using firebase
     * success go to main
     * failed show message
     */
    public void loginFunction() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);

        login = findViewById(R.id.buttonMasuk);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                loginFunctionWithAPI();
//
                login.setEnabled(false);
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, getString(R.string.fill_data),
                            Toast.LENGTH_SHORT).show();
                    login.setEnabled(true);
                }
                else {
                    findViewById(R.id.loading_circle_bar_login).setVisibility(View.VISIBLE);
                    Toast.makeText(LoginActivity.this, "Login..." ,
                            Toast.LENGTH_SHORT).show();
                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // Sign in success, update UI with the signed-in user's information
                                        FirebaseUser user = mAuth.getCurrentUser();
//                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                                        Toast.makeText(LoginActivity.this, "Login Success: " + user.getEmail(),
                                                Toast.LENGTH_SHORT).show();
                                        checkUserType(user);
                                    } else {
                                        // If sign in fails, display wordsArray message to the user.
                                        Toast.makeText(LoginActivity.this, task.getException().getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                        findViewById(R.id.loading_circle_bar_login).setVisibility(View.INVISIBLE);
                                        login.setEnabled(true);
                                    }
                                }
                            });
                }
            }
        });
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
                                     findViewById(R.id.loading_circle_bar_login).setVisibility(View.INVISIBLE);
                                     Bundle bundle=new Bundle();
                                     bundle.putStringArrayList("list",listKota);
                                     Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
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

    public void createAccount() {
//        Button create = findViewById(R.id.buttonDaftarBaru);
//        create.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
//                finish();
//
//            }
//        });
    }

}
