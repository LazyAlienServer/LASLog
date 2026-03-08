package com.las.backenduser.service

import com.las.backenduser.utils.result.Result
import java.io.Serializable

fun interface RenameService {
    fun rename(uuid: String, newName: String): Result<Serializable>
}