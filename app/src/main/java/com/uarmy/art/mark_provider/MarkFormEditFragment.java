package com.uarmy.art.mark_provider;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.uarmy.art.bt_sync.R;

import java.util.UUID;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.uarmy.art.mark_provider.MarkFormEditFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link com.uarmy.art.mark_provider.MarkFormEditFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MarkFormEditFragment extends Fragment {

    private static final String TAG = "MarkFormFragment";
    private static final String ARG_ID = "ID";
    private long myID;

    private OnMarkFormInteractionListener mListener;

    /**
     * @param id mark id in the content providers database.
     * @return A new instance of fragment fragments_marks_browser_form.
     */
    public static MarkFormEditFragment newInstance(long id) {
        MarkFormEditFragment fragment = new MarkFormEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    private class OnApplyListener implements View.OnClickListener {

        private final Fragment myFragment;
        private final long myID;
        private final View myRootView;
        private final OnMarkFormInteractionListener myListener;

        OnApplyListener(Fragment f, long mark_id, View rootView, OnMarkFormInteractionListener listener) {
            myFragment = f;
            myID = mark_id;
            myRootView = rootView;
            myListener = listener;
        }
        public void onClick(View v) {
            Bundle b = gatherMarkInfo(myRootView);
            mListener.onMarkEditFormFragmentInteraction(myFragment, b);

        }
    };

    public MarkFormEditFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            myID = getArguments().getLong(ARG_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_fragments_marks_browser_edit_form, container, false);

        String[] proj = new String[] {

                MarksProvider.Contract.TITLE,
                MarksProvider.Contract.DESC,
                MarksProvider.Contract.OWNER,
                MarksProvider.Contract.UUID,
                MarksProvider.Contract.X,
                MarksProvider.Contract.Y,
                MarksProvider.Contract.H,
        };

        Cursor c = null;

        if(myID!=-1) { // if not "add new"

            /** Synchronous way to query Marks. Query marks with id = myID */
            ContentResolver cr = getActivity().getContentResolver();
            String strID = String.valueOf(myID);
            Uri uri = MarksProvider.Contract.CONTENT_URI.buildUpon().appendPath(strID).build();
            c = cr.query(uri, proj, null, null, null);
        }

        if(c!=null && c.getCount()!=1) {
            Log.e(TAG, "Error mark with ID: " + myID + " was not found!");

        } else {

            // do it in a synchronous way here (just to have a variety of approaches)

            EditText titleEditView = (EditText)rootView.findViewById(R.id.mark_form_title_content);
            EditText descEditView = (EditText)rootView.findViewById(R.id.mark_form_desc_content);
            TextView ownerTextView = (TextView)rootView.findViewById(R.id.mark_form_owner_content);
            TextView uuidTextView = (TextView)rootView.findViewById(R.id.mark_form_uuid_content);

            EditText px = (EditText)rootView.findViewById(R.id.mark_form_pos_x);
            EditText py = (EditText)rootView.findViewById(R.id.mark_form_pos_y);
            EditText ph = (EditText)rootView.findViewById(R.id.mark_form_pos_h);

            if(c!=null) {
                c.moveToNext(); // move from -1 to first record

                String[] names = {MarksProvider.Contract.TITLE, MarksProvider.Contract.DESC, MarksProvider.Contract.OWNER, MarksProvider.Contract.UUID};
                TextView[] views = {titleEditView, descEditView, ownerTextView, uuidTextView};
                for (int i = 0; i < names.length; ++i) {
                    int idx = c.getColumnIndex(names[i]);
                    views[i].setText(c.getString(idx));
                }

                String[] pos_names = {MarksProvider.Contract.X, MarksProvider.Contract.Y, MarksProvider.Contract.H};
                TextView[] pos_views = {px, py, ph};
                for (int i = 0; i < pos_names.length; ++i) {
                    int idx = c.getColumnIndex(pos_names[i]);
                    pos_views[i].setText(String.valueOf(c.getDouble(idx)));
                }

            } else if(c==null && myID==-1) { // if adding new
                //String uuid = UUID.randomUUID().toString();
                String owner = getUser(); // should return user of a program/device
                ownerTextView.setText(owner);
                uuidTextView.setText("----");
                //...
            }

            Button apply_btn = (Button)rootView.findViewById(R.id.mark_edit_form_apply_changes_btn);
            apply_btn.setOnClickListener( new OnApplyListener(this, myID, rootView, mListener));

        }

        return rootView;
    }

    private Bundle gatherMarkInfo(View rootView) {
        Bundle b = new Bundle();

        EditText titleEditView = (EditText)rootView.findViewById(R.id.mark_form_title_content);
        EditText descEditView = (EditText)rootView.findViewById(R.id.mark_form_desc_content);

        EditText px = (EditText)rootView.findViewById(R.id.mark_form_pos_x);
        EditText py = (EditText)rootView.findViewById(R.id.mark_form_pos_y);
        EditText ph = (EditText)rootView.findViewById(R.id.mark_form_pos_h);

        b.putString(MarksProvider.Contract.TITLE, titleEditView.getText().toString());
        b.putString(MarksProvider.Contract.DESC, descEditView.getText().toString());

        String[] dbl_fields = {MarksProvider.Contract.X, MarksProvider.Contract.Y, MarksProvider.Contract.H};
        EditText[] edits = {px, py, ph};
        for(int i=0;i<dbl_fields.length;++i) {
            b.putDouble(dbl_fields[i],  Double.valueOf(edits[i].getText().toString()));
        }

        if(myID==-1) { // if adding new mark
            b.putString(MarksProvider.Contract.OWNER, getUser());
            b.putString(MarksProvider.Contract.UUID, UUID.randomUUID().toString());
        } else {
            b.putLong(MarksProvider.Contract._ID, myID);
        }


        return b;

    }

    private String getUser() {
        return "Sergii";
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnMarkFormInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnMarkFormInteractionListener {
        public void onMarkEditFormFragmentInteraction(Fragment f, Bundle b);
    }

}
