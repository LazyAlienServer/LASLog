package com.las.backenduser.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.las.backenduser.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
