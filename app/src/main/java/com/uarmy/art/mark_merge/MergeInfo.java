package com.uarmy.art.mark_merge;

import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by a on 8/31/14.
 */
public class MergeInfo implements Serializable {

    private static final long serialVersionUID = 1982112979501878067L;

    private final JSONObject myTheir;
    private final JSONObject myOur;
    private final String myUUID;

    private boolean myLocationChanged;
    private boolean myTitleChanged;
    private boolean myDescChanged;
    private boolean myModTimeChanged;

    private boolean myIsResolved = false;
    // <field name,  selection info>
    Map<String, Boolean> myFieldSelection = new HashMap<String, Boolean>(); // false or absent - accept ours, true - accept theirs

    MergeInfo(JSONObject their, JSONObject our, String uuid) {
        myTheir = their;
        myOur = our;
        myUUID = uuid;
        myLocationChanged = false;
        myTitleChanged = false;
        myDescChanged = false;
        myModTimeChanged = false;
    }


    public void setLocationChanged(boolean v) { myLocationChanged = v; }
    public void setTitleChanged(boolean v)  {  myTitleChanged = v; }
    public void setDescChanged(boolean v) { myDescChanged = v; }
    public void setModTimeChanged(boolean v) { myModTimeChanged = v; }

    public boolean getLocationChanged() { return myLocationChanged; }
    public boolean getTitleChanged()  {  return myTitleChanged; }
    public boolean getDescChanged() { return myDescChanged; }
    public boolean getModTimeChanged() { return myModTimeChanged; }

    public void setFieldSelection(String fld , boolean v) {
        myFieldSelection.put(fld, v);
    }

    public void markResolved(boolean v) { myIsResolved = v; }
    public boolean isMarkedResolved() { return myIsResolved; }


    public boolean isAnythingChanged() { return myModTimeChanged || myDescChanged || myTitleChanged || myLocationChanged; }

    public JSONObject getOur() { return myOur; }
    public JSONObject getTheir() { return myTheir; }
    public String getUUID() { return myUUID; }


    /**
     * Always treat de-serialization as a full-blown constructor, by validating
     * the final state of the de-serialized object.
     */
    private void readObject(ObjectInputStream aInputStream)
            throws ClassNotFoundException, IOException {
        // always perform the default de-serialization first
        aInputStream.defaultReadObject();

        // ensure that object state has not been corrupted or tampered with
        // malicious code
        //verifyUserDetails();
    }

    /**
     * This is the default implementation of writeObject. Customise if
     * necessary.
     */
    private void writeObject(ObjectOutputStream aOutputStream)
            throws IOException {
        // perform the default serialization for all non-transient, non-static
        // fields
        aOutputStream.defaultWriteObject();
    }

};
