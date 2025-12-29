package com.routerecipt.project.controller;


import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.routerecipt.project.dto.ItemCategory;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.mapper.ReceiptMapper;
import com.routerecipt.project.mapper.UserMapper;
import com.routerecipt.project.ocr.OcrService;
import com.routerecipt.project.ocr.ReceiptSaveService;
import com.routerecipt.project.ocr.ServiceIMP;

import com.routerecipt.project.controller.ReceiptController;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/receipt")
@RequiredArgsConstructor
public class ReceiptController {
	
	// 영수증 등록 기능
	@PostMapping("/receiptRegisterPage/ReceiptRegister")
	public String receiptRegister() {
		return "receiptRegisterPage";
	}
	
	// 영수증 삭제 기능
	@DeleteMapping("/userInfoShowPage/receiptInfoDelete/{receiptNo}")
	public String receiptInfoDelete(@PathVariable(value="receiptNo") Long receiptNo) {
		return "userInfoShowPage";
	}
	
	@PostMapping("/userInfoShowPage/receiptInfoShow")
	public String receiptInfoShow() {
		return "userInfoShowPage";
	}
	
	private final ReceiptMapper receiptMapper;           // ✅ 조회는 mapper로 바로 해도 됨
	
	private final OcrService ocrService;
    
    @Autowired
    private ServiceIMP serviceImp;
    
    private final UserMapper userMapper;
    
    private final ReceiptSaveService receiptSaveService; // ✅ 이게 없어서 터진 것

    /**
     * 달력 셀(하루) 표현용 DTO
     * - 프로젝트에 DayDTO가 없어서 터졌으니, 우선 내부 클래스로 정의해 컴파일/부팅부터 정상화
     */
    public static class DayDTO {
        private int day;                       // 1~31
        private List<ReceiptDTO> receipts;      // 그 날짜의 영수증들

        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }

        public List<ReceiptDTO> getReceipts() { return receipts; }
        public void setReceipts(List<ReceiptDTO> receipts) { this.receipts = receipts; }
    }
    
    private String pickReceiptCategoryByMode(ReceiptDTO receipt) {
        if (receipt == null || receipt.getItems() == null || receipt.getItems().isEmpty()) {
            return "ETC"; // 또는 "미분류"
        }

        // 카테고리별 개수 카운트
        Map<String, Integer> countByCat = new HashMap<>();
        for (ReceiptItemDTO it : receipt.getItems()) {
            if (it == null) continue;

            String cat = it.getItem_category();
            if (cat == null || cat.isBlank()) cat = "ETC";

            countByCat.merge(cat, 1, Integer::sum);
        }

        if (countByCat.isEmpty()) return "ETC";

        // 1) ETC 제외하고 최빈값 먼저 시도
        Optional<Map.Entry<String, Integer>> bestNonEtc = countByCat.entrySet().stream()
                .filter(e -> !"ETC".equalsIgnoreCase(e.getKey()))
                .max(Map.Entry.comparingByValue());

        if (bestNonEtc.isPresent()) {
            return bestNonEtc.get().getKey();
        }

        // 2) 전부 ETC면 ETC
        return countByCat.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("ETC");
    }

    
    

    @PostMapping("/uploadReceipt")
    @Transactional
    public String uploadReceipt(@RequestParam("receipt") List<MultipartFile> files,
                                Principal principal,
                                HttpSession session,
                                RedirectAttributes ra) {

        String userId = (principal != null) ? principal.getName() : null;
        if (userId == null) return "redirect:/login";

        if (files == null || files.isEmpty()) {
            ra.addFlashAttribute("saveMsg", "파일을 선택해 주세요.");
            return "redirect:/receipt/receiptRegisterPage";
        }

        List<Long> recentNos = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty() || file.getSize() == 0) continue;

            // 1) CLOVA
            JSONObject json = ocrService.callClovaOCR(file);
            if (json == null) continue;

            // 2) OCR 파싱 + 보강(텍스트/비전 포함)
            ReceiptDTO receipt = ocrService.parseReceiptWithAssist(json, file);
            if (receipt == null) continue;

            // 3) 부모(Receipt) 필수값 세팅
            receipt.setR_u(userId);

         // ✅ item_category null 방지 (DB NOT NULL 때문에)
            if (receipt.getItems() != null) {
                for (ReceiptItemDTO it : receipt.getItems()) {
                    if (it.getItem_category() == null || it.getItem_category().isBlank()) {
                        it.setItem_category(ItemCategory.ETC.name());
                    }
                }
            }

            // ✅ receipt.category = receipt_item의 대표 카테고리(최빈값)
            receipt.setCategory(pickReceiptCategoryByMode(receipt));

            // r_goods null 방어
            if (receipt.getR_goods() == null) receipt.setR_goods("");
            
         // 로그인 유저 정보 조회
            Userdto user = userMapper.UserSelectById(userId);

            // gender 세팅
            if (user != null && user.getGender() != null) {
                receipt.setGender(null);
            }

            // 4) 부모 insert
            receiptMapper.insertReceipt(receipt);
            Long rNo = receipt.getR_no();

            // 5) 자식 items 다건 insert  ✅ 여기(이 블록)에 너 코드 넣음
            if (rNo != null && receipt.getItems() != null && !receipt.getItems().isEmpty()) {

                // 🔽 여기 넣는 거임 (카테고리 null 방지 + r_no 세팅)
                for (ReceiptItemDTO it : receipt.getItems()) {
                    it.setR_no(rNo);

                    if (it.getItem_category() == null || it.getItem_category().isBlank()) {
                        it.setItem_category("ETC"); // 또는 ItemCategory.ETC.name()
                    }
                }

                // 실제 INSERT
                receiptMapper.insertReceiptItems(rNo, receipt.getItems());

                // (선택) 부모 category도 "아이템 카테고리"로 맞추고 싶으면 여기서 계산해서 update까지 해야 함
                // 지금 코드는 부모 insert를 이미 했기 때문에, 반영하려면 UPDATE 쿼리/매퍼가 필요함.
            }

            if (rNo != null) recentNos.add(rNo);
        }

        if (recentNos.isEmpty()) {
            ra.addFlashAttribute("saveMsg", "분석할 영수증이 없거나 OCR 결과가 비어있습니다.");
            return "redirect:/receipt/receiptRegisterPage";
        }

        session.setAttribute("recentReceiptNos", recentNos);
        ra.addFlashAttribute("saveMsg", "영수증 분석이 완료되었습니다. 목록에서 선택해 확인하세요.");
        return "redirect:/receipt/receiptRegisterPage";
    }




    // =========================
    // 2) 페이지 조회: "이번에 분석한 것만" 보여주기
    // =========================
    @GetMapping("/receiptRegisterPage")
    public String receiptRegisterPage(Principal principal,
                                      HttpSession session,
                                      Model model) {

        if (principal == null) return "redirect:/login";

        @SuppressWarnings("unchecked")
        List<Long> recentNos = (List<Long>) session.getAttribute("recentReceiptNos");

        if (recentNos == null || recentNos.isEmpty()) {
            model.addAttribute("receipts", Collections.emptyList());
            return "receiptRegisterPage";
        }

        // 1) receipt 조회
        List<ReceiptDTO> receipts = receiptMapper.selectTempReceiptsByNos(recentNos);

        if (receipts == null || receipts.isEmpty()) {
            model.addAttribute("receipts", Collections.emptyList());
            return "receiptRegisterPage";
        }

        // 2) items를 r_no IN (...) 으로 한 번에 조회
        List<ReceiptItemDTO> items = receiptMapper.selectItemsByReceiptNos(recentNos);

        // 3) r_no로 묶어서 각 receipt에 setItems
        Map<Long, List<ReceiptItemDTO>> itemMap =
                (items == null) ? Collections.emptyMap()
                        : items.stream().collect(Collectors.groupingBy(ReceiptItemDTO::getR_no));

        for (ReceiptDTO r : receipts) {
            r.setItems(itemMap.getOrDefault(r.getR_no(), Collections.emptyList()));
        }

        // 4) 업로드 순서 유지 정렬 (indexOf 반복 제거)
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < recentNos.size(); i++) {
            order.put(recentNos.get(i), i);
        }

        receipts.sort(Comparator.comparingInt(r -> order.getOrDefault(r.getR_no(), Integer.MAX_VALUE)));

        model.addAttribute("receipts", receipts);
        return "receiptRegisterPage";
    }



    
    
    






    @GetMapping("/writeReceipt")
    public String writeReceiptForm(Model model, Principal principal) {

        ReceiptDTO receipt = new ReceiptDTO();
        receipt.setR_u(principal.getName());
        receipt.setR_place("");

        
        receipt.setR_date(LocalDate.now());
        

        model.addAttribute("receipt", receipt);
        return "home";
    }
    
   
    
    
    @PostMapping("/analyze")
    @Transactional
    public String analyze(@RequestParam("receipt") MultipartFile file,
                          Principal principal,
                          HttpSession session,
                          RedirectAttributes ra) {

        String userId = principal.getName();

        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("saveMsg", "파일이 없습니다.");
            return "redirect:/receipt";
        }

        // 1) OCR
        JSONObject json = ocrService.callClovaOCR(file);
        ReceiptDTO temp = ocrService.parseReceiptWithAssist(json, file);
        if (temp == null) {
            ra.addFlashAttribute("saveMsg", "OCR 결과를 만들지 못했습니다.");
            return "redirect:/receipt";
        }

        // 2) TEMP receipt 저장 0(부모 먼저)
        temp.setR_u(userId);
        temp.setCategory("TEMP"); // TEMP 표시
        receiptMapper.insertReceipt(temp);   // ✅ insertTempReceipt → insertReceipt
        Long rNo = temp.getR_no();

        // 3) TEMP items 저장
        if (temp.getItems() != null && !temp.getItems().isEmpty()) {
            receiptMapper.insertReceiptItems(rNo, temp.getItems());
        }

        // 4) 세션에 이번 분석 rNo들 저장
        @SuppressWarnings("unchecked")
        List<Long> recent = (List<Long>) session.getAttribute("recentReceiptNos");
        if (recent == null) recent = new ArrayList<>();
        recent.add(rNo);
        session.setAttribute("recentReceiptNos", recent);

        return "redirect:/receipt/receiptRegisterPage";
    }


}
