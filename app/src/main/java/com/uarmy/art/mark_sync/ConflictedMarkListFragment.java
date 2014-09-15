package com.uarmy.art.mark_sync;

/**
 * Created by a on 9/3/14.
 */

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.uarmy.art.bt_sync.R;
import com.uarmy.art.mark_merge.MarkMerger;
import com.uarmy.art.mark_merge.MergeInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ConflictedMarkListFragment extends ListFragment
                                                            implements MarkSync.MarksHolder {

    public static final String ARG_TYPE = "type";

    private String myType;
    private OnFragmentInteractionListener myListener;
    private ArrayAdapter<MarkDesc> myAdapter;
    Collection<MergeInfo> myMarks;

    public class ConflictMarkAdapter extends ArrayAdapter<MarkDesc> {
        public ConflictMarkAdapter(Context context/*, ArrayList<MarkDesc> users*/) {
            super(context, R.layout.conflict_list_item/*, users*/);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            MarkDesc md = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.conflict_list_item, parent, false);
            }
            // Lookup view for data population
            TextView title = (TextView) convertView.findViewById(R.id.title);
            TextView desc = (TextView) convertView.findViewById(R.id.desc);
            // Populate the data into the template view using the data object
            title.setText(md.myTitle);
            desc.setText(md.myDesc);

            TextView lc = (TextView) convertView.findViewById(R.id.location_changed);
            lc.setVisibility(md.getMergeInfo().getLocationChanged() ? View.VISIBLE : View.INVISIBLE);

            TextView dc = (TextView) convertView.findViewById(R.id.desc_changed);
            dc.setVisibility( md.getMergeInfo().getDescChanged() ? View.VISIBLE : View.INVISIBLE);

            TextView tc = (TextView) convertView.findViewById(R.id.title_changed);
            tc.setVisibility( md.getMergeInfo().getTitleChanged() ? View.VISIBLE : View.INVISIBLE);

            // Return the completed view to render on screen
            return convertView;
        }
    }

    private class MarkDesc {
        public final MergeInfo myMergeInfo;
        public String myTitle = null;
        public String myDesc = null;
        public String myOwner = null;

        MarkDesc(MergeInfo mi) {
            myMergeInfo = mi;
            try {
                JSONObject o = mi.getOur();
                myTitle = o.getString(MarkMerger.FLD_TITLE);
                myOwner = o.getString(MarkMerger.FLD_OWNER);
                myDesc = o.getString(MarkMerger.FLD_DESC);
            } catch(JSONException je) {
                je.printStackTrace();
            }
        };

        public MergeInfo getMergeInfo() { return myMergeInfo; }

    }

    public static ConflictedMarkListFragment newInstance(String type) {
        ConflictedMarkListFragment fragment = new ConflictedMarkListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ConflictedMarkListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            myType = getArguments().getString(ARG_TYPE);
        }

        myAdapter = new ConflictMarkAdapter(getActivity());
        setListAdapter(myAdapter);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            myListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        myListener = null;
    }

    public void updateMarks() {
        if (myListener == null)
            return;

        myMarks = myListener.getConflictsItems();
        setItems(myMarks);

        if (getListView() != null) {
            if (myAdapter != null && myAdapter.getCount() != 0)
                getListView().setVisibility(View.VISIBLE);
            else
                getListView().setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (null != myListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            myListener.onFragmentInteraction(position, myType);
        }
    }

    public void setItems(Collection<MergeInfo> items) {
        myAdapter.clear();

        if(items!=null && !items.isEmpty()) {
            for(MergeInfo o: items) {
                myAdapter.add( new MarkDesc(o));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMarks();
    }
}

