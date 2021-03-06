package io.intelehealth.client.activities.patient_detail_activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.intelehealth.client.R;
import io.intelehealth.client.activities.complaint_node_activity.ComplaintNodeActivity;
import io.intelehealth.client.activities.home_activity.HomeActivity;
import io.intelehealth.client.activities.identification_activity.IdentificationActivity;
import io.intelehealth.client.activities.visit_summary_activity.VisitSummaryActivity;
import io.intelehealth.client.activities.vitals_activity.VitalsActivity;
import io.intelehealth.client.database.LocalRecordsDatabaseHelper;
import io.intelehealth.client.node.Node;
import io.intelehealth.client.objects.Obs;
import io.intelehealth.client.objects.Patient;
import io.intelehealth.client.services.ClientService;
import io.intelehealth.client.services.PatientUpdateService;
import io.intelehealth.client.utilities.ConceptId;
import io.intelehealth.client.utilities.HelperMethods;

/**
 * This class displays all details about the patient.It also enables to print these details.
 * It creates a summary list of older visits of patient.
 */
public class PatientDetailActivity extends AppCompatActivity {

    LocalRecordsDatabaseHelper mDbHelper;
    private WebView mWebView;

    String LOG_TAG = "Patient Detail Activity";

    Integer patientID;
    String patientName;
    String visitID;
    String intentTag = "";
    Patient patient = new Patient();
    public static String previsitValue;
    Button editbtn;
    Button newVisit;
    LinearLayout previousVisitsList;
    SharedPreferences.Editor e;
    SharedPreferences sharedPreferences;
    boolean returning = false;
    String phistory = "";
    String fhistory = "";
    Obs complaint = new Obs();
     String visitValue;
    boolean hasLicense = false;
    String mFileName="config.json";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        Intent intent = this.getIntent(); // The intent was passed to the activity
        if (intent != null) {
            patientID = intent.getIntExtra("patientID", -1);
            patientName = intent.getStringExtra("name");
            intentTag = intent.getStringExtra("tag");
            Log.v(LOG_TAG, "Patient ID: " + patientID);
//            Log.v(TAG, "Patient Name: " + patientName);
//            Log.v(TAG, "Intent Tag: " + intentTag);
        }

        newVisit = (Button) findViewById(R.id.button_new_visit);

        patient.setId(patientID);
        setDisplay(String.valueOf(patientID));
        editbtn = (Button) findViewById(R.id.edit_button);
        editbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = new Intent(PatientDetailActivity.this, IdentificationActivity.class);
                intent2.putExtra("pid", patientID);
                startActivity(intent2);

            }
        });
        if (intentTag != null && intentTag.equals("new")) {
            Intent serviceIntent = new Intent(this, ClientService.class);
            serviceIntent.putExtra("serviceCall", "patient");
            serviceIntent.putExtra("patientID", patientID);
            serviceIntent.putExtra("name", patientName);
            startService(serviceIntent);
        }

        if (intentTag != null && intentTag.equals("edit")) {
            Intent serviceIntentUpdate = new Intent(this, PatientUpdateService.class);
            serviceIntentUpdate.putExtra("serviceCall", "patientUpdate");
            serviceIntentUpdate.putExtra("patientID", patientID);
            serviceIntentUpdate.putExtra("name", patientName);
            startService(serviceIntentUpdate);
        }


        newVisit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // before starting, we determine if it is new visit for a returning patient
                // extract both FH and PMH

                LocalRecordsDatabaseHelper mDatabaseHelper = new LocalRecordsDatabaseHelper(PatientDetailActivity.this);
                SQLiteDatabase sqLiteDatabase = mDatabaseHelper.getReadableDatabase();
                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String CREATOR_ID = sharedPreferences.getString("creatorid", null);
                e = sharedPreferences.edit();
                returning = false;
                e.putBoolean("returning", returning); // change in Sp
                e.commit();

                String[] cols = {"value"};
                Cursor cursor = sqLiteDatabase.query("obs", cols, "patient_id=? and concept_id=?",// querying for PMH
                        new String[]{String.valueOf(patient.getId()), String.valueOf(ConceptId.RHK_MEDICAL_HISTORY_BLURB)},
                        null, null, null);

                if (cursor.moveToFirst()) {
                    // rows present
                    do {
                        // so that null data is not appended
                        phistory = phistory + cursor.getString(0);

                    }
                    while (cursor.moveToNext());
                    returning = true;
                    e.putBoolean("returning", true);
                    e.commit();
                }
                cursor.close();

                Cursor cursor1 = sqLiteDatabase.query("obs", cols, "patient_id=? and concept_id=?",// querying for FH
                        new String[]{String.valueOf(patient.getId()), String.valueOf(ConceptId.RHK_FAMILY_HISTORY_BLURB)},
                        null, null, null);
                if (cursor1.moveToFirst()) {
                    // rows present
                    do {
                        fhistory = fhistory + cursor1.getString(0);
                    }
                    while (cursor1.moveToNext());
                    returning = true;
                    e.putBoolean("returning", true);
                    e.commit();
                }
                cursor1.close();

                // Will display data for patient as it is present in database
                // Toast.makeText(PatientDetailActivity.this,"PMH: "+phistory,Toast.LENGTH_SHORT).sƒhow();
                // Toast.makeText(PatientDetailActivity.this,"FH: "+fhistory,Toast.LENGTH_SHORT).show();

                Intent intent2 = new Intent(PatientDetailActivity.this, VitalsActivity.class);
                String fullName = patient.getFirstName() + " " + patient.getLastName();
                intent2.putExtra("patientID", patientID);

                SimpleDateFormat currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
                Date todayDate = new Date();
                String thisDate = currentDate.format(todayDate);

                ContentValues visitData = new ContentValues();
                visitData.put("patient_id", patient.getId());
                Log.i(LOG_TAG, "onClick: " + thisDate);
                visitData.put("start_datetime", thisDate);
                visitData.put("visit_type_id", 0);
                visitData.put("visit_location_id", 0);
                visitData.put("visit_creator", CREATOR_ID);

                LocalRecordsDatabaseHelper mDbHelper = new LocalRecordsDatabaseHelper(PatientDetailActivity.this);
                SQLiteDatabase localdb = mDbHelper.getWritableDatabase();
                Long visitLong = localdb.insert(
                        "visit",
                        null,
                        visitData
                );

                visitID = String.valueOf(visitLong);
                localdb.close();
                intent2.putExtra("visitID", visitID);
                intent2.putExtra("name", fullName);
                intent2.putExtra("tag", "new");
                startActivity(intent2);
            }
        });

        final CardView patientSummary = (CardView) findViewById(R.id.cardView_patDetail_summary);
        patientSummary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.detail_home:
                Intent intent = new Intent(PatientDetailActivity.this, HomeActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This method displays basic details about patient (eg: name, address).
     *
     * @param dataString variable of type String
     * @return void
     */
    public void setDisplay(String dataString) {
        LocalRecordsDatabaseHelper mDbHelper = new LocalRecordsDatabaseHelper(this.getApplicationContext());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        String patientSelection = "_id = ?";
        String[] patientArgs = {dataString};
        String[] patientColumns = {"openmrs_id", "first_name", "middle_name", "last_name",
                "date_of_birth", "address1", "address2", "city_village", "state_province",
                "postal_code", "country", "phone_number", "gender", "sdw", "occupation",
                "patient_photo", "economic_status", "education_status", "caste"};
        final Cursor idCursor = db.query("patient", patientColumns, patientSelection, patientArgs, null, null, null);

        if (idCursor.moveToFirst()) {
            do {
                patient.setOpenmrs_patient_id(idCursor.getString(idCursor.getColumnIndexOrThrow("openmrs_id")));
                patient.setFirstName(idCursor.getString(idCursor.getColumnIndexOrThrow("first_name")));
                patient.setMiddleName(idCursor.getString(idCursor.getColumnIndexOrThrow("middle_name")));
                patient.setLastName(idCursor.getString(idCursor.getColumnIndexOrThrow("last_name")));
                patient.setDateOfBirth(idCursor.getString(idCursor.getColumnIndexOrThrow("date_of_birth")));
                patient.setAddress1(idCursor.getString(idCursor.getColumnIndexOrThrow("address1")));
                patient.setAddress2(idCursor.getString(idCursor.getColumnIndexOrThrow("address2")));
                patient.setCityVillage(idCursor.getString(idCursor.getColumnIndexOrThrow("city_village")));
                patient.setStateProvince(idCursor.getString(idCursor.getColumnIndexOrThrow("state_province")));
                patient.setPostalCode(idCursor.getString(idCursor.getColumnIndexOrThrow("postal_code")));
                patient.setCountry(idCursor.getString(idCursor.getColumnIndexOrThrow("country")));
                patient.setPhoneNumber(idCursor.getString(idCursor.getColumnIndexOrThrow("phone_number")));
                patient.setGender(idCursor.getString(idCursor.getColumnIndexOrThrow("gender")));
                patient.setSdw(idCursor.getString(idCursor.getColumnIndexOrThrow("sdw")));
                patient.setOccupation(idCursor.getString(idCursor.getColumnIndexOrThrow("occupation")));
                patient.setPatientPhoto(idCursor.getString(idCursor.getColumnIndexOrThrow("patient_photo")));
                patient.setEconomic_status(idCursor.getString(idCursor.getColumnIndexOrThrow("economic_status")));
                patient.setEducation_level(idCursor.getString(idCursor.getColumnIndexOrThrow("education_status")));
                patient.setCaste(idCursor.getString(idCursor.getColumnIndexOrThrow("caste")));
            } while (idCursor.moveToNext());
        }
        idCursor.close();

        ImageView photoView = (ImageView) findViewById(R.id.imageView_patient);

        TextView idView = (TextView) findViewById(R.id.textView_ID);
        TextView dobView = (TextView) findViewById(R.id.textView_DOB);
        TextView ageView = (TextView) findViewById(R.id.textView_age);
        TextView addr1View = (TextView) findViewById(R.id.textView_address_1);
        TableRow addr2Row = (TableRow) findViewById(R.id.tableRow_addr2);
        TextView addr2View = (TextView) findViewById(R.id.textView_address2);
        TextView addrFinalView = (TextView) findViewById(R.id.textView_address_final);
        TextView casteView = (TextView) findViewById(R.id.textView_caste);
        TextView economic_statusView = (TextView) findViewById(R.id.textView_economic_status);
        TextView education_statusView = (TextView) findViewById(R.id.textView_education_status);
        TextView phoneView = (TextView) findViewById(R.id.textView_phone);
        TextView sdwView = (TextView) findViewById(R.id.textView_SDW);
        TableRow sdwRow = (TableRow) findViewById(R.id.tableRow_SDW);
        TextView occuView = (TextView) findViewById(R.id.textView_occupation);
        TableRow occuRow = (TableRow) findViewById(R.id.tableRow_Occupation);
        TableRow economicRow = (TableRow) findViewById(R.id.tableRow_Economic_Status);
        TableRow educationRow = (TableRow) findViewById(R.id.tableRow_Education_Status);
        TableRow casteRow = (TableRow) findViewById(R.id.tableRow_Caste);

        TextView medHistView = (TextView) findViewById(R.id.textView_patHist);
        TextView famHistView = (TextView) findViewById(R.id.textView_famHist);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (sharedPreferences.contains("licensekey"))
            hasLicense = true;

        //Check for license key and load the correct config file
        try {
            JSONObject obj = null;
            if (hasLicense) {
                obj = new JSONObject(HelperMethods.readFileRoot(mFileName, this)); //Load the config file

            }else {
                obj = new JSONObject(String.valueOf(HelperMethods.encodeJSON(this, mFileName)));
            }
                if (obj.getBoolean("casteRow")) {
                    casteRow.setVisibility(View.VISIBLE);
                } else {
                    casteRow.setVisibility(View.GONE);
                }
                if (obj.getBoolean("educationRow")) {
                    educationRow.setVisibility(View.VISIBLE);
                } else {
                    educationRow.setVisibility(View.GONE);
                }
                if (obj.getBoolean("economicRow")) {
                    economicRow.setVisibility(View.VISIBLE);
                } else {
                    economicRow.setVisibility(View.GONE);
                }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (patient.getMiddleName() == null) {
            patientName = patient.getLastName() + ", " + patient.getFirstName();
        } else {
            patientName = patient.getLastName() + ", " + patient.getFirstName() + " " + patient.getMiddleName();
        }
        setTitle(patientName);

        if (patient.getPatientPhoto() != null && patient.getPatientPhoto() != "") {
            File image = new File(patient.getPatientPhoto());
            Glide.with(this)
                    .load(image)
                    .thumbnail(0.3f)
                    .centerCrop()
                    .into(photoView);
        }

        if (patient.getOpenmrs_patient_id() != null && !patient.getOpenmrs_patient_id().isEmpty()) {
            idView.setText(patient.getOpenmrs_patient_id());
        } else {
            idView.setText(getString(R.string.patient_not_registered));
        }

        int age = HelperMethods.getAge(patient.getDateOfBirth());
        ageView.setText(String.valueOf(age));

        String dob = HelperMethods.SimpleDatetoLongDate(patient.getDateOfBirth());
        dobView.setText(dob);
        if (patient.getAddress1() == null || patient.getAddress2().equals("")) {
            addr1View.setVisibility(View.GONE);
        } else {
            addr1View.setText(patient.getAddress1());
        }
        if (patient.getAddress2() == null || patient.getAddress2().equals("")) {
            addr2Row.setVisibility(View.GONE);
        } else {
            addr2View.setText(patient.getAddress2());
        }
        String city_village;
        if (patient.getCityVillage() != null) {
            city_village = patient.getCityVillage().trim();
        } else {
            city_village = "";
        }

        String postal_code;
        if (patient.getPostalCode() != null) {
            postal_code = patient.getPostalCode().trim() + ",";
        } else {
            postal_code = "";
        }

        String addrFinalLine =
                String.format("%s, %s, %s %s",
                        city_village, patient.getStateProvince(),
                        postal_code, patient.getCountry());
        addrFinalView.setText(addrFinalLine);
        phoneView.setText(patient.getPhoneNumber());
        education_statusView.setText(patient.getEducation_level());
        economic_statusView.setText(patient.getEconomic_status());
        casteView.setText(patient.getCaste());

        if (patient.getSdw() != null && !patient.getSdw().equals("")) {
            sdwView.setText(patient.getSdw());
        } else {
            sdwRow.setVisibility(View.GONE);
        }

        if (patient.getOccupation() != null && !patient.getOccupation().equals("")) {
            occuView.setText(patient.getOccupation());
        } else {
            occuRow.setVisibility(View.GONE);
        }

        if (visitID != null) {
            CardView histCardView = (CardView) findViewById(R.id.cardView_history);
            histCardView.setVisibility(View.GONE);
        } else {
            String medHistSelection = "patient_id = ? AND concept_id = ?";
            String[] medHistArgs = {dataString, String.valueOf(ConceptId.RHK_MEDICAL_HISTORY_BLURB)};
            String[] medHistColumms = {"value", " concept_id"};
            Cursor medHistCursor = db.query("obs", medHistColumms, medHistSelection, medHistArgs, null, null, null);
            medHistCursor.moveToLast();

            String medHistValue;

            try {
                medHistValue = medHistCursor.getString(medHistCursor.getColumnIndexOrThrow("value"));
            } catch (Exception e) {
                medHistValue = "";
            } finally {
                medHistCursor.close();
            }

            if (medHistValue != null && !medHistValue.equals("")) {
                medHistView.setText(Html.fromHtml(medHistValue));
            } else {
                medHistView.setText(getString(R.string.string_no_hist));
            }


            String famHistSelection = "patient_id = ? AND concept_id = ?";
            String[] famHistArgs = {dataString, String.valueOf(ConceptId.RHK_FAMILY_HISTORY_BLURB)};
            String[] famHistColumns = {"value", " concept_id"};
            Cursor famHistCursor = db.query("obs", famHistColumns, famHistSelection, famHistArgs, null, null, null);
            famHistCursor.moveToLast();
            String famHistValue;

            try {
                famHistValue = famHistCursor.getString(famHistCursor.getColumnIndexOrThrow("value"));
            } catch (Exception e) {
                famHistValue = "";
            } finally {
                famHistCursor.close();
            }

            if (famHistValue != null && !famHistValue.equals("")) {
                famHistView.setText(Html.fromHtml(famHistValue));
            } else {
                famHistView.setText(getString(R.string.string_no_hist));
            }
        }

        String visitSelection = "patient_id = ?";
        String[] visitArgs = {dataString};
        String[] visitColumns = {"_id, start_datetime", "end_datetime"};
        String visitOrderBy = "_id";
        Cursor visitCursor = db.query("visit", visitColumns, visitSelection, visitArgs, null, null, visitOrderBy);
        previousVisitsList = (LinearLayout) findViewById(R.id.linearLayout_previous_visits);
        if (visitCursor.getCount() < 1) {
            neverSeen();
        } else {

            if (visitCursor.moveToLast() && visitCursor!=null) {
                do {
                    String date = visitCursor.getString(visitCursor.getColumnIndexOrThrow("start_datetime"));
                    String end_date = visitCursor.getString(visitCursor.getColumnIndexOrThrow("end_datetime"));
                    Integer visit_id = visitCursor.getInt(visitCursor.getColumnIndexOrThrow("_id"));

                    String previsitSelection = "visit_id = ? AND concept_id = ?";
                    String[] previsitArgs = {String.valueOf(visit_id), String.valueOf(ConceptId.CURRENT_COMPLAINT)};
                    String[] previsitColumms = {"value", " concept_id", "visit_id"};
                    Cursor previsitCursor = db.query("obs", previsitColumms, previsitSelection, previsitArgs, null, null, null);
                   if (previsitCursor.moveToLast() &&previsitCursor!=null ) {

                       String visitValue = previsitCursor.getString(previsitCursor.getColumnIndexOrThrow("value"));
                       if (visitValue != null && !visitValue.isEmpty()) {
                           String complaints[] = StringUtils.split(visitValue, Node.bullet_arrow);

                           visitValue = "";
                           String colon = ":";
                           if (complaints != null) {
                               for (String comp : complaints) {
                                   if (!comp.trim().isEmpty()) {
                                       visitValue = visitValue + Node.bullet_arrow + comp.substring(0, comp.indexOf(colon)) + "<br/>";
                                   }
                               }
                               if (!visitValue.isEmpty()) {
                                   visitValue = visitValue.substring(0, visitValue.length() - 2);
                                   visitValue = visitValue.replaceAll("<b>", "");
                                   visitValue = visitValue.replaceAll("</b>", "");
                               }
                               SimpleDateFormat currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                               try {

                                   Date formatted = currentDate.parse(date);
                                   String visitDate = currentDate.format(formatted);
                                   createOldVisit(visitDate, visit_id, end_date, visitValue);
                               } catch (ParseException e) {
                                   e.printStackTrace();
                               }
                           }
                       }
                       // Called when we select complaints but not select any sub node inside that complaint
                       else {
                           SimpleDateFormat currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                           try {

                               Date formatted = currentDate.parse(date);
                               String visitDate = currentDate.format(formatted);
                               createOldVisit(visitDate, visit_id, end_date, visitValue);
                           } catch (ParseException e) {
                               e.printStackTrace();
                           }
                       }
                   }
                   // Called when we close app on vitals screen and Didn't select any complaints
                   else {
                       SimpleDateFormat currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                       try {

                           Date formatted = currentDate.parse(date);
                           String visitDate = currentDate.format(formatted);
                           createOldVisit(visitDate, visit_id, end_date, visitValue);
                       } catch (ParseException e) {
                           e.printStackTrace();
                       }
                   }
                } while (visitCursor.moveToPrevious());
            }
        }
        visitCursor.close();
    }

    /**
     * This class updates patient details and image.
     */
    protected class UpdatePatientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Snackbar.make(findViewById(R.id.clayout_detail), "Refreshing patient data", Snackbar.LENGTH_LONG).show();
        }

        @Override
        public Void doInBackground(String... params) {
            String patientId = params[0];
            queryExtraInfo(patientId);
            loadImage(patientId);

            return null;
        }

        public void queryExtraInfo(String id) {
            // TODO: Connect to OpenMRS via the Middleware API and get more info on the Patient
        }

        public void loadImage(String id) {
            // TODO: Update the image with the picture of the patient as stored on the filesystem
        }

    }

    /**
     * This method retrieves details about patient's old visits.
     *
     * @param datetime variable of type String.
     * @param visit_id variable of type int.
     * @return void
     */
    private void createOldVisit(final String datetime, int visit_id, String end_datetime,String visitValue) throws ParseException {
        // final LayoutInflater inflater = PatientDetailActivity.this.getLayoutInflater();
        //  View convertView = inflater.inflate(R.layout.list_item_previous_visit, null);
        //  TextView textView = (TextView) convertView.findViewById(R.id.textView_visit_info);

        final Boolean past_visit;
        final TextView textView = new TextView(this);

        final String visitString = String.format("Seen on (%s)", HelperMethods.SimpleDatetoLongDate(datetime));
        if (end_datetime == null || end_datetime.isEmpty()) {
            // visit has not yet ended

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            for (int i = 1; i <= 2; i++) {
                if (i == 1 ) {
                    SpannableString spannableString = new SpannableString(visitString + " Active");
                    Object greenSpan = new BackgroundColorSpan(Color.GREEN);
                    Object underlineSpan = new UnderlineSpan();
                    spannableString.setSpan(greenSpan, spannableString.length() - 6, spannableString.length(), 0);
                    spannableString.setSpan(underlineSpan, 0, spannableString.length() - 7, 0);
                    textView.setText(spannableString);
                    layoutParams.setMargins(2, 2, 2, 2);
                    previousVisitsList.addView(textView);
                }
                //If patient come up with any complaints
                if (i == 2) {
                    TextView complaintxt1 = new TextView(this);
                    complaintxt1.setLayoutParams(layoutParams);
                    if (visitValue != null && !visitValue.equals("")) {
                        complaintxt1.setText(Html.fromHtml(visitValue));
                    } else {
                        Log.e("Check", "No complaint");
                    }
                    layoutParams.setMargins(2, 2, 2, 2);
                    previousVisitsList.addView(complaintxt1);
                }
            }
            past_visit = false;

            if (newVisit.isEnabled()) {
                newVisit.setEnabled(false);
            }
            if (newVisit.isClickable()) {
                newVisit.setClickable(false);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) newVisit.setBackgroundColor
                        (getColor(R.color.divider));
                else newVisit.setBackgroundColor(getResources().getColor(R.color.divider));
            }

        } else {
            // when visit has ended
            past_visit = true;
            for (int i = 1; i <= 2; i++) {
                if (i == 1) {
                    textView.setText(visitString);
                    textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    previousVisitsList.addView(textView);
                }
                //If patient has any past complaints
                if (i == 2) {
                    TextView complaintxt1 = new TextView(this);
                    if (visitValue != null && !visitValue.equals("")) {
                        complaintxt1.setText(Html.fromHtml(visitValue));
                    }
                    else {
                        Log.e("Check", "No complaint");
                    }
                    previousVisitsList.addView(complaintxt1);
                }
            }
        }

        textView.setTextSize(18);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, 0, 0, 0);
        textView.setLayoutParams(llp);
        textView.setTag(visit_id);

//        previousVisitsList.addView(textView);
       /* textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        Toast.makeText(PatientDetailActivity.this,"Touch Down",Toast.LENGTH_SHORT).show();
                        v.getParent().getParent().getParent()
                                .requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        Toast.makeText(PatientDetailActivity.this,"Touch Up",Toast.LENGTH_SHORT).show();
                        v.getParent().getParent()
                                .requestDisallowInterceptTouchEvent(false);

                        break;
                }
                return true;
            }
        });*/
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (Integer) v.getTag();
                Intent visitSummary = new Intent(PatientDetailActivity.this, VisitSummaryActivity.class);
                visitSummary.putExtra("visitID", String.valueOf(position));
                visitSummary.putExtra("patientID", patientID);
                visitSummary.putExtra("name", patientName);
                visitSummary.putExtra("tag", intentTag);
                visitSummary.putExtra("pastVisit", past_visit);
                startActivity(visitSummary);
            }
        });
        //previousVisitsList.addView(textView);
        //TODO: add on click listener to open the previous visit
    }

    /**
     * This method is called when patient has no prior visits.
     *
     * @return void
     */
    private void neverSeen() {
        final LayoutInflater inflater = PatientDetailActivity.this.getLayoutInflater();
        View convertView = inflater.inflate(R.layout.list_item_previous_visit, null);
        TextView textView = (TextView) convertView.findViewById(R.id.textView_visit_info);
        String visitString = "No prior visits.";
        textView.setText(visitString);
        previousVisitsList.addView(convertView);
    }

}

