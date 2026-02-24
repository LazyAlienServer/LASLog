package com.las.backend.utils.result;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author sunyinuo
 */
@Data
@AllArgsConstructor
public class Result {
    private Integer code;
    private String msg;
    private Object data;

    public Result() {
        super();
    }

}
