package com.routerecipt.project.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.Role;
import com.routerecipt.project.dto.UserDTO;
import com.routerecipt.project.entity.User;
import com.routerecipt.project.redis.BloomFilter.RedisBloomService;
import com.routerecipt.project.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회원(User) 관련 서비스 구현체
 *
 * - 기존 UserMapper(MyBatis) 의존을 모두 UserRepository(JPA)로 교체
 * - UserInfoUpdate, UserUpdatePW는 명시적인 update SQL 호출 없이
 *   변경감지(Dirty Checking)로 트랜잭션 종료 시 자동 반영됨
 */
@Service
@RequiredArgsConstructor
public class UserServiceImp implements UserService {
	
//	@Autowired
//	private UserMapper userMapper;
//	
//	@Autowired
//	private RedisBloomService bloomService;
//	
//	@Autowired
//	private PasswordEncoder passwordEncoder;
	
	private final UserRepository userRepository;
	private final RedisBloomService bloomService;
	private final PasswordEncoder passwordEncoder;
	
	// 사용자 아이디 중복 여부 확인
	/**
    * 동작:
    * 1) Bloom Filter에 "있다"면(중복 가능) → DB로 확정 검사(오탐 가능성 때문)
    * 2) Bloom Filter에 "없다"면 → DB 확인 후 없으면 Bloom Filter에 등록
    */
//	public boolean checkDuplicateUserId(String userId) {
//
//	    // 1단계: Bloom Filter 판단
//	    if (bloomService.isDuplicate(userId)) {
//	        // Bloom Filter는 false positive 가능 → DB로 확정 검사
//	        UserDTO user = userMapper.UserFindID(userId);
//	        return user != null; // true면 중복
//	    }
//
//	    // 2단계: Bloom Filter에 없음 → DB 확인
//	    UserDTO user = userMapper.UserFindID(userId);
//
//	    if (user == null) {
//	        // DB에도 없으면 Bloom Filter에 등록
//	        bloomService.register(userId);
//	        return false; // 중복 아님
//	    }
//
//	    return true; // DB에 존재 → 중복
//	}
	@Override
	public boolean checkDuplicateUserId(String userId) {
		if (bloomService.isDuplicate(userId)) {
			return userRepository.existsById(userId);
		}
		boolean exists = userRepository.existsById(userId);
		if (!exists) {
			bloomService.register(userId);
		}
		return exists;
	}

	
	// Spring Security 로그인 전용 사용자 조회
//	@Override
//	public UserDTO loadUserByUsername(String u_id) {
//		return userMapper.loadUserByUsername(u_id);
//	}
	
	@Override
	public User loadUserByUsername(String username) {
		return userRepository.findById(username).orElse(null);
	}
	
	// 회원가입 
	// User.from()으로 만든 객체는 isNew=true 상태라 save() 호출 시
	// merge가 아니라 persist(순수 INSERT)로 동작 -> 중복 PK면 정상적으로
	// DataIntegrityViolationException 발생 (컨트롤러의 기존 try-catch 그대로 유효)
//	@Override
//	@Transactional
//	public void UserSignUp(UserDTO u) {
//		userMapper.UserSignUp(u);
//	}
	
	@Override
	@Transactional
	public void UserSignUp(UserDTO u) {
		userRepository.save(User.from(u, Role.ROLE_USER));
		
	}
	
	// 이메일로 계정 찾기
//	@Override
//	public UserDTO UserFindID(String u_email) {
//		return userMapper.UserFindID(u_email);
//	}
	
	@Override
	public String UserfindID(String u_email) {
		return userRepository.findUserIdByEmail(u_email).orElse(null);
	}

	// 비밀번호 변경 전 계정 확인
//	@Override
//	public int UserCheckID(String u_id, String email) {
//		return userMapper.UserCheckID(u_id, email);
//	}
	@Override
	public boolean UserCheckID(String u_id, String email) {
		return userRepository.existByIdAndEmail(u_id, email);
	}
	
	// 비밀번호 변경
	// findById로 영속 상태(managed) 엔티티를 가져온 뒤 setter만 호출하면
	// 트랜잭션 커밋 시점에 변경감지가 UPDATE 쿼리를 자동 생성함 -> save() 불필요
//	@Override
//	public void UserUpdatePW(UserDTO u) {
//		String encodedPw = passwordEncoder.encode(u.getU_pw());
//		u.setU_pw(encodedPw);
//		userMapper.UserUpdatePW(u);		// DB 반영
//	}
	
	@Override
	@Transactional
	public void UserUpdatePW(UserDTO u) {
		User user = userRepository.findById(u.getU_id())
				.orElseThrow(() -> new IllegalAccessError("존재하지 않는 사용자 입니다." + u.getU_id()));
		user.setU_pw(passwordEncoder.encode(u.getU_pw()));
	}
	
	// 회원 탈퇴
//	@Override
//	public void UserInfoDelete(String u_id) {
//		userMapper.UserInfoDelete(u_id);
//	}
	
	@Override
	@Transactional
	public void UserInfoDelete(String u_id) {
		userRepository.deleteById(u_id);
	}
	
	// 회원 정보 id로 찾기
//	@Override
//	public UserDTO UserSelectById(String u_id) {
//		return userMapper.UserSelectById(u_id);
//	}
	
	@Override
	public User UserSelectById(String u_id) {
		return userRepository.findById(u_id).orElse(null);
	}
	
	// 회원 정보 수정
	// 회원 정보 수정 (이름, 이메일만) - 위와 동일하게 변경감지로 처리
//	@Override
//	public void UserInfoUpdate(UserDTO u) {
//		userMapper.UserInfoUpdate(u);
//	}
	@Override
	public void UserInfoUpdate(UserDTO u) {
		User user = userRepository.findById(u.getU_id())
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자 입니다." + u.getU_id()));
		user.setU_name(u.getU_name());
		user.setU_email(u.getU_email());
	}
}
