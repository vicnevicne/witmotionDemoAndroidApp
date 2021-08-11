package com.bt901;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 葛文博 on 2017/11/21.
 */
public class ExLisViewAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<MenuGroup> groupList;

    public ExLisViewAdapter(Context context, List<MenuGroup> groups) {
        this.context = context;
        this.groupList = groups;
    }


    @Override
    public int getGroupCount() {
        return groupList.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return groupList.get(i).getChildList().size();
    }

    @Override
    public Object getGroup(int i) {
        return groupList.get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return groupList.get(i).getChildList().get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        view = LayoutInflater.from(context).inflate(R.layout.lay_group_item, viewGroup, false);
        TextView tvName = (TextView) view.findViewById(R.id.tvName);
        MenuGroup menuGroup = groupList.get(i);
        tvName.setText(menuGroup.getName());
        ImageView arrow = (ImageView) view.findViewById(R.id.iv_arrow);
        if (menuGroup.getChildList().size() > 0) {
            arrow.setVisibility(View.VISIBLE);
        } else {
            arrow.setVisibility(View.INVISIBLE);
        }
        if (b) {
            arrow.setImageResource(R.drawable.icon_open);
        } else {
            arrow.setImageResource(R.drawable.icon_close);
        }
        return view;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        view = LayoutInflater.from(context).inflate(R.layout.lay_rv_item, viewGroup, false);
        TextView tvName = (TextView) view.findViewById(R.id.tvName);
        if (groupList.get(i).getChildList() != null) {
            MenuItem menuItem = groupList.get(i).getChildList().get(i1);
            tvName.setText(menuItem.getName());
        }

        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }
}
