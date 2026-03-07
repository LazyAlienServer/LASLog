package com.las.backend.model.projectmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialReq {
    /**投影文件名**/
    private String filename;
    /**角点1**/
    private int mx1;
    private int my1;
    private int mz1;
    /**角点2**/
    private int mx2;
    private int my2;
    private int mz2;
    private boolean includeBuilt = true;
}