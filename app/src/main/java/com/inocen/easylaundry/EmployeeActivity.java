package com.inocen.easylaundry;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class EmployeeActivity extends AppCompatActivity {

    String urlTambahNotaUser = "https://easylaundry-2c69e.firebaseapp.com/api/add-idnota";
    String url = "https://easylaundry-2c69e.firebaseapp.com/api/scan-employee";
    DateTimeFormatter tglHarian = DateTimeFormat.forPattern("yyyy-MMM-dd");
    DateTimeFormatter tglBulanan = DateTimeFormat.forPattern("yyyy-MMM");
    public static String nama = "";
    public static String posisi = "";
    public static String kode = "";
    public static String token = "";
    String notaId = "";
    RadioButton radioButtonPickup, radioButtonDelivery;
    private Activity activity;
    private final int MAX_RETRY = 10;
    private final int MAX_LENGTH = 30;
    StringRequest stringRequestScan, stringRequestNotaUser;
    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee);

        activity = this;
        requestQueue = Volley.newRequestQueue(activity);
        setUserData();
        setButtonClick();
    }

    private void setButtonClick() {
        Button buttonActivateCamera, buttonInputManual;
        buttonActivateCamera = findViewById(R.id.buttonActivateCamera);
        buttonActivateCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                new IntentIntegrator(MainActivity.this).initiateScan(); // `this` is the current Activity
                IntentIntegrator integrator = new IntentIntegrator(EmployeeActivity.this);
                integrator.setOrientationLocked(false);
                integrator.initiateScan();
            }
        });
        buttonInputManual = findViewById(R.id.buttonInputManual);
        buttonInputManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(EmployeeActivity.this);
                builder.setTitle(R.string.dialog_title);

                // Set up the input
                final EditText input = new EditText(EmployeeActivity.this);
                InputFilter[] FilterArray = new InputFilter[1];
                FilterArray[0] = new InputFilter.LengthFilter(MAX_LENGTH);
                input.setFilters(FilterArray);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        notaId = input.getText().toString();
                        createNewNotes();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });
    }

    private void setUserData() {
        final TextView textViewNama, textViewPosisi, textViewKode;

        String titleBar = getString(R.string.selamat_datang) + ", sdr. " + nama;
        getSupportActionBar().setTitle(titleBar);
        textViewNama = findViewById(R.id.textViewNama);
        textViewNama.setText(nama);
        textViewPosisi = findViewById(R.id.textViewPosisi);
        textViewPosisi.setText(posisi);
        textViewKode = findViewById(R.id.textViewKode);
        textViewKode.setText(kode);

        radioButtonDelivery = findViewById(R.id.radioDelivery);
        radioButtonPickup = findViewById(R.id.radioPickUp);

        switch (posisi) {
            case "Driver":
                break;
            default:
                radioButtonDelivery.setVisibility(View.INVISIBLE);
                radioButtonPickup.setVisibility(View.INVISIBLE);
                break;
        }
    }

    // Get the results:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                try {
                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                    ImageView imageViewQrCode = findViewById(R.id.imageView);
                    Bitmap bitmap = barcodeEncoder.encodeBitmap(result.getContents(), BarcodeFormat.QR_CODE, 250, 250);
                    imageViewQrCode.setImageBitmap(bitmap);
                    notaId = result.getContents();
                    createNewNotes();
                } catch (Exception e) {

                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.drawermenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.harian:
                showPopupRapor(1);
                return true;
            case R.id.bulanan:
                showPopupRapor(2);
                return true;
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(EmployeeActivity.this, LoginActivity.class));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createNewNotes() {
        activity.findViewById(R.id.loading_circle_bar).setVisibility(View.VISIBLE);
        //if everything is fine
        stringRequestScan = new StringRequest(Request.Method.PUT, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
//                        progressBar.setVisibility(View.GONE);
                        try {
                            //converting response to json object
                            JSONObject obj = new JSONObject(response);
                            Toast.makeText(EmployeeActivity.this, obj.getString("message"), Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Toast.makeText(EmployeeActivity.this, "nota id tidak ditemukan", Toast.LENGTH_SHORT).show();
                            activity.findViewById(R.id.loading_circle_bar).setVisibility(View.INVISIBLE);
                        }
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                tambahNotaApi();
                            }
                        }, 500);
                        try {
                            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                            ImageView imageViewQrCode = findViewById(R.id.imageView);
                            Bitmap bitmap = barcodeEncoder.encodeBitmap(notaId, BarcodeFormat.QR_CODE, 250, 250);
                            imageViewQrCode.setImageBitmap(bitmap);
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestQueue.add(stringRequestScan)
                                .setRetryPolicy(new DefaultRetryPolicy(3000, MAX_RETRY, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                String status = "";
                if (radioButtonPickup.isChecked()) status = "pickup";
                else status = "delivery";
                DateTime temp = new DateTime();
                DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
                temp = temp.plusHours(7);
                String sekarang = temp.toString(dateTimeFormatter);
                Map<String, String> params = new HashMap<>();
                params.put("token", token);
                params.put("nota", notaId);
                params.put("username", nama);
                params.put("employee", kode);
                params.put("posisi", posisi);
                params.put("waktu", sekarang);
                if (posisi.equals("Driver")) params.put("tugas", status);
                return params;
            }
        };
        requestQueue.add(stringRequestScan)
                .setRetryPolicy(new DefaultRetryPolicy(2000, MAX_RETRY, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//        String status="";
//
//        switch (posisi){
//            case "Driver":
//                if(radioButtonPickup.isChecked())status="pickup";
//                else status="delivery";
//                break;
//            case "Washer":
//                status = "wash";
//                break;
//            case "Setrika":
//                status = "setrika";
//                break;
//            case "Packaging":
//                status = "packaging";
//                break;
//        }
//        final Note note=new Note(status);
//        final DatabaseReference databaseReference=mDatabase.child("notes").child(noteId);
//        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    databaseReference.setValue(note);
//                    Toast.makeText(EmployeeActivity.this, "Success",
//                            Toast.LENGTH_SHORT).show();
//                }
//                else
//                    Toast.makeText(EmployeeActivity.this, "id nota tidak ditemukan, pastikan sudah benar",
//                            Toast.LENGTH_LONG).show();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(EmployeeActivity.this, databaseError.getMessage(),
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    private void tambahNotaApi() {
        stringRequestNotaUser = new StringRequest(Request.Method.POST, urlTambahNotaUser,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JSONObject obj = null;
                        try {
                            obj = new JSONObject(response);
                            Toast.makeText(EmployeeActivity.this, obj.getString("message"), Toast.LENGTH_SHORT).show();
                            //TODO nambah request ini jadi kebuat activity baru?? aku udh cari tapi sama aja
                            activity.findViewById(R.id.loading_circle_bar).setVisibility(View.INVISIBLE);
//                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestQueue.add(stringRequestNotaUser)
                                .setRetryPolicy(new DefaultRetryPolicy(2000, MAX_RETRY, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("token", token);
                if (posisi.equals("Driver")) {
                    if (radioButtonPickup.isChecked())
                        params.put("idNota", notaId + "PICKUP");
                    else
                        params.put("idNota", notaId + "DELIVER");
                } else
                    params.put("idNota", notaId);
                return params;
            }
        };
        requestQueue.add(stringRequestNotaUser)
                .setRetryPolicy(new DefaultRetryPolicy(2000, MAX_RETRY, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    private void showPopupRapor(int i) {
        activity.findViewById(R.id.loading_circle_bar).setVisibility(View.VISIBLE);
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        DateTime dateTime = new DateTime();
        dateTime = dateTime.plusHours(7);
        String waktu = "", by = "", rapor = "", jumlah = "";
        switch (i) {
            case 1://harian
                waktu = dateTime.toString(tglHarian);
                by = "byDay";
                rapor = "Rapor Harian";
                jumlah = "Jumlah order hari ini: ";
                break;
            case 2://bulanan
                waktu = dateTime.toString(tglBulanan);
                by = "byMonth";
                rapor = "Rapor Bulanan";
                jumlah = "Jumlah order bulan ini: ";
                break;
        }

        final Query query = database.getReference("users/" + firebaseUser.getUid() + "/kodeNota")
                .orderByChild(by).equalTo(waktu);
        final String finalRapor = rapor;
        final String finalJumlah = jumlah;
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(finalRapor);

                // Set up the input
                final TextView input = new TextView(activity);
                String text = finalJumlah + dataSnapshot.getChildrenCount();

                input.setGravity(Gravity.CENTER);
                input.setText(text);
                input.setTypeface(ResourcesCompat.getFont(activity, R.font.lato_medium));
                input.setTextSize(20);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                builder.show();
                activity.findViewById(R.id.loading_circle_bar).setVisibility(View.INVISIBLE);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        requestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                // do I have to cancel this?
                return true; // -> always yes
            }
        });
    }
}
