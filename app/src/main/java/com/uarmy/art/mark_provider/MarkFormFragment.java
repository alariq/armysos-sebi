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
import android.widget.ListView;
import android.widget.TextView;

import com.uarmy.art.bt_sync.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MarkFormFragment.OnMarkFormInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MarkFormFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MarkFormFragment extends Fragment {

    private static final String TAG = "MarkFormFragment";
    private static final String ARG_ID = "ID";
    private long myID;

    private OnMarkFormInteractionListener mListener;

    /**
     * @param id mark id in the content providers database.
     * @return A new instance of fragment fragments_marks_browser_form.
     */
    public static MarkFormFragment newInstance(long id) {
        MarkFormFragment fragment = new MarkFormFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    public MarkFormFragment() {
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

        View rootView = inflater.inflate(R.layout.fragment_fragments_marks_browser_form, container, false);

        String[] proj = new String[] {

                MarksProvider.Contract.TITLE,
                MarksProvider.Contract.DESC,
                MarksProvider.Contract.OWNER,
                MarksProvider.Contract.UUID,
                MarksProvider.Contract.X,
                MarksProvider.Contract.Y,
                MarksProvider.Contract.H,
        };

        /** Synchronous way to query Marks. Query marks with id = myID */
        ContentResolver cr = getActivity().getContentResolver();
        String strID = String.valueOf(myID);
        Uri uri = MarksProvider.Contract.CONTENT_URI.buildUpon().appendPath(strID).build();
        Cursor c = cr.query(uri, proj, null, null, null);
        if(c.getCount()!=1) {
            Log.e(TAG, "Error mark with ID: " + myID + " was not found!");
        } else {

            // do it ina synchronous way here (just to have a variety of approaches)

            TextView titleTextView = (TextView)rootView.findViewById(R.id.mark_form_title_content);
            TextView descTextView = (TextView)rootView.findViewById(R.id.mark_form_desc_content);
            TextView ownerTextView = (TextView)rootView.findViewById(R.id.mark_form_owner_content);
            TextView uuidTextView = (TextView)rootView.findViewById(R.id.mark_form_uuid_content);

            TextView px = (TextView)rootView.findViewById(R.id.mark_form_pos_x);
            TextView py = (TextView)rootView.findViewById(R.id.mark_form_pos_y);
            TextView ph = (TextView)rootView.findViewById(R.id.mark_form_pos_h);

            c.moveToNext(); // move from -1 to first record

            String[] names = {MarksProvider.Contract.TITLE, MarksProvider.Contract.DESC, MarksProvider.Contract.OWNER, MarksProvider.Contract.UUID};
            TextView[] views = {titleTextView, descTextView, ownerTextView, uuidTextView};
            for(int i=0; i<names.length;++i) {
                int idx = c.getColumnIndex(names[i]);
                views[i].setText(c.getString(idx));
            }

            String[] pos_names = {MarksProvider.Contract.X, MarksProvider.Contract.Y, MarksProvider.Contract.H};
            TextView[] pos_views = {px, py, ph};
            for(int i=0; i<pos_names.length;++i) {
                int idx = c.getColumnIndex(pos_names[i]);
                pos_views[i].setText(String.valueOf(c.getDouble(idx)));
            }
        }

        return rootView;
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
        public void onFragmentInteraction(int id);
    }

}
