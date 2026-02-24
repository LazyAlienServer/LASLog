package com.las.backend.model.projectmanager;

public class DataMsg {
    public final String filename;
    public final int mx1, my1, mz1;
    public final int mx2, my2, mz2;
    public final boolean includeBuilt;


    public DataMsg(String filename, int mx1, int my1, int mz1, int mx2, int my2, int mz2, boolean includeBuilt) {
        this.filename = filename;
        this.mx1 = mx1;
        this.my1 = my1;
        this.mz1 = mz1;
        this.mx2 = mx2;
        this.my2 = my2;
        this.mz2 = mz2;
        this.includeBuilt = includeBuilt;
    }
    public DataMsg(String filename){
        this.filename = filename;
        this.mx1 = 0;
        this.my1 = 0;
        this.mz1 = 0;
        this.mx2 = 0;
        this.my2 = 0;
        this.mz2 = 0;
        this.includeBuilt = true;
    }
}