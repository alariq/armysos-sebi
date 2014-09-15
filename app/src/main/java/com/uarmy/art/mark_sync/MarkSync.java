package com.uarmy.art.mark_sync;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.uarmy.art.bt_sync.BtSync;
import com.uarmy.art.bt_sync.R;
import com.uarmy.art.mark_merge.MarkMerger;
import com.uarmy.art.mark_merge.MergeInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MarkSync extends ActionBarActivity
    implements OnFragmentInteractionListener {

    private static final int RESULT_SYNC_MARKS = 1;
    private static final String TAG = "MarkSync";

    private MarkMerger myMarkMerger;

    Collection<JSONObject> myOwn = new ArrayList<JSONObject>();
    Collection<JSONObject> myNew = new ArrayList<JSONObject>();
    List<MergeInfo> myConflicts = new ArrayList<MergeInfo>();

    private MergeFragment myMergeFragment;
    private MarksHolder myCurrentFragmentTab;
    private boolean myMergeFragmentActive = false;

    AlertDlg myNotResolvedDlg, myResolveOkDlg, myResolveFailedDlg;

    public static AlertDlg newAlertDlgInstance(int title_res_id) {
        AlertDlg fragment = new AlertDlg();
        Bundle args = new Bundle();
        args.putInt(AlertDlg.ARG_TITLE_RES_ID, title_res_id);
        fragment.setArguments(args);
        return fragment;
    }

    public static class AlertDlg extends DialogFragment {

        public static final String ARG_TITLE_RES_ID = "title";

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            int title_res_id = -1;

            if (getArguments() != null) {
                title_res_id = getArguments().getInt(ARG_TITLE_RES_ID);
            }

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(title_res_id/*R.string.conflicts_are_not_resolved*/)
                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });

            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    private static String createMark(String title, String owner, String uuid, Double x, Double y, String desc) {
        String mark = "{"
                + "  \"location\": [" + x.toString() + ", " + y.toString() + "], "
                + "  \"title\": \"" + title + "\", "
                + " \"uuid\": \"" + uuid + "\", "
                + " \"desc\": \"" + desc + "\", "
                + " \"owner\": \"" + owner + "\", "
                + " \"mod_time\": 15"
                + "}\n";
        return mark;
    }

    // used as test, in reality should read users marks file
    private String readMarks() {
        // TODO: remove BluetoothAdapter if run on emulator or it will crash, emulator does not have Bluetooth!!!!!!!!!!
        String user =  "Android-x86_N" + (int)(100.0*Math.random());
                       //BluetoothAdapter.getDefaultAdapter().getName();
        String marks =
                createMark("base1", user, UUID.randomUUID().toString(),  11.0467, 54.0021, "simple description") +  " \n" + // unique
                // should create conflict, user can never be different if uuid is same, so we keep it same
                createMark("base2" + user, "Sergii", "23",  22.0467, 55.0021, "some waypoint on the map") + " \n" +
                createMark("MathBase" + user, "Sergii", "24",  2.71, 3.1415, "other mathematical waypoint") + " \n" +
                createMark("base3", user, UUID.randomUUID().toString(),  33.0467, 56.0021, "unknown base location") + "\n"; // unique
        return marks;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(false) {
            setContentView(R.layout.activity_mark_sync);
            //setupUI();
            //updateUI(jmarks, null, null);
        } else {
            // setup action bar for tabs
            ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.setDisplayShowTitleEnabled(false);

                ActionBar.Tab tab = actionBar.newTab()
                        .setText(R.string.my_tab)
                        .setTabListener(new MarkListTabListener(this, "my"));
                actionBar.addTab(tab);

                tab = actionBar.newTab()
                        .setText(R.string.new_tab)
                        .setTabListener(new MarkListTabListener(this, "new"));
                actionBar.addTab(tab);

                tab = actionBar.newTab()
                        .setText(R.string.conflicts_tab)
                        .setTabListener(new ConflictedMarkListTabListener(this, "conflicts"));
                actionBar.addTab(tab);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        // if mark merger is created this means that we were restored, so do not read marks
        if(myMarkMerger == null) {
            String my_marks = readMarks();
            String[] arr = {my_marks};
            myOwn = MarkMerger.stringToJSON(arr);
            myMarkMerger = new MarkMerger(myOwn, true);
        }
        super.onResume();
    }

    @Override
    protected void onRestoreInstanceState (Bundle savedInstanceState) {
        Log.d(TAG, "onRestoreInstanceState ");
        myMarkMerger = MarkMerger.fromBundle(savedInstanceState);
        updateItems(myMarkMerger);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");

        // TODO: create MarkMerger at the beginning!
        if(myMarkMerger!=null) {
            myMarkMerger.toBundle(outState);
        }
        // we do not want any other saved crap (live current fragment because we do not it to be restored, we will do everything ourself!)
        //super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        // stuff ...
        super.onDestroy();
    }

    public interface MarksHolder {
        public void updateMarks();
    }

    public void setCurrentTab(Fragment mFragment) {
        myCurrentFragmentTab = (MarksHolder)mFragment;
    }

    private void openMergeDialog(MergeInfo mi, int position) {
        MergeFragment mf = MergeFragment.newInstance(position);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(android.R.id.content, mf);
        transaction.addToBackStack(null);
        transaction.commit();
        myMergeFragment = mf;
        setCurrentTab(null); // no tab visible

    }

    public void onFragmentInteraction(int position, String tag) {
        if(tag.equals("conflicts")) {
            MergeInfo mi = myConflicts.get(position);
            openMergeDialog(mi, position);
        } else if(tag.equals("merge")) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            //transaction.remove(myMergeFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }

    }

    public void onApplyMergeResultBtn(View view) {

        int pos = myMergeFragment.getConflictPosition();
        myMarkMerger.getConflicts().get(pos).markResolved(true);
        myMarkMerger.resolve(pos);
        updateItems(myMarkMerger);
        getSupportFragmentManager().popBackStack();
    }

    public Collection<JSONObject> getOwnItems() { return myOwn; }
    public Collection<JSONObject> getNewItems() { return myNew; }
    public List<MergeInfo> getConflictsItems() { return myConflicts; }
    public void setMergeFragmentState(boolean b_active) { myMergeFragmentActive = b_active; }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mark_sync, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_sync) {
            if(myConflicts.size()!=0) {
                if(myNotResolvedDlg==null)
                    myNotResolvedDlg = newAlertDlgInstance(R.string.conflicts_are_not_resolved);
                myNotResolvedDlg.show(getSupportFragmentManager(), "AlertDlg");
            } else if(myNew.size()!=0) {
                newAlertDlgInstance(R.string.please_apply_sync_results).show(getSupportFragmentManager(), "AlertDlg");
            } else {
                sync();
            }
            return true;
        } else if(id == R.id.action_apply_merge_results) {
            if(myConflicts.size()!=0) {
                if(myNotResolvedDlg==null)
                    myNotResolvedDlg = newAlertDlgInstance(R.string.conflicts_are_not_resolved);
                myNotResolvedDlg.show(getSupportFragmentManager(), "AlertDlg");
            } else {
                // TODO: maybe have one dialog and use setTitle() ?
                boolean res = myMarkMerger.apply();
                if(res) {
                    if(myResolveOkDlg==null)
                        myResolveOkDlg = newAlertDlgInstance(R.string.resolve_succeeded);
                    myResolveOkDlg.show(getSupportFragmentManager(), "AlertDlg");
                } else {
                    if(myResolveFailedDlg==null)
                        myResolveFailedDlg = newAlertDlgInstance(R.string.resolve_failed);
                    myResolveFailedDlg.show(getSupportFragmentManager(), "AlertDlg");
                }
                updateItems(myMarkMerger);
                if(myCurrentFragmentTab!=null)
                    myCurrentFragmentTab.updateMarks();
                return true;
            }
        } else if(id == R.id.action_cancel_sync_results) {
            boolean res = myMarkMerger.revert();
            updateItems(myMarkMerger);
            if(myCurrentFragmentTab!=null)
                myCurrentFragmentTab.updateMarks();
            if(myMergeFragmentActive) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.remove(myMergeFragment);
                transaction.commit();
            }
            // TODO: switch to OWN tab
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onDataReceived(String msg) {

        //String[] arr_tmp =  { genFakeRecvData() };
        //List<JSONObject> jmarks = MarkMerger.stringToJSON(arr_tmp);

        //myMarkMerger = new MarkMerger(myOwn, true);
        myMarkMerger.sync(msg);
        updateItems(myMarkMerger);
        //updateUI(mm.getOurs().values(), mm.getNew().values(), mm.getConflicts());
    }

    private void updateItems(MarkMerger mm) {
        myOwn = mm.getOurs().values();
        myNew = mm.getNew().values();
        myConflicts = mm.getConflicts();
    }

    private void sync() {

        String str="";
        for(JSONObject o : myOwn) {
            str += o.toString();
        }

        Intent intent = new Intent(this, BtSync.class);
        intent.putExtra(BtSync.EXTRA_SYNC_DATA, str);
        startActivityForResult(intent, RESULT_SYNC_MARKS);
        //onDataReceived(genFakeRecvData());
    }

    private static String genFakeRecvData() {
        String ourJsonSeveralUnique = "{"
                + "  \"location\": [25.00, 5.01], "
                + "  \"title\": \"sep_base1\", "
                + " \"uuid\": \"8\", "
                + " \"desc\": \"För länge sen...\", "
                + " \"owner\": \"Ivan\", "
                + " \"mod_time\": 7532"
                + "}"
                + " {"
                + "  \"location\": [35.00, 5.01], "
                + "  \"title\": \"sep_base1\", "
                + " \"uuid\": \"9\", "
                + " \"desc\": \"För länge sen...\", "
                + " \"owner\": \"Ivan\", "
                + " \"mod_time\": 654654"
                + "}";
        return ourJsonSeveralUnique + createMark("base2_mod", "Sergii", "23",  48.0467, 55.0021, "För länge sen...") +
                createMark("MathBase", "Sergii", "24",  42.0, 24.0, "För länge sen...");
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        // FIXME: Log does not work in this method... probably because it starts new intent
        Log.d(TAG, "reqCode: " + requestCode + " resCode: " + resultCode);
        switch (requestCode) {
            case RESULT_SYNC_MARKS:
                switch (resultCode) {
                    case RESULT_OK:
                        Log.d(TAG, "SYNC RESULT OK");
                        String recv_msg = data.getStringExtra(BtSync.RECV_DATA);
                        if(recv_msg!=null) {
                            Log.d(TAG, "DATA: \n " + recv_msg);
                            onDataReceived(recv_msg);
                        }
                        break;
                    case BtSync.ACTIVITY_RESULT_FAILED:
                        Log.d(TAG, "SYNC RESULT FAILED");
                        break;
                    case RESULT_CANCELED:
                        Log.d(TAG, "SYNC RESULT CANCELED");
                        onDataReceived(genFakeRecvData());
                        break;
                    default:
                        Log.d(TAG, "unknown BtSync result: " + resultCode);
                }
                break;
        }
    }
}