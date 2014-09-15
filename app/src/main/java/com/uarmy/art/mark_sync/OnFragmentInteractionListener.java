package com.uarmy.art.mark_sync;

/**
 * Created by a on 9/4/14.
 */

import com.uarmy.art.mark_merge.MergeInfo;

import org.json.JSONObject;

import java.util.Collection;
import java.util.List;

/**
 * This interface must be implemented by activities that contain this
 * fragment to allow an interaction in this fragment to be communicated
 * to the activity and potentially other fragments contained in that
 * activity.
 * <p>
 * See the Android Training lesson <a href=
 * "http://developer.android.com/training/basics/fragments/communicating.html"
 * >Communicating with Other Fragments</a> for more information.
 */
public interface OnFragmentInteractionListener {
    // TODO: Update argument type and name
    public void onFragmentInteraction(int position, String type);
    public Collection<JSONObject> getOwnItems();
    public Collection<JSONObject> getNewItems();
    public List<MergeInfo> getConflictsItems();

    public void setMergeFragmentState(boolean b_active);
}

