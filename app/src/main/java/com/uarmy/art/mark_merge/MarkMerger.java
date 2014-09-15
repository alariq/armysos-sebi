package com.uarmy.art.mark_merge;

/**
 * Created by a on 8/31/14.
 */

import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MarkMerger {

    private final static String TAG = "MarksMerger";

    Map<String, JSONObject> myOurs = null;
    Map<String, JSONObject> myNew = null;
    List<MergeInfo> myConflicts = null;

    public Map<String, JSONObject> getOurs() { return myOurs; };
    public Map<String, JSONObject> getNew() { return myNew; };
    public List<MergeInfo> getConflicts() { return myConflicts; };

    public static final String FLD_LOCATION = "location";
    public static final String FLD_TITLE = "title";
    public static final String FLD_UUID = "uuid";
    public static final String FLD_DESC = "desc";
    public static final String FLD_OWNER = "desc";
    public static final String FLD_MOD_TIME = "mod_time";

    public static final int RESOLVE_STD = 0;
    public static final int RESOLVE_FORCE_OURS = 1;
    public static final int RESOLVE_FORCE_THEIRS = 2;

    private static class KeyDesc {

        public static final int T_DOUBLE = 0;
        public static final int T_ARRAY = 1;
        public static final int T_STR = 2;
        public static final int T_LONG = 3;

        private final String myKeyName;
        private final int myValueType;
        private final int myArrayLength;
        private final int myArrayElementType;

        KeyDesc(String k, int value_type) {
            myKeyName = k;
            myValueType = value_type;
            myArrayLength = -1;
            myArrayElementType = -1;

        }

        // switched params order to lesser mistakes
        KeyDesc(int array_length, int array_el_type, String k) {
            myKeyName = k;
            myValueType = T_ARRAY;
            myArrayLength = array_length;
            myArrayElementType = array_el_type;
        }

        public String getKeyName() { return myKeyName; }
        public int getValueType() { return myValueType; }

        public int getArrayLength() { return myArrayLength; }
        public int getArrayElType() { return myArrayElementType; }

    }

    //private static final String[] ourMandatoryKeys = { FLD_UUID, FLD_OWNER, FLD_LOCATION, FLD_TITLE, FLD_DESC, FLD_MOD_TIME };
    private static final KeyDesc[] ourMandatoryKeys = {
            new KeyDesc(FLD_UUID, KeyDesc.T_STR),
            new KeyDesc(FLD_OWNER, KeyDesc.T_STR),
            new KeyDesc(2, KeyDesc.T_DOUBLE, FLD_LOCATION),
            new KeyDesc(FLD_TITLE, KeyDesc.T_STR),
            new KeyDesc(FLD_DESC, KeyDesc.T_STR),
            new KeyDesc(FLD_MOD_TIME, KeyDesc.T_DOUBLE)
    };

    // converts string to array of JSON objects, each string in array may contain several marks
    public static List<JSONObject> stringToJSON(String[] marks) {
        List<JSONObject> jsons = new ArrayList<JSONObject>();
        for(String mark : marks) {
            JSONTokener tokener = new JSONTokener(mark);
            try {
                while(tokener.more()) {
                    JSONObject object = (JSONObject) tokener.nextValue();
                    if(verify(object)) {
                        jsons.add(object);
                    } else {
                        Log.d(TAG, "Malformed Mark object:\n" + object.toString() + " \n Will not be added. Has format changed?");
                    }
                }
            } catch(JSONException je) {
                Log.d(TAG, "error parsing json mark from string, skipping: ---------\n" + mark + "-----------------\n\n");
                je.printStackTrace();
            }
        }
        return jsons;
    }


    public MarkMerger(String[] marks) {
        reset();
        List<JSONObject> jsons = stringToJSON(marks);
        addOurs(jsons, true);
    }

    public MarkMerger(Collection<JSONObject> jsons, boolean b_verified) {
        reset();
        addOurs(jsons, b_verified);
    }

    public void addOurs(Collection<JSONObject> jsons, boolean b_verified) {
        try {
            for(JSONObject o : jsons) {
                if (b_verified || verify(o)) {
                    String uuid = o.getString(FLD_UUID);
                    if(!myOurs.containsKey(uuid))
                        myOurs.put(uuid, o);
                    else {
                        Log.d(TAG, "Trying to add mark which uuid is already added! Skipping");
                    }
                } else {
                    Log.d(TAG, "Malformed Mark object:\n" + o.toString() + " \n Will not be added. Has format changed?");
                }
            }
        }  catch(JSONException je) {
            Log.d(TAG, "Error: Mark has no uuid field!");
            je.printStackTrace();
        }
    }


    private void reset() {
        myNew = new HashMap<String, JSONObject>();
        myOurs = new HashMap<String, JSONObject>();
        myConflicts = new ArrayList<MergeInfo>();
    }

    private MergeInfo merge(String uuid, JSONObject their, JSONObject our) {

        MergeInfo mi = new MergeInfo(their, our, uuid);

        try {
            double their_x = their.getJSONArray(FLD_LOCATION).getDouble(0);
            double their_y = their.getJSONArray(FLD_LOCATION).getDouble(1);

            double x = our.getJSONArray(FLD_LOCATION).getDouble(0);
            double y = our.getJSONArray(FLD_LOCATION).getDouble(1);
            mi.setLocationChanged(their_x!=x || their_y!=y);

            String their_title = their.getString(FLD_TITLE);
            String title = our.getString(FLD_TITLE);
            mi.setTitleChanged(!their_title.equals(title));

            String their_desc = their.getString(FLD_DESC);
            String desc = our.getString(FLD_DESC);
            mi.setDescChanged(!their_desc.equals(desc));

            long their_mod_time = their.getLong(FLD_MOD_TIME);
            long mod_time = our.getLong(FLD_MOD_TIME);
            mi.setModTimeChanged(their_mod_time!=mod_time);

            return mi;

        } catch(JSONException je) {
            Log.d(TAG, "merge: error during json method call. Probaby malformed Mark json object. Has format changed?");
            je.printStackTrace();
            return null;
        }

    }

    // NOTE: after resolve conflict array items change their positions, so everybody who relied on them should update them
    // e.g. we had 2 items , after resolve second item became first in a list!
    public boolean resolve(int index) {

        boolean b_result = false;

        MergeInfo mi = myConflicts.get(index);
        if(!mi.isMarkedResolved()) {
            Log.d(TAG, "Trying to resolve conflict which was not marked for resolve. Logic error?");
            return b_result;
        }

        JSONObject o = null;
        try {
            o = new JSONObject(mi.getOur().toString());

            for(String fld : mi.myFieldSelection.keySet()) {
                Boolean v = mi.myFieldSelection.get(fld);

                if(v == false) continue;

                if (fld.equals(MarkMerger.FLD_DESC) || fld.equals(MarkMerger.FLD_TITLE)) {
                    String val = mi.getTheir().getString(fld);
                    o.put(fld, val);
                } else if (fld.equals(MarkMerger.FLD_LOCATION)) {
                    JSONArray loc_arr = mi.getTheir().getJSONArray(fld);
                    o.put(fld, loc_arr);
                }
            }

            String uuid = o.getString(FLD_UUID);
            // check that one of new items do not have same uuid
            if(myNew.containsKey(uuid)) {
                Log.d(TAG, "Merged item has same uuid as existing new item, should not happen! Item will not be merged");
                return b_result;
            }

            myNew.put(uuid, o);
            myConflicts.remove(index);

            b_result = true;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return b_result;
    }

    public boolean apply() {
        if(myConflicts.size()!=0) {
            Log.d(TAG, "Cannot apply, because have unresolved items");
            return false;
        }

        Set<String> tmp = myNew.keySet();

        // check that everything is fine
        for(String uuid : tmp) {
            if(myOurs.containsKey(uuid)) {
                Log.d(TAG, "Ours item has same uuid as existing new item, should not happen! Aborting apply");
                return false;
            }
        }

        for(String uuid : tmp) {
            JSONObject o = myNew.get(uuid);
            myOurs.put(uuid, o);

        }

        myNew.clear();

        return true;
    }

    public boolean revert() {

        myNew.clear();
        if(myConflicts.size()==0) {
            return true;
        }

        // check that everything is fine
        for(MergeInfo mi: myConflicts) {

            String uuid = mi.getUUID();
            if(myOurs.containsKey(uuid)) {
                Log.d(TAG, "Ours item has same uuid as conflict item, should not happen! Aborting apply");
                return false;
            }
        }

        for(MergeInfo mi: myConflicts) {
            String uuid = mi.getUUID();
            myOurs.put(uuid, mi.getOur());
        }

        myConflicts.clear();
        return true;
    }

    private static boolean hasDouble(JSONObject o, String k) {
        Double d = (Double)o.optDouble(k);
        if(d.isNaN())
        {
            Log.d(TAG, "key: \"" + k + "\" must be a double number");
            return false;
        }
        return true;
    }
    private static boolean hasLong(JSONObject o, String k) {
        Long l = (Long)o.optLong(k);
        if(l == 0)
        {
            Log.d(TAG, "key: \"" + k + "\" must be a Long number and not 0");
            return false;
        }
        return true;
    }

    private static boolean hasString(JSONObject o, String k) {
        String s = o.optString(k);
        if(s==null || s.isEmpty())
        {
            Log.d(TAG, "key: \"" + k + "\" must be not empty string");
            return false;
        }
        return true;
    }

    private static boolean verify_key(KeyDesc k, JSONObject o) {

        String key = k.getKeyName();
        boolean b_is_ok = true;

        switch(k.getValueType()) {

            case KeyDesc.T_STR:
                b_is_ok = hasString(o, key);
                break;

            case KeyDesc.T_DOUBLE:
                b_is_ok = hasDouble(o, key);
                break;

            case KeyDesc.T_LONG:
                b_is_ok = hasLong(o, key);
                break;

            case KeyDesc.T_ARRAY:
                JSONArray a = o.optJSONArray(key);
                int len = k.getArrayLength();
                if(a==null || a.length() != len)
                {
                    Log.d(TAG, "array: \"" + key + "\" must have length:" + len);
                    return false;
                }

                switch(k.getArrayElType()) {
                    case KeyDesc.T_STR:
                        for(int i=0; i<len; ++i) {
                            String s = a.optString(i);
                            if(s==null || s.isEmpty()) {
                                Log.d(TAG, "element: " + i + "of array \"" + key + "\" must be not empty string");
                                return false;
                            }
                        }
                        break;
                    case KeyDesc.T_DOUBLE:
                        for(int i=0; i<len; ++i) {
                            Double d = a.optDouble(i);
                            if(d.isNaN()) {
                                Log.d(TAG, "element: " + i + " of array \"" + key + "\" must be not empty string");
                                return false;
                            }

                        }
                        break;
                    case KeyDesc.T_LONG:
                        for(int i=0; i<len; ++i) {
                            Long l = a.optLong(i);
                            if(l == 0) {
                                Log.d(TAG, "element: " + i + " of array \"" + key + "\" must be Long number and not 0");
                                return false;
                            }

                        }
                        break;
                    default:
                        Log.d(TAG, "Unrecognized array type for key: \"" + key + "\", just skipping");
                        return false;
                }
                break;
            default:
                Log.d(TAG, "Unrecognized type for key: \"" + key + "\", just skipping");
                return false;

        }
        return b_is_ok;
    }

    private static boolean verify(JSONObject o) {

        for(KeyDesc k: ourMandatoryKeys) {
            String key = k.getKeyName();

            if(false == o.has(key)) {
                Log.d(TAG, "Mandatory key: \"" + key + "\" is not present");
                return false;
            }

            if(!verify_key(k, o))
                return false;

        }
        return true;
    }

    public boolean sync(String mark) {
        JSONTokener tokener = new JSONTokener(mark);

        boolean b_were_conflicts = false;
        try {

            while(tokener.more()) {
                JSONObject object = (JSONObject) tokener.nextValue();
                if(verify(object)) {
                    b_were_conflicts |= sync(object, true);
                } else {
                    Log.d(TAG, "Malformed Mark object: " + object.toString() +" Skipping sync. Has format changed?");
                }
            }
        } catch(JSONException je) {
            Log.d(TAG, "error json parsing mark from string");
            je.printStackTrace();
        }

        return b_were_conflicts;
    }


    // true - were conflicts
    public boolean sync(JSONObject their, boolean b_verified ) {

        // TODO: check that every next added "their" has unique UUID
        // otherwise we may end up having several conflicts containing same UUIDs which has to be unique!
        // or just check that myOurs has no such UUID when adding from myConflicts

        if(!b_verified && verify(their)==false) {
            Log.d(TAG, "Trying to merge mark which has wrong JSON format");
            return false;
        }

        boolean b_were_conflicts = false;
        String uuid = null;
        try {
            uuid = their.getString(FLD_UUID);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        if(myOurs.containsKey(uuid)) {
            JSONObject our = myOurs.get(uuid);
            MergeInfo mi = merge(uuid, their, our);
            if(mi != null && mi.isAnythingChanged())
            {
                myOurs.remove(uuid);
                myConflicts.add(mi);
                b_were_conflicts = true;
            }
        } else {
            myNew.put(uuid, their);
        }

        return b_were_conflicts;
    }

    public int getNumConflicts() { return myConflicts.size(); }


    public void toBundle(Bundle outState) {

        String own_str = "";
        for(String uuid: myOurs.keySet()) {
            JSONObject o = myOurs.get(uuid);
            own_str += o.toString();
        }

        String new_str = "";
        if(myNew!=null) {
            for (String uuid : myNew.keySet()) {
                JSONObject o = myNew.get(uuid);
                new_str += o.toString();
            }
        }

        outState.putString("own", own_str);
        outState.putString("new", new_str);

        if(myConflicts!=null) {
            int idx = 0;
            outState.putInt("num_conflicts", myConflicts.size());
            for (MergeInfo md : myConflicts) {
                outState.putSerializable("md" + idx, md);
                idx += 1;
            }
        }

    }

    private void setNew(Map<String, JSONObject> jsons) {
        myNew = jsons;
    }
    private void setConflicts(List<MergeInfo> mi_list) {
        myConflicts = mi_list;
    }

    public static MarkMerger fromBundle(Bundle inState) {

        String[] our_str = { inState.getString("own") };
        String[] new_str = { inState.getString("new") };

        MarkMerger mm = new MarkMerger(our_str);

        List<JSONObject> tmp = MarkMerger.stringToJSON(new_str);
        Map<String, JSONObject> new_list = new HashMap<String, JSONObject>();

        try {
            for (JSONObject o : tmp) {
                new_list.put(o.getString(FLD_UUID), o);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mm.setNew(new_list);

        int num_conflicts = inState.getInt("num_conflicts");
        List<MergeInfo> mi_list = new ArrayList<MergeInfo>();
        for(int i=0; i<num_conflicts;++i) {
            MergeInfo mi = (MergeInfo)inState.getSerializable("md"+i);
            mi_list.add(mi);
        }
        mm.setConflicts(mi_list);

        return mm;
    }
}

