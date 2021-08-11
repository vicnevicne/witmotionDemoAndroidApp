package com.bt901;

import java.util.List;

/**
 * Created by 葛文博 on 2017/11/21.
 */
public class MenuGroup {
    private String name;
    private List<MenuItem> childList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MenuItem> getChildList() {
        return childList;
    }

    public void setChildList(List<MenuItem> childList) {
        this.childList = childList;
    }
}
