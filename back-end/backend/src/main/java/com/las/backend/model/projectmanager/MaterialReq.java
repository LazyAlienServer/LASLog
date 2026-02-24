package com.las.backend.model.projectmanager;

import lombok.Data;

@Data
public class MaterialReq {
    private String filename;
    private int mx1, my1, mz1;
    private int mx2, my2, mz2;
    private boolean includeBuilt = true;
}