package com.routerecipt.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.routerecipt.project.entity.User;

/**
 * 사용자(User) JPA Repository
 *
 * - 기존 UserMapper(MyBatis)를 대체
 * - PK(u_id) 기반 조회/삭제/저장은 JpaRepository가 기본 제공하는
 *   existsById / findById / deleteById / save를 그대로 사용
 * - u_id, u_email처럼 필드명에 _가 들어가면 메서드 이름 자동 파싱
 *   (existsByU_email 같은 형태)이 어색해져서, 이메일 관련 조회는
 *   명시적으로 @Query(JPQL)로 작성
 */

public interface UserRepository extends JpaRepository<User, String> {
	
	// 이메일 중복 여부 확인 (현재는 미사용, 추후 이메일 기반 가입제한 등에 쓸 수 있어 남겨둠)
	@Query("select (count(u) > 0) from User u where u.u_email = :email")
	boolean existsByEmail(@Param("email") String email);
	
	// 아이디(ID) 찾기 기능 - 이메일로 u_id 조회
	// 기존 UserMapper.UserFindID가 인터페이스 반환형(UserDTO)과 XML resulType(string)이
	// 불일치하던 버그를 여기서 Optional<String)으로 명확하게 고침
	@Query("select u.u_id from User u where u.u_email = :email")
	Optional<String> findUserIdByEmail(@Param("email") String email);
	
	// 비밀번호 변경 전 본인 확인용 - 아이디 + 이메일 일치 여부
	@Query("select (count(u) > 0) from User u where u.u_id= :uId and u.u_email= :email")
	boolean existByIdAndEmail(@Param("uId") String uId, @Param("email") String email);

}
