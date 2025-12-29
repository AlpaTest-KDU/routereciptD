package com.routerecipt.project.config;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.routerecipt.project.dto.Role;

@MappedTypes(Role.class)
public class RoleTypeHandler extends BaseTypeHandler<Role>  {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Role parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public Role getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String role = rs.getString(columnName);
        return role == null ? null : Role.valueOf(role);
    }

    @Override
    public Role getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String role = rs.getString(columnIndex);
        return role == null ? null : Role.valueOf(role);
    }

    @Override
    public Role getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String role = cs.getString(columnIndex);
        return role == null ? null : Role.valueOf(role);
    }
}