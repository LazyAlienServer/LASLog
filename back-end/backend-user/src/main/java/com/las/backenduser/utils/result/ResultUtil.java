package com.las.backenduser.utils.result;


import java.io.Serializable;

/**
 * @author sunyinuo
 */
public class ResultUtil {
    /**
     * HaveData
     * @param code 包装类
     * @param object 数据
     * @param msg 消息
     * @return 包装类
     */
    public static <T extends Serializable> Result<T> result(Integer code, T object, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(object);
        return result;
    }
    /**
     * NoData
     * @param code 状态码
     * @param msg 消息
     * @return 包装类
     */
    public static Result<Serializable> result(Integer code, String msg) {
        // 强制转型或依靠编译器推导
        return result(code, (Serializable) null, msg);
    }

    private ResultUtil() {
        //INOP
    }
}
