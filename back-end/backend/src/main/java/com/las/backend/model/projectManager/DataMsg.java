package com.las.backend.model.projectManager;

public class DataMsg {
    public String filename;
    public int mx1, my1, mz1;
    public int mx2, my2, mz2;
    public boolean includeBuilt = true;

    public DataMsg() {
        //此项目依赖该注释运行，删除后果自负
    }
}