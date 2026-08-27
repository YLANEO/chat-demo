package com.ylaneo.chatserverweb.dao.impl;

import com.ylaneo.chatserverweb.dao.UserDao;
import com.ylaneo.chatserverweb.dto.RegisterDto;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository//这个注解表示：交给Spring管理，JdbcTemplate自动注入
public class UserDaoImpl implements UserDao {

    private final JdbcTemplate jdbcTemplate;

    public UserDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // User 行的映射器，把 ResultSet 的一行映射成 User 对象
    private final RowMapper<User> userRowMapper = (ResultSet rs, int rowNum) -> {
        User user = new User();
        user.setUserid(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setNickname(rs.getString("nickname"));
        user.setAvatar_url(rs.getString("avatar_url"));
        user.setStatus(rs.getInt("status"));
        return user;
    };

    @Override
    public Result register(RegisterDto registerDto) {
        String username = registerDto.getUsername();
        String password = registerDto.getPassword();
        String rePassword = registerDto.getRePassword();

        // 前端校验：两次密码是否一致
        if (!password.equals(rePassword)) {
            return Result.error("两次密码不一致");
        }

        String sql = "INSERT INTO user (username, password) VALUES (?, ?)";
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int rows = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, username);
                ps.setString(2, password);
                return ps;
            }, keyHolder);

            if (rows > 0) {
                System.out.println("注册成功");
                return Result.success("注册成功");
            }
            return Result.error("注册失败");
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return Result.error("用户名已存在");
        } catch (Exception e) {
            return Result.error("数据库错误: " + e.getMessage());
        }
    }

    @Override
    public Result login(String username, String password) {
        String sql = "SELECT * FROM user WHERE username = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            if (!user.getPassword().equals(password)) {
                return Result.error("密码错误");
            }
            System.out.println("登录成功: userId=" + user.getUserid());
            return Result.success(user);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Result.error("用户不存在");
        } catch (Exception e) {
            return Result.error("数据库错误: " + e.getMessage());
        }
    }

    @Override
    public Result updateUsername(Integer userId, String reUsername) {
        String sql = "UPDATE user SET username = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, reUsername, userId);
        if (rows > 0) {
            return Result.success("修改成功");
        }
        return Result.error("修改失败");
    }

    @Override
    public Result getUsername(Integer userId) {
        String sql = "SELECT username FROM user WHERE id = ?";
        try {
            String username = jdbcTemplate.queryForObject(sql, String.class, userId);
            User user = new User();
            user.setUsername(username);
            return Result.success(user);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Result.error("用户不存在");
        } catch (Exception e) {
            return Result.error("数据库错误: " + e.getMessage());
        }
    }

    @Override
    public Result updateUserStatus(Integer userId, Integer userStatus) {
        String sql = "UPDATE user SET status = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, userStatus, userId);
        if (rows > 0) {
            return Result.success("修改成功");
        }
        return Result.error("修改失败");
    }

    @Override
    public Result updateUserAvatar(Integer userId, String userAvatarUrl) {
        String sql = "UPDATE user SET avatar_url = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, userAvatarUrl, userId);
        if (rows > 0) {
            return Result.success("修改成功");
        }
        return Result.error("修改失败");
    }

    @Override
    public Result updateNickname(Integer userId, String newNickname) {
        String sql = "UPDATE user SET nickname = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, newNickname, userId);
        if (rows > 0) {
            return Result.success("修改成功");
        }
        return Result.error("修改失败");
    }

    @Override
    public Result<User> getUserData(Integer userId) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, userId);
            return Result.success(user);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Result.error("用户不存在");
        } catch (Exception e) {
            return Result.error("数据库错误: " + e.getMessage());
        }
    }
}