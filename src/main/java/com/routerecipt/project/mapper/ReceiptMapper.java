package com.routerecipt.project.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;




/**
 * 영수증(Receipt) / 영수증 항목(ReceiptItem) MyBatis Mapper
 *
 * 역할:
 *  - 영수증(부모) 저장/조회/수정
 *  - 영수증 항목(자식) 저장/조회/삭제(배치 포함)
 *
 * 주요 사용 흐름:
 *  1) OCR/수기 저장: insertReceipt -> insertReceiptItems(또는 insertItem)
 *  2) 등록페이지(최근 분석 목록): selectTempReceiptsByNos + selectItemsByReceiptNos
 *  3) 확정/수정(confirm): updateReceiptBasic -> deleteItemsByReceiptNo -> insertItemsBatch
 */
@Mapper
public interface ReceiptMapper {

	// =========================================================
    // 1) 저장(부모: receipt)
    // =========================================================

    /**
     * 영수증 1건 저장 (부모 테이블 insert)
     *
     * - 보통 r_no는 DB에서 자동 생성(PK auto increment/sequence)
     * - MyBatis에서 generatedKeys 설정이 되어 있으면
     *   insert 후 ReceiptDTO.r_no에 값이 채워짐
     *
     * @param r 저장할 영수증 DTO
     */
	void insertReceipt(ReceiptDTO r);
	
	
	/**
     * 특정 사용자(r_u)의 특정 월(yearMonth)에 해당하는 영수증 목록 조회
     *
     * @param r_u 사용자 ID(영수증 소유자)
     * @param yearMonth 조회할 연-월 문자열 (예: "2026-01")
     * @return 해당 월의 영수증 목록
     */
	List<ReceiptDTO> getSavedReceiptsDate(
	        @Param("r_u") String r_u,
	        @Param("yearMonth") String yearMonth
	    );
	
	
	// =========================================================
    // 2) 저장(자식: receipt_item)
    // =========================================================

    /**
     * 영수증 항목 1건 저장 (자식 테이블 단건 insert)
     *
     * - 배치 저장(insertReceiptItems / insertItemsBatch)이 있는 구조이므로
     *   단건 저장이 필요한 경우(테스트/특정 상황)에서 사용
     *
     * @param r 저장할 아이템 DTO
     */
	void insertItem (ReceiptItemDTO r);
	
	
	/**
     * 영수증 항목 여러 건 저장 (자식 테이블 batch insert)
     *
     * - 서비스에서 items 각각에 r_no(FK)를 세팅한 뒤 호출하는 방식이 일반적
     * - 반환값은 insert된 row 수(영향받은 행 수)
     *
     * @param rNo   부모 영수증 번호(FK)
     * @param items 저장할 항목 리스트
     * @return 영향받은 행 수
     */
    int insertReceiptItems(@Param("r_no") Long rNo,
                           @Param("items") List<ReceiptItemDTO> items);

    
    
    // =========================================================
    // 3) 등록 페이지 출력용 조회(최근 분석한 영수증만)
    //    - 부모/자식을 분리 조회한 뒤 서비스에서 합치는 방식 권장
    // =========================================================

    /**
     * r_no 목록으로 영수증(부모) 목록 조회
     *
     * - "최근 분석한 영수증만 보여주기" 기능에서 사용
     * - items는 join으로 한번에 가져오기보다 별도 조회(selectItemsByReceiptNos) 후
     *   서비스에서 붙이는 방식이 중복을 줄이고 관리가 쉬움
     *
     * @param nos 영수증 번호 목록 (IN 조건)
     * @return 영수증 목록(부모)
     */
    List<ReceiptDTO> selectTempReceiptsByNos(@Param("nos") List<Long> nos);
    
    
    /**
     * r_no 목록으로 영수증 항목(자식) 목록 조회
     *
     * - selectTempReceiptsByNos 결과에 items를 붙이기 위해 사용
     * - 서비스에서 r_no 기준으로 그룹핑(Map<Long, List<ReceiptItemDTO>>)하여
     *   각 ReceiptDTO.items에 세팅하는 방식이 일반적
     *
     * @param nos 영수증 번호 목록 (IN 조건)
     * @return 해당 영수증들의 전체 아이템 목록(자식)
     */
    List<ReceiptItemDTO> selectItemsByReceiptNos(@Param("nos") List<Long> nos);
    
    
    
    // =========================================================
    // 4) 확정(confirm) / 수정 업데이트 흐름
    //    - 부모 기본정보 업데이트 -> 자식 전삭제 -> 자식 재삽입
    // =========================================================

    /**
     * 영수증 기본정보 업데이트(부모 테이블 update)
     *
     * @param r_no    영수증 번호(PK)
     * @param r_place 상호명
     * @param r_date  거래 날짜
     * @param r_price 총액
     * @re
     */
    int updateReceiptBasic(
            @Param("r_no") Long r_no,
            @Param("r_place") String r_place,
            @Param("r_date") LocalDate r_date,
            @Param("r_price") Integer r_price
    );

    
    /**
     * 특정 영수증의 모든 아이템(자식) 삭제
     *
     * - confirm 시 기존 OCR 추출 항목을 통째로 교체하는 전략에서 사용
     *
     * @param r_no 영수증 번호(FK)
     * @return 삭제된 행 수
     */
    int deleteItemsByReceiptNo(@Param("r_no") Long r_no);

    
    /**
     * 아이템 리스트를 이름/가격/카테고리 3개 리스트로 받아 batch insert
     *
     * - 컨트롤러에서 길이(item_names, item_prices, item_categories)가
     *   동일한지 검증 후 호출해야 인덱스 매칭이 깨지지 않음
     *
     * @param r_no            부모 영수증 번호(FK)
     * @param item_names      아이템명 리스트
     * @param item_prices     아이템가격 리스트
     * @param item_categories 아이템카테고리 리스트
     * @return 영향받은 행 수
     */
    int insertItemsBatch(
            @Param("r_no") Long r_no,
            @Param("item_names") List<String> item_names,
            @Param("item_prices") List<Integer> item_prices,
            @Param("item_categories") List<String> item_categories
    );
    
    List<ReceiptDTO> selectRecentReceiptsByUser(
            @Param("userId") String userId,
            @Param("limit") int limit
    );
    
    void updateOcrStatus(@Param("r_no") Long r_no,
            @Param("status") String status);
    
    void updateOcrStatusByImagePath(@Param("imagePath") String imagePath,
            @Param("status") String status);
    
    void insertPendingReceipt(ReceiptDTO receipt);
    
    String selectImagePathByReceiptNo(@Param("receiptNo") Long receiptNo);
    
    ReceiptDTO selectReceiptByNo(Long r_no);
    
}
