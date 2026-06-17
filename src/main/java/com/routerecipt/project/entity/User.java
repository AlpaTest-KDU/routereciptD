package com.routerecipt.project.entity;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;
import org.springframework.lang.Nullable;

import com.routerecipt.project.dto.Role;
import com.routerecipt.project.dto.UserDTO;
import com.routerecipt.project.dto.UserDTO.Gender;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자(User) JPA Entity
 *
 * - 기존 UserDTO + UserMapper.xml(MyBatis)을 대체하는 영속성 객체
 * - 회원가입/로그인 기능에서만 사용 (통계/분석 쪽은 여전히 MyBatis 유지)
 *
 * Persistable<String>을 구현한 이유:
 *  u_id를 사용자가 직접 입력해서 PK로 쓰기 때문에, 객체를 새로 만들어도
 *  @Id 값이 항상 채워져 있다. Spring Data JPA의 save()는 기본적으로
 *  "@Id가 null이면 신규(INSERT), 아니면 기존 row(UPDATE)"로 판단하는데
 *  이 기준대로면 회원가입(신규 INSERT)인데도 UPDATE(merge)로 동작해버린다.
 *  그래서 isNew 플래그로 신규 여부를 직접 판단하도록 오버라이드한다.
 */

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 스펙상 기본 생성자 필요, 외부에서 new로  막 만들지 못하게 protected 
public class User implements Persistable<String> {
	
	@Id
	@Column(name = "u_id", length = 16)
	private String u_id;
	
	@Id
	@Column(name = "u_pw", nullable = false)
	private String u_pw;
	
	@Id
	@Column(name = "u_name", nullable = false)
	private String u_name;
	
	@Temporal(TemporalType.DATE)
	@Column(name = "u_birthday")
	private Date u_birthday;
	
	@Column(name = "u_email", nullable = false)
	private String u_email;
	
	// 기존 MyBatis의 NOW() 대신, Hibernate가 INSERT 시점에 자동으로 채워줌
	@CreationTimestamp
	@Column(name = "u_date", updatable = false)
	private Date u_date;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "gender")
	private UserDTO.Gender gender;
	
	// DB에는 enum 이름 그대로("MALE"/"FEMALE") 저장되어 있어서 STRING으로 매핑
	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	private Role role;
	
	// DB컬럼이 아닌, "이 객체가 아직 한 번도 영속화되지 않은 신규 객체인가"를
	// JVM 메모리상에서만 추적하는 플래그, 새로 생성하면 무조건 ture로 시작
	@Transient
	@Getter(AccessLevel.NONE)
	private boolean isNew = true;

	@Builder
	public User(String u_id, String u_pw, String u_name, Date u_birthday, String u_email, Date u_date, Gender gender,
			Role role) {
		super();
		this.u_id = u_id;
		this.u_pw = u_pw;
		this.u_name = u_name;
		this.u_birthday = u_birthday;
		this.u_email = u_email;
		this.u_date = u_date;
		this.gender = gender;
		this.role = role;
	}
	
	
	@Override
	@Nullable
	public String getId() {
		return u_id;
	}
	
	@Override
	public boolean isNew() {
		return isNew;
	}
	
	// DB에서 select로 읽혀오거나(PostLoad), Insert가 끝나면(PostPersist)
	// 더 이상 "신규"가 아니므로 false로 전환 -> 이후 같은 객체를 save()하면 정상적으로 update됨
	
	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}
	
	public static User from(UserDTO dto, Role role) {
		return User.builder()
				.u_id(dto.getU_id())
				.u_pw(dto.getU_pw())
				.u_name(dto.getU_name())
				.u_birthday(dto.getU_birthday())
				.u_email(dto.getU_email())
				.gender(dto.getGender())
				.role(role)
				.build();
	}

	
	
	
	
	
	
	
	
	
}
