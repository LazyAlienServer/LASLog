package com.las.backenduser.utils.salt;



import com.las.backenduser.model.Password;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.UUID;
public class Salt {
    public static Password salt(String password) {
        String saltValue = UUID.randomUUID().toString();
        return new Password(DigestUtils.sha256Hex(saltValue+password),saltValue);
    }
    private Salt(){
        //INOP
    }
}
