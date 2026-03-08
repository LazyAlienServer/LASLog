package com.las.backenduser.service

import com.las.backenduser.utils.result.Result
import java.io.Serializable

fun interface PasswordService {
    fun changePassword(uuid: String, oldPass: String, newPass: String): Result<Serializable>
}
