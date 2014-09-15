package com.uarmy.art.mark_sync;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;

/**
 * Created by a on 9/1/14.
 */
public class MarkListTabListener implements ActionBar.TabListener {
    private MarkListFragment mFragment;
    private final MarkSync mActivity;
    private final String mTag;

    /** Constructor used each time a new tab is created.
     * @param activity  The host Activity, used to instantiate the fragment
     * @param tag  The identifier tag for the fragment
     */
    public MarkListTabListener(MarkSync activity, String tag) {
        mActivity = activity;
        mTag = tag;
    }

    /* The following are each of the ActionBar.TabListener callbacks */

    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        if (mFragment == null) {
            // If not, instantiate and add it to the activity
            Bundle args = new Bundle();
            args.putString(MarkListFragment.ARG_TYPE, mTag);
            mFragment = (MarkListFragment)Fragment.instantiate(mActivity, MarkListFragment.class.getName(), args);
            ft.add(android.R.id.content, mFragment, mTag);

        } else {
            ft.attach(mFragment);

        }
        mActivity.setCurrentTab(mFragment);
    }

    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        if (mFragment != null) {
            // Detach the fragment, because another one is being attached
            ft.detach(mFragment);
        }
    }

    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // User selected the already selected tab. Usually do nothing.
    }
}