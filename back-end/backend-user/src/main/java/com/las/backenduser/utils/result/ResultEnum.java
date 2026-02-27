package com.las.backenduser.utils.result;

import lombok.Getter;

/**
 * @author sunyinuo
 */
@Getter
public enum ResultEnum {
    //常见code
    SUCCESS(200),
    FORBIDDEN(403),
    UNAUTHORIZED(401),
    NOT_FOUND(404);

    private final Integer code;

    ResultEnum(Integer code) {
        this.code = code;
    }

}
