package com.bt901;

import java.util.List;

/**
 * Created by 葛文博 on 2017/11/21.
 */
public class GroupBeen {
    private String name;
    private List<ChildBeen> childList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ChildBeen> getChildList() {
        return childList;
    }

    public void setChildList(List<ChildBeen> childList) {
        this.childList = childList;
    }
}
