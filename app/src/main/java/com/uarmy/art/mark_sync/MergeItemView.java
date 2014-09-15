package com.uarmy.art.mark_sync;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.uarmy.art.bt_sync.R;
import com.uarmy.art.mark_merge.MergeInfo;

/**
 * Created by a on 9/6/14.
 */
public class MergeItemView extends LinearLayout {
    private TextView myLeft;
    private TextView myRight;
    private MergeFragment.FieldInfo myFieldInfo;

    public MergeItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.merge_list_item, this, true);
        setupChildren();
    }

    public static MergeItemView inflate(ViewGroup parent) {
        return (MergeItemView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.merge_item_view, parent, false);
    }

    private void setupChildren() {
        myLeft = (TextView) findViewById(R.id.merge_left);
        myRight= (TextView) findViewById(R.id.merge_right);
        myFieldInfo = null;
    }

    public void setItem(MergeFragment.FieldInfo fi) {
        // Lookup view for data population
        myLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myFieldInfo.setFieldSelection(false);
                view.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                myRight.setBackgroundColor(getResources().getColor(android.R.color.background_light));
            }
        });

        myRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myFieldInfo.setFieldSelection(true);
                view.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                myLeft.setBackgroundColor(getResources().getColor(android.R.color.background_light));
            }
        });

        myFieldInfo = fi;
        // Populate the data into the template view using the data object
        myLeft.setText(fi.myLeft);
        myRight.setText(fi.myRight);
    }

}
