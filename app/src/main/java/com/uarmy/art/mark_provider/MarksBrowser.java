package com.uarmy.art.mark_provider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.uarmy.art.bt_sync.R;

import java.util.UUID;

public class MarksBrowser extends ActionBarActivity
    implements  MarkFormFragment.OnMarkFormInteractionListener,
                MarkFormEditFragment.OnMarkFormInteractionListener {

    //MarksBrowserFragmentInteractionListener

    private static final String TAG = "MarksBrowser";

    private PlaceholderFragment myPlaceholderFragment;

    private static final String DEBUG_PREF = "__debug_pref";

    public static PopupMenuDlg newPopupMenuDlgInstance(long mark_id) {
        PopupMenuDlg fragment = new PopupMenuDlg();
        Bundle args = new Bundle();
        args.putLong(PopupMenuDlg.ARG_MARK_ID, mark_id);
        fragment.setArguments(args);
        return fragment;
    }

    public static class PopupMenuDlg extends DialogFragment {

        public static final String ARG_MARK_ID = "MARK_ID";

        // NOTE: should be in sync with R.array.popup_actions_array !!!
        private static final int ACTION_VIEW = 0;
        private static final int ACTION_EDIT = 1;
        private static final int ACTION_DELETE = 2;

        private long myID = -1;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            if (getArguments() != null)
                myID = getArguments().getLong(ARG_MARK_ID);

             if(myID==-1)
                throw new IllegalArgumentException("Must provide correct mark id argument, when call this dialog");

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.mark_browser_popup_menu_title);
            builder.setItems(R.array.popup_actions_array,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item_index) {
                            MarksBrowser activity = (MarksBrowser)getActivity();
                            switch (item_index) {
                                case ACTION_VIEW:
                                    activity.onViewMark(myID);
                                    break;
                                case ACTION_EDIT:
                                    activity.onEditMark(myID);
                                    break;
                                case ACTION_DELETE:
                                    activity.onDeleteMark(myID);
                                    break;
                            }
                        }
                    });

            return builder.create();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marks_browser);
        if (savedInstanceState == null) {
            myPlaceholderFragment = new PlaceholderFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, myPlaceholderFragment)
                    .commit();
        } else {
            Log.d(TAG, "lading with saved data");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.marks_browser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add_new_mark) {
            onEditMark(-1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // implementing OnMarkFormInteractionListener interface
    public void onFragmentInteraction(int id) {

    }

    // apply button was pressed when editing/adding new mark
    public void onMarkEditFormFragmentInteraction(Fragment f, Bundle b) {

        /** Another example of Mark update / insert */

        // does Java have function pointers??? :-/
        String[] strFields = { MarksProvider.Contract.TITLE, MarksProvider.Contract.DESC };
        String[] dblFields = { MarksProvider.Contract.X, MarksProvider.Contract.Y, MarksProvider.Contract.H };

        ContentValues update_cv = new ContentValues();
        // title, desc
        for(String fld : strFields) {
            update_cv.put(fld, b.getString(fld));
        }
        // x, y, h
        for(String fld: dblFields) {
            update_cv.put(fld, b.getDouble(fld));
        }

        if(b.containsKey(MarksProvider.Contract._ID)) {// edited
            String updSel = MarksProvider.Contract._ID + " = ?";
            String[] updSelArgs = {  String.valueOf(b.getLong(MarksProvider.Contract._ID)) };
            getContentResolver().update(MarksProvider.Contract.CONTENT_URI, update_cv, updSel, updSelArgs);
        } else { // new
            update_cv.put(MarksProvider.Contract.UUID, b.getString(MarksProvider.Contract.UUID));
            update_cv.put(MarksProvider.Contract.OWNER, b.getString(MarksProvider.Contract.OWNER));
            getContentResolver().insert(MarksProvider.Contract.CONTENT_URI, update_cv);
        }

        myPlaceholderFragment.getLoaderManager().restartLoader(0, null, myPlaceholderFragment);
        getSupportFragmentManager().popBackStack();
    }

    public void onMarkSelected(long id) {
        onViewMark(id);
    }

    public boolean onMarkLongCLick(long id) {
        newPopupMenuDlgInstance(id).show(getSupportFragmentManager(), "popup_menu_dialog");
        return true;
    }

    public boolean onViewMark(long id) {
        MarkFormFragment mf = MarkFormFragment.newInstance(id);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, mf);
        transaction.addToBackStack(null);
        transaction.commit();
        return true;
    }

    public boolean onEditMark(long id) {
        MarkFormEditFragment mf = MarkFormEditFragment.newInstance(id);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, mf);
        transaction.addToBackStack(null);
        transaction.commit();
        return true;
    }

    public boolean onDeleteMark(long mark_id) {
        /** Another example of deleteing Mark by its content provider database id field */
        ContentResolver cr = getContentResolver();
        Uri uri = MarksProvider.Contract.CONTENT_URI.buildUpon().appendPath(String.valueOf(mark_id)).build();
        int values_affected = cr.delete(uri,null, null);
        if(values_affected!=1) {
            Log.e(TAG, "Error deleting mark, id provided: " + mark_id + ", possibly, was not in the database");
        } else {
            myPlaceholderFragment.getLoaderManager().restartLoader(0, null, myPlaceholderFragment);
        }

        return true;
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment
            implements LoaderManager.LoaderCallbacks<Cursor> {

        private MarksBrowser myListener;

        private static final String DO_STARTUP_INIT = "do_startup_init";

        // This is the Adapter being used to display the list's data
        SimpleCursorAdapter myAdapter;

        // These are the Contacts rows that we will retrieve
        static final String[] ourProjection = new String[] {
                MarksProvider.Contract._ID,
                MarksProvider.Contract.TITLE,
        };

        // This is the select criteria
        static final String ourSelection = null; // select all
        static final String[] ourSelectionArgs = {""};

        private boolean myIsTestInitialized = false;


        public PlaceholderFragment() {
        }

        private void init(Activity activity) {

            if(myIsTestInitialized) // check variable first to not fetch preferences every time
                return;

            SharedPreferences settings = activity.getSharedPreferences(DEBUG_PREF, 0);
            boolean b_do_init = settings.getBoolean(DO_STARTUP_INIT, true);

            if(false == b_do_init)
                return ;

            String[] proj = new String[] {

                    MarksProvider.Contract.TITLE,
                    MarksProvider.Contract.OWNER,
                    MarksProvider.Contract.UUID,
                    MarksProvider.Contract.X,
                    MarksProvider.Contract.Y,
                    MarksProvider.Contract.H,
            };

            /** to add a new Mark you need to get a content provider */
            ContentResolver cr = activity.getContentResolver();

            /** You also need to provide an URI which identifies particular content provider
             * This is the example of URI which ponts to particular Mark (with database id = 1)
             * NOTE: This id is returned by content provider e.g. by insert() operations (refer to ContentProvider class doc),
             * you can only use it to identify Mark in your local ContentProvider's database.
             * Each mark has its own unique UUID which is a different thing, and can be used to identify
             * Mark even if it is on other users device
            */
            Uri uri = MarksProvider.Contract.CONTENT_URI.buildUpon().appendPath("1").build();
            /** deleting Mark with id=1 */
            int values_affected = cr.delete(uri,null, null);
            Log.d(TAG, "num_deleted: " + values_affected);

            /** Another way to delete mark having its id
             *  Delete all marks with title Bas01 and ids < 10
            */
            String[] vals = {"Bas01" , "10" };
            values_affected = cr.delete(MarksProvider.Contract.CONTENT_URI," title = ? AND _id < ?", vals);
            Log.d(TAG, "num_deleted: " + values_affected);

            /** Adding new Marks. If you want to add many Marks you batch insert as shown below */
            ContentValues[] rows = new ContentValues[5];
            String[] users = {"Ivan", "Petro", "Orest", "Sergii", "Sashko"};
            String[] titles = {"Bas01", "Shron", "Pidval", "Shovysche", "Stadion"};
            ContentValues duplicate = null;
            for(int i=0; i<5; i++) {
                rows[i] = new ContentValues();
                rows[i].put(MarksProvider.Contract.OWNER, users[i]);
                rows[i].put(MarksProvider.Contract.TITLE, titles[i]);
                rows[i].put(MarksProvider.Contract.UUID, UUID.randomUUID().toString());
                rows[i].put(MarksProvider.Contract.X, Math.random());
                rows[i].put(MarksProvider.Contract.Y, Math.random());
                rows[i].put(MarksProvider.Contract.H, Math.random());
                if(i==0)
                    duplicate = rows[i];
            }
            cr.bulkInsert(MarksProvider.Contract.CONTENT_URI, rows);

            /** Check that inserting Mark with duplicate UUID will fail,
             * you can check other errors this way. I tried to provide clear description once error occurs
             */
            boolean test_passed = false;
            try {
                cr.insert(MarksProvider.Contract.CONTENT_URI, duplicate);
            } catch (IllegalArgumentException e) {
                test_passed = true;
                Log.d(TAG, "Duplication error-test passed, error msg: " + e.toString());
            }
            if(false == test_passed) {
                Log.e(TAG, "ERROR: Duplication error-test did not pass!");
            }

            /** Example of Mark modification */
            ContentValues update_cv = new ContentValues();
            update_cv.put(MarksProvider.Contract.TITLE, "Shron-mod");
            String updSel = "title = ?";
            String[] updSelArgs = { "Shron"};
            cr.update(MarksProvider.Contract.CONTENT_URI, update_cv, updSel, updSelArgs);

            myIsTestInitialized = true;

            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(DO_STARTUP_INIT, !myIsTestInitialized);
            editor.commit();
        }

        public void onAttach(Activity activity) {
            super.onAttach(activity);
            try {
                myListener = (MarksBrowser) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + " must implement OnFragmentInteractionListener");
            }
        }
        @Override
        public void onCreate (Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            // For the cursor adapter, specify which columns go into which views
            String[] fromColumns = {MarksProvider.Contract.TITLE};
            int[] toViews = {android.R.id.text1}; // The TextView in simple_list_item_1

            // Create an empty adapter we will use to display the loaded data.
            // We pass null for the cursor, then update it in onLoadFinished()
            myAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_1, null, fromColumns, toViews, 0);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            Activity activity = getActivity();
            init(activity);

            // TODO: maybe just new ListView() and forget about xml?
            View rootView = inflater.inflate(R.layout.fragment_marks_browser, container, false);

            ListView listView = (ListView)rootView.findViewById(R.id.marks_listview);
            listView.setAdapter(myAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView parent, View v, int position, long id) {
                    myListener.onMarkSelected(id);
                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView parent, View v, int position, long id) {
                    return myListener.onMarkLongCLick(id);
                }
            });

            /** Start query from ContentProvider. The query itself is created in {@link onCreateLoader} function.
             * When query is finished {@link onLoadFinished} is called and we fill cursor adapter with data.
             * This is a handy way of filling ListView with data from Cursor (Note, that you need to have _ID field to
             * automatically fill data from cursor to ListView). Check ListView and SimpleCursorAdapter documentation for details.
             * It is done this way, to not stall main thread. For synchronous way you may check out {@link com.uarmy.art.mark_provider.MarkFormEditFragment.onCreateView}.
             * But I recommend using this async way.
             */
            getLoaderManager().initLoader(0, null, this);


            return rootView;
        }

        // Called when a new Loader needs to be created
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.

            /** This is an implicit method of querying data from ContentProvider */
            return new CursorLoader(getActivity(), MarksProvider.Contract.CONTENT_URI,
                    ourProjection, ourSelection, null, null);
        }

        // Called when a previously created loader has finished loading
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            // Swap the new cursor in.  (The framework will take care of closing the
            // old cursor once we return.)
            String[] names = data.getColumnNames();
            for(String n: names) {
                Log.d(TAG, n);
            }
            /** fill cursor adapter with data */
            myAdapter.swapCursor(data);
        }

        // Called when a previously created loader is reset, making the data unavailable
        public void onLoaderReset(Loader<Cursor> loader) {
            // This is called when the last Cursor provided to onLoadFinished()
            // above is about to be closed.  We need to make sure we are no
            // longer using it.
            myAdapter.swapCursor(null);
        }

    }
}
