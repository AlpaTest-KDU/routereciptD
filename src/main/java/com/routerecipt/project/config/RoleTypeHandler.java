package com.routerecipt.project.config;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.routerecipt.project.dto.Role;


/**
 * Role Enum 전용 MyBatis TypeHandler
 *
 * 역할:
 *  - Java의 Role enum ↔ DB의 문자열(VARCHAR) 간 변환 처리
 *
 * DB 저장 형태 예:
 *  - Role.ADMIN  → "ADMIN"
 *  - Role.USER   → "USER"
 */
@MappedTypes(Role.class)
public class RoleTypeHandler extends BaseTypeHandler<Role>  {

	
	/**
     * Java → DB (INSERT / UPDATE 시 호출)
     *
     * @param ps        PreparedStatement
     * @param i         파라미터 인덱스
     * @param parameter Role enum 값 (null 아님)
     * @param jdbcType  JDBC 타입 (보통 VARCHAR)
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Role parameter, JdbcType jdbcType) throws SQLException {
        
    	// enum 이름을 문자열로 DB에 저장
        // 예: Role.ADMIN → "ADMIN"
    	ps.setString(i, parameter.name());
    }

    /**
     * DB → Java (컬럼명 기반 조회)
     */
    @Override
    public Role getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String role = rs.getString(columnName);
        return role == null ? null : Role.valueOf(role);
    }

    
    /**
     * DB → Java (컬럼 인덱스 기반 조회)
     */
    @Override
    public Role getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String role = rs.getString(columnIndex);
        return role == null ? null : Role.valueOf(role);
    }

    /**
     * DB → Java (Stored Procedure 결과)
     */
    @Override
    public Role getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String role = cs.getString(columnIndex);
        return role == null ? null : Role.valueOf(role);
    }
}