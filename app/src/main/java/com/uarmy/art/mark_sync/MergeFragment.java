package com.uarmy.art.mark_sync;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.uarmy.art.bt_sync.BtDeviceDesc;
import com.uarmy.art.bt_sync.R;
import com.uarmy.art.mark_merge.MarkMerger;
import com.uarmy.art.mark_merge.MergeInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MergeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MergeFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MergeFragment extends Fragment {

    private static final String ARG_CONFLICT_POSITION = "position";
    private int myPosition;
    private MergeMarkAdapter myArrayAdapter;

    private OnFragmentInteractionListener myListener;

    public class FieldInfo {
        public final String myType;
        public final String myLeft;
        public final String myRight;
        public boolean myLeftSelected;
        public MergeInfo myMergeInfo;
        FieldInfo(String type, MergeInfo mi, String left, String right) {
            myType = type;
            myLeft = left;
            myRight = right;
            myMergeInfo = mi;
        }

        public void setFieldSelection(boolean v) {
            myMergeInfo.setFieldSelection(myType, v);
        }
    };

    public class MergeMarkAdapter extends ArrayAdapter<FieldInfo> {
        public MergeMarkAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            FieldInfo fi = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            MergeItemView itemView = (MergeItemView)convertView;
            if (itemView == null) {
                itemView = MergeItemView.inflate(parent);
            }

            itemView.setItem(fi);
            return itemView;
        }
    }

    /**
     * @param position position in conflicts array.
     * @return A new instance of fragment MergeFragment.
     */
    public static MergeFragment newInstance(int position) {
        MergeFragment fragment = new MergeFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CONFLICT_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }
    public MergeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            myPosition = getArguments().getInt(ARG_CONFLICT_POSITION);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_merge, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            myListener = (OnFragmentInteractionListener) activity;
            myListener.setMergeFragmentState(true);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        myListener.setMergeFragmentState(false);
        myListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(myArrayAdapter == null) {
            myArrayAdapter = new MergeMarkAdapter(getActivity());
            ListView listView = (ListView) getActivity().findViewById(R.id.merge_list);
            //listView.setOnItemClickListener(myMessageClickedHandler);
            listView.setAdapter(myArrayAdapter);
        }
        updateUI();
    }


    private void updateUI() {
        if(myListener==null)
            return;

        List<MergeInfo> mi_collection = myListener.getConflictsItems();
        MergeInfo mi = mi_collection.get(myPosition);

        myArrayAdapter.clear();
        //String[] fields = { MarkMerger.FLD_TITLE, MarkMerger.FLD_DESC, MarkMerger.FLD_LOCATION };

        try {

            if (mi.getTitleChanged()) {
                myArrayAdapter.add(
                    new FieldInfo(MarkMerger.FLD_TITLE, mi,
                        mi.getOur().getString(MarkMerger.FLD_TITLE),
                        mi.getTheir().getString(MarkMerger.FLD_TITLE))
                );
            }

            if(mi.getDescChanged()) {
                myArrayAdapter.add(
                    new FieldInfo(MarkMerger.FLD_DESC, mi,
                                mi.getOur().getString(MarkMerger.FLD_DESC),
                                mi.getTheir().getString(MarkMerger.FLD_DESC))
                );
            }

            if(mi.getLocationChanged()) {

                    double our_x = mi.getOur().getJSONArray(MarkMerger.FLD_LOCATION).getDouble(0);
                    double our_y = mi.getOur().getJSONArray(MarkMerger.FLD_LOCATION).getDouble(1);

                    double their_x = mi.getTheir().getJSONArray(MarkMerger.FLD_LOCATION).getDouble(0);
                    double their_y = mi.getTheir().getJSONArray(MarkMerger.FLD_LOCATION).getDouble(1);

                    myArrayAdapter.add(
                            new FieldInfo(MarkMerger.FLD_LOCATION, mi,
                                    "x: " + our_x + ", y: " + our_y,
                                    "x: " + their_x + ", y: " + their_y
                            )
                    );
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    public int getConflictPosition() { return myPosition; }

}
