package com.las.backenduser.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.User;
import com.las.backenduser.model.dto.user.UserPageVO;
import com.las.backenduser.model.dto.user.UserVO;
import com.las.backenduser.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserPageVO getAllUsers(int page, int size, String search) {
        Page<User> pageParam = new Page<>(page, size);

        QueryWrapper<User> wrapper = new QueryWrapper<>();

        if (search != null && !search.trim().isEmpty()) {
            String keyword = "%" + search.trim() + "%";
            wrapper.and(w -> w
                    .like("uuid", search.trim())
                    .or().like("username", search.trim())
                    .or().apply("CAST(qq AS TEXT) LIKE {0}", keyword)
                    .or().apply("array_to_string(id_minecraft, ',') LIKE {0}", keyword)
            );
        }

        // 按注册时间倒序
        wrapper.orderByDesc("registerdate");

        Page<User> result = userMapper.selectPage(pageParam, wrapper);

        // 转换为 VO（脱敏）
        List<UserVO> voList = result.getRecords().stream()
                .map(this::toUserVO)
                .toList();

        UserPageVO pageVO = new UserPageVO();
        pageVO.setRecords(voList);
        pageVO.setTotal(result.getTotal());
        pageVO.setCurrent(result.getCurrent());
        pageVO.setSize(result.getSize());
        pageVO.setPages(result.getPages());

        return pageVO;
    }

    @Override
    public void updatePermission(String uuid, List<String> permission) {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("uuid", uuid);
        User user = new User();
        user.setPermission(permission);
        userMapper.update(user, wrapper);
    }

    /**
     * User → UserVO 脱敏转换
     */
    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setUuid(user.getUuid());
        vo.setUsername(user.getUsername());
        vo.setQq(user.getQq());
        vo.setMinecraftIds(user.getMinecraftIds());
        vo.setMinecraftUuids(user.getMinecraftUuids());
        vo.setMainMinecraftUuid(user.getMainMinecraftUuid());
        vo.setRegisterDate(user.getRegisterDate());
        vo.setStatus(user.getStatus());
        vo.setPermission(user.getPermission());
        vo.setWhitelist(user.getWhitelist());
        return vo;
    }
}

