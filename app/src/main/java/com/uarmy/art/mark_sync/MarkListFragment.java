package com.uarmy.art.mark_sync;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.uarmy.art.mark_merge.MergeInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class MarkListFragment extends ListFragment
                                                    implements MarkSync.MarksHolder {

    public static final String ARG_TYPE = "type";

    private String myType;
    private OnFragmentInteractionListener myListener;
    private ArrayAdapter<MarkDesc> myAdapter;
    Collection<JSONObject> myMarks;

    private class MarkDesc {
        //JSONObject myJson;
        String myStr;
        MarkDesc(JSONObject o) {
            //myJson = o;
            try {
                myStr = o.getString("title") + "\n" + o.getString("owner") + "\n" + o.getString("uuid");
            } catch(JSONException je) {
                je.printStackTrace();
            }
        };

        MarkDesc(MergeInfo mi) {
            //myJson = o;
            try {
                JSONObject o = mi.getOur();
                myStr = o.getString("title") + "\n" + o.getString("owner") + "\n" + o.getString("uuid") + "\n[ ";
                if(mi.getDescChanged())
                    myStr += "desc ";
                if(mi.getLocationChanged())
                    myStr += "loc ";
                if(mi.getModTimeChanged())
                    myStr += "mod_time ";
                if(mi.getTitleChanged())
                    myStr += "title ";
                myStr += "]";

            } catch(JSONException je) {
                je.printStackTrace();
            }
        };

        public String toString() { return myStr; }
    }

    public static MarkListFragment newInstance(String type) {
        MarkListFragment fragment = new MarkListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MarkListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            myType = getArguments().getString(ARG_TYPE);
        }

        myAdapter = new ArrayAdapter<MarkDesc>(getActivity(), android.R.layout.simple_list_item_1);
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

    //@interface MarksHolder
    public void updateMarks() {

        if(myListener==null)
           return;

        if(myType.equals("my"))
            myMarks = myListener.getOwnItems();
        else
            myMarks = myListener.getNewItems();

        setItems(myMarks);

        if(getListView() != null) {

            if (myAdapter != null && myAdapter.getCount() != 0)
                getListView().setVisibility(View.VISIBLE);
            else
                getListView().setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("FR", "Hello I am fragment!!!!!!!!!!!!!!!!!!!!!!!!");
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


    public void setItems(Collection<JSONObject> items) {
        myAdapter.clear();

        if(items!=null && !items.isEmpty()) {
            for(JSONObject o: items) {
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
