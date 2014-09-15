package com.uarmy.art.mark_provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.uarmy.art.mark_merge.MarkMerger;

import org.apache.http.auth.AUTH;

import java.util.Map;

public class MarksProvider extends ContentProvider {

    private static final UriMatcher ourUriMatcher;
    private static final String TAG = "MarksProvider";

    private static final String MARKS_TABLE_NAME = "markstbl";
    private static final String AUTHORITY = "com.uarmy.art.mark_provider.provider";

    /** Every Mark has next fields. You can see below that many of them cannot be NULL.
     * ContentProvider will check for required fields and throw an exception in case one of them is not provided.
     */
    public static final class Contract {
        public final static String _ID = "_id";
        public final static String UUID = "uuid";
        public final static String OWNER = "owner";
        public final static String TITLE = "title";
        public final static String DESC = "description";
        public final static String X = "x";
        public final static String Y = "y";
        public final static String H = "h";

        public final static Uri CONTENT_URI = (new Uri.Builder()).authority(AUTHORITY).scheme("content").appendPath(MARKS_TABLE_NAME).build();

    };


    private static final String SQL_CREATE_MAIN =
            //"DROP TABLE " + MARKS_TABLE_NAME + ";" +
            "CREATE TABLE " +
            MARKS_TABLE_NAME +
            "(" +
            " _id INTEGER PRIMARY KEY, " +
            " uuid TEXT NOT NULL," +
            " title TEXT NOT NULL," +
            " description TEXT," +
            " owner TEXT NOT NULL," +
            " x REAL NOT NULL," +
            " y REAL NOT NULL," +
            " h REAL NOT NULL" +
            ")";


    private static final int MARKS_TABLE = 1;
    private static final int MARKS_TABLE_RECORD = 2;

    protected static final class MainDatabaseHelper extends SQLiteOpenHelper {
        MainDatabaseHelper(Context context, String db_name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, db_name, factory, version);
        }

        public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {
            // put everything what is needed when upgrading database (changing version in (*) )
            Log.d(TAG, "onUpgrade");
            db.execSQL("DROP TABLE " + MARKS_TABLE_NAME);
            db.execSQL(SQL_CREATE_MAIN);
        }

        /*
         * Creates the data repository. This is called when the provider attempts to open the
         * repository and SQLite reports that it doesn't exist.
         */
        public void onCreate(SQLiteDatabase db) {

            // Creates the main table
            db.execSQL(SQL_CREATE_MAIN);
        }
    }
    private MainDatabaseHelper myOpenHelper;

    // Defines the database name
    private static final String DB_NAME = "marksdb";

    // Holds the database object
    private SQLiteDatabase myDB;

    static {
        ourUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        ourUriMatcher.addURI(AUTHORITY, MARKS_TABLE_NAME, MARKS_TABLE);
        ourUriMatcher.addURI(AUTHORITY, MARKS_TABLE_NAME + "/#", MARKS_TABLE_RECORD);
    }

    public MarksProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int match = ourUriMatcher.match(uri);
        switch (match) {
            case MARKS_TABLE:
                break;
            case MARKS_TABLE_RECORD:
                if(selection==null)
                    selection = "_ID = " + uri.getLastPathSegment();
                else
                    selection = selection + " AND _ID = " + uri.getLastPathSegment();
                break;
            default:
                throw new IllegalArgumentException("Unrecognized uri. Uri cannot be matched.");
        }

        myDB = myOpenHelper.getWritableDatabase();
        return myDB.delete(MARKS_TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public String getType(Uri uri) {

        switch (ourUriMatcher.match(uri)) {
            case MARKS_TABLE:
                return "vnd.android.cursor.dir/vnd.com.uarmy.art.mark_provider.provider." + MARKS_TABLE_NAME;
                //break;
            case MARKS_TABLE_RECORD:
                return "vnd.android.cursor.item/vnd.com.uarmy.art.mark_provider.provider." + MARKS_TABLE_NAME;
                //break;
            default:
                throw new IllegalArgumentException("Unrecognized uri. Uri cannot be matched.");
        }

        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        String[] mandatory_fields = {Contract.UUID, Contract.OWNER};
        for(String fld: mandatory_fields) {
            // create JSON from values and validate it or just validate values
            if (!values.containsKey(fld) || values.getAsString(fld).isEmpty())
                throw new IllegalArgumentException(fld + " column cannot be empty.");
        }

        String[] mandatory_number_fields = {Contract.X, Contract.Y, Contract.H};
        for(String fld: mandatory_number_fields) {
            // create JSON from values and validate it or just validate values
            if (!values.containsKey(fld))
                throw new IllegalArgumentException(fld + " column cannot be empty");
        }

        for(Map.Entry<String, Object> e: values.valueSet()) {
            Log.d(TAG, e.getKey() + ": " + e.getValue().toString());


        }

        myDB = myOpenHelper.getWritableDatabase();

        // check that uuid(s) are unique
        String[] col = {Contract.UUID};
        String[] selArgs = { values.getAsString(Contract.UUID) };
        Cursor c = myDB.query(MARKS_TABLE_NAME, col, "UUID = ?",  selArgs, null, null, null);
        if(c.getCount()!=0)
            throw new IllegalArgumentException(Contract.UUID + " column should be unique. Trying to insert mark with existing uuid: " + selArgs[0]);


        long id = myDB.insert(MARKS_TABLE_NAME, "", values);
        //throw new IllegalArgumentException(Contract.UUID + " column cannot be empty.");
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public boolean onCreate() {
        /*
         * Creates a new helper object. This method always returns quickly.
         * Notice that the database itself isn't created or opened
         * until SQLiteOpenHelper.getWritableDatabase is called
         */
        myOpenHelper = new MainDatabaseHelper(
                getContext(),        // the application context
                DB_NAME,              // the name of the database)
                null,                // uses the default SQLite cursor
                1                    // the version number (*)
        );

        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        int match = ourUriMatcher.match(uri);
        switch (match) {
            case MARKS_TABLE:
                if (TextUtils.isEmpty(sortOrder))
                    sortOrder = "_ID ASC";
                break;
            case MARKS_TABLE_RECORD:
                if(selection==null)
                    selection = "_ID = " + uri.getLastPathSegment();
                else
                    selection += " AND _ID = " + uri.getLastPathSegment();
                break;
            default:
                throw new IllegalArgumentException("Unrecognized uri. Uri cannot be matched.");
        }


        // TODO: add group by by using uri matcher

        myDB = myOpenHelper.getReadableDatabase();
        Cursor c = myDB.query(MARKS_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {

        if (values.containsKey(Contract.UUID))
            throw new IllegalArgumentException("Cannot modify uuid value of a mark.");

        int match = ourUriMatcher.match(uri);
        switch (match) {
            case MARKS_TABLE:
                break;
            case MARKS_TABLE_RECORD:
                if(selection.isEmpty())
                    selection = "_ID = " + uri.getLastPathSegment();
                else
                    selection = selection + " AND _ID = " + uri.getLastPathSegment();
                break;
            default:
                throw new IllegalArgumentException("Unrecognized uri. Uri cannot be matched.");
        }

        myDB = myOpenHelper.getWritableDatabase();
        int num_rows_affected = myDB.update(MARKS_TABLE_NAME, values, selection, selectionArgs);
        return num_rows_affected;
    }
}