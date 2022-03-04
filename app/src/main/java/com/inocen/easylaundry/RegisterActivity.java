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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    String url = "https://easylaundry-2c69e.firebaseapp.com/api/register";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private final String TAG = "Register";
    private String tempKota="", tempPosisi="", kode;
    private EditText editTextUsername, editTextEmail, editTextPasswordConfirmation, editTextPassword;
    private TextView textViewGenerateKode;
    private Spinner spinnerPosisi, spinnerKota;
    ArrayList<String> listKota = new ArrayList<>();
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Bundle bundle = getIntent().getExtras().getBundle("bundle");
        listKota = bundle.getStringArrayList("list");

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        registerFunction();
    }

    /**
     * Register using firebase
     * success go to MenuActivity
     * failed show toast
     */
    public void registerFunction() {
        editTextUsername = findViewById(R.id.editTextNama);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPasswordConfirmation = findViewById(R.id.editTextKonfirmasiPassword);
        spinnerKota = findViewById(R.id.spinnerKota);
        ArrayAdapter<String> adapterKota = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listKota);
        adapterKota.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKota.setAdapter(adapterKota);
        spinnerKota.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerPosisi = findViewById(R.id.spinnerPosisi);
        ArrayAdapter<CharSequence> adapterPosisi = ArrayAdapter.createFromResource(this,
                R.array.posisi_array, android.R.layout.simple_spinner_item);
        adapterPosisi.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPosisi.setAdapter(adapterPosisi);
        spinnerPosisi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button buatAkun = findViewById(R.id.buttonBuatAkun);
        buatAkun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                createUserInDatabase();
                createUserInDatabaseAPI();
            }
        });
        Button buttonLihatAkun = findViewById(R.id.buttonLihatDaftarAkun);
        buttonLihatAkun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor = sharedPref.edit();
                editor.remove(getString(R.string.userid_preference));
                editor.apply();

                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(RegisterActivity.this,LoginActivity.class));
                finish();
            }
        });
    }

    private void createUserInDatabaseAPI() {
        String username = editTextUsername.getText().toString().trim();
        String kota = spinnerKota.getSelectedItem().toString();
        String posisi = spinnerPosisi.getSelectedItem().toString();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String passwordConfirmation = editTextPasswordConfirmation.getText().toString().trim();

        //creating user in firebase database, checking empty values
        if (username.isEmpty() ||email.isEmpty() || password.isEmpty() || kota.isEmpty() || posisi.isEmpty())
            Toast.makeText(RegisterActivity.this, getString(R.string.fill_data),
                    Toast.LENGTH_SHORT).show();
        else if (password.length()<6)
            Toast.makeText(RegisterActivity.this, "password minimal 6 huruf/angka",
                    Toast.LENGTH_SHORT).show();
        else if (password.equals(passwordConfirmation)) {
            //if everything is fine
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
//                        progressBar.setVisibility(View.GONE);

                            JSONObject obj = null;
                            try {
                                obj = new JSONObject(response);
                                Toast.makeText(RegisterActivity.this, obj.getString("message"),
                                        Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("email", editTextEmail.getText().toString().trim());
                    params.put("kota", spinnerKota.getSelectedItem().toString());
                    params.put("password", editTextPassword.getText().toString().trim());
                    params.put("posisi", spinnerPosisi.getSelectedItem().toString());
                    params.put("username", editTextUsername.getText().toString().trim());
                    return params;
                }
            };

            Volley.newRequestQueue(this).add(stringRequest);
        }else Toast.makeText(RegisterActivity.this, getString(R.string.password_mismatch),
                Toast.LENGTH_SHORT).show();
    }

    private void createUserInDatabase() {
        String username = editTextUsername.getText().toString().trim();
        String kota = spinnerKota.getSelectedItem().toString();
        String posisi = spinnerPosisi.getSelectedItem().toString();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String passwordConfirmation = editTextPasswordConfirmation.getText().toString().trim();

        final User user = new User(username, kota, posisi, kode, email,password);

        //creating user in firebase database, checking empty values
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || kota.isEmpty() || posisi.isEmpty())
            Toast.makeText(RegisterActivity.this, getString(R.string.fill_data),
                    Toast.LENGTH_SHORT).show();
        else if (password.equals(passwordConfirmation)) {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(RegisterActivity.this, "Register Success",
                                        Toast.LENGTH_SHORT).show();
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();

                                createNewUser(task.getResult().getUser(), user);

//                                //TODO kemana ini?
//                                startActivity(new Intent(RegisterActivity.this, EnterCodeActivity.class));
//                                finish();
                            } else {
                                Log.e(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(RegisterActivity.this, task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else Toast.makeText(RegisterActivity.this, getString(R.string.password_mismatch),
                Toast.LENGTH_SHORT).show();
    }


    /**
     * @param firebaseUser
     * @param user         add user to database for additional values like phone number and age
     */
    private void createNewUser(FirebaseUser firebaseUser, User user) {
        String userId = firebaseUser.getUid();
        mDatabase.child("users").child(userId).setValue(user);
    }
}
