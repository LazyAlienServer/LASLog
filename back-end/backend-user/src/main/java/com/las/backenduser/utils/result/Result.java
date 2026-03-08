package com.las.backenduser.utils.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author sunyinuo
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T extends Serializable> implements Serializable {
    private Integer code;
    private String msg;
    private T data;
}
