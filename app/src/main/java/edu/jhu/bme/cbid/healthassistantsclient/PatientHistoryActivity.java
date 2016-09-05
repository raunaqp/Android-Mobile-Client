package edu.jhu.bme.cbid.healthassistantsclient;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ExpandableListView;
import edu.jhu.bme.cbid.healthassistantsclient.objects.Node;

public class PatientHistoryActivity extends AppCompatActivity {

    String LOG_TAG = "Patient History Activity";
    String patient = "patient";


    String patientID = "1";
    String visitID;
    String patientName;
    String intentTag;

    int lastExpandedPosition = -1;

    String mFileName = "patHist.json";
//    String mFileName = "patHistDemo.json";

    Node patientHistoryMap;
    CustomExpandableListAdapter adapter;
    ExpandableListView historyListView;

    String patientHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //For Testing
//        patientID = Long.valueOf("1");

        Intent intent = this.getIntent(); // The intent was passed to the activity
        if (intent != null) {
            patientID = intent.getStringExtra("patientID");
            visitID = intent.getStringExtra("visitID");
            patientName = intent.getStringExtra("name");
            intentTag = intent.getStringExtra("tag");
//            Log.v(LOG_TAG, "Patient ID: " + patientID);
//            Log.v(LOG_TAG, "Visit ID: " + visitID);
//            Log.v(LOG_TAG, "Patient Name: " + patientName);
//            Log.v(LOG_TAG, "Intent Tag: " + intentTag);
        }


        setTitle(R.string.title_activity_patient_history);
        setTitle(getTitle() + ": "  + patientName);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_history);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //If nothing is selected, there is nothing to put into the database.
                if(patientHistoryMap.anySubSelected()){
                    patientHistory = patientHistoryMap.generateLanguage();

                    insertDb(patientHistory);
                }


                if (intentTag.equals("edit")){
                    Intent intent = new Intent(PatientHistoryActivity.this, VisitSummaryActivity.class);
                    intent.putExtra("patientID", patientID);
                    intent.putExtra("visitID", visitID);
                    intent.putExtra("name", patientName);
                    intent.putExtra("tag", intentTag);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(PatientHistoryActivity.this, FamilyHistoryActivity.class);
                    intent.putExtra("patientID", patientID);
                    intent.putExtra("visitID", visitID);
                    intent.putExtra("name", patientName);
                    intent.putExtra("tag", intentTag);
                    startActivity(intent);
                }

            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        patientHistoryMap = new Node(HelperMethods.encodeJSON(this, mFileName)); //Load the patient history mind map
        historyListView = (ExpandableListView) findViewById(R.id.patient_history_expandable_list_view);
        adapter = new CustomExpandableListAdapter(this, patientHistoryMap, this.getClass().getSimpleName()); //The adapter might change depending on the activity.
        historyListView.setAdapter(adapter);

        historyListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Node clickedNode = patientHistoryMap.getOption(groupPosition).getOption(childPosition);
                clickedNode.toggleSelected();

                //Nodes and the expandable list act funny, so if anything is clicked, a lot of stuff needs to be updated.
                if (patientHistoryMap.getOption(groupPosition).anySubSelected()) {
                    patientHistoryMap.getOption(groupPosition).setSelected();
                } else {
                    patientHistoryMap.getOption(groupPosition).setUnselected();
                }
                adapter.notifyDataSetChanged();

                if(!clickedNode.isTerminal()){
                    HelperMethods.subLevelQuestion(clickedNode, PatientHistoryActivity.this, adapter);
                }

                return false;
            }
        });

        //Same fix as before, close all other groups when something is clicked.
        historyListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {

                if (lastExpandedPosition != -1
                        && groupPosition != lastExpandedPosition) {
                    historyListView.collapseGroup(lastExpandedPosition);
                }
                lastExpandedPosition = groupPosition;
            }
        });

    }


    private long insertDb(String value) {
        LocalRecordsDatabaseHelper mDbHelper = new LocalRecordsDatabaseHelper(this);

        final int CREATOR_ID = 42;
        //TODO: Get the right creator_ID


        final int CONCEPT_ID = 163187; // RHK MEDICAL HISTORY BLURB
        //Eventually will be stored in a separate table

        ContentValues complaintEntries = new ContentValues();

        complaintEntries.put("patient_id", patientID);
        complaintEntries.put("visit_id", visitID);
        complaintEntries.put("value", value);
        complaintEntries.put("concept_id", CONCEPT_ID);
        complaintEntries.put("creator", CREATOR_ID);

        SQLiteDatabase localdb = mDbHelper.getWritableDatabase();
        return localdb.insert("obs", null, complaintEntries);


    }


}
