package com.routerecipt.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;

@Mapper
public interface ReceiptMapper {

	/**
     * 📌 OCR 결과 영수증 저장
     */
	
	void insertReceipt(ReceiptDTO r);
	
	List<ReceiptDTO> getSavedReceiptsDate(
	        @Param("r_u") String r_u,
	        @Param("yearMonth") String yearMonth
	    );
	
	void insertItem (ReceiptItemDTO r);
	
	
	// ====== TEMP 저장 ======
    

    int insertReceiptItems(@Param("r_no") Long rNo,
                           @Param("items") List<ReceiptItemDTO> items);

    // ====== 등록페이지 출력용 조회 ======
    // 방법 A) receipt 목록 조회 + items는 별도 조회해서 서비스에서 붙이기(권장)
    List<ReceiptDTO> selectTempReceiptsByNos(@Param("nos") List<Long> nos);
    List<ReceiptItemDTO> selectItemsByReceiptNos(@Param("nos") List<Long> nos);
    
    
    
    
}
