package com.routerecipt.project.controller;


import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.routerecipt.project.dto.ItemCategory;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.dto.UploadResult;
import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.dto.Userdto.Gender;
import com.routerecipt.project.mapper.ReceiptMapper;
import com.routerecipt.project.mapper.UserMapper;
import com.routerecipt.project.service.ReceiptAnalyzeService;
import com.routerecipt.project.service.ReceiptApplicationService;
import com.routerecipt.project.service.ReceiptQueryService;
import com.routerecipt.project.service.ReceiptService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/receipt")
@RequiredArgsConstructor
public class ReceiptController {
	
	private final ReceiptService receiptService;
	
	private final ReceiptQueryService receiptQueryService;
	
	private final ReceiptAnalyzeService receiptAnalyzeService;
	
	@Autowired
	private final UserMapper usermapper;
	private final ReceiptMapper receiptmapper;
	
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


	 @PostMapping("/uploadReceipt")
	    public String uploadReceipt(
	            @RequestParam("receipt") List<MultipartFile> files,
	            Principal principal,
	            HttpSession session,
	            RedirectAttributes ra) {

	        // 1️⃣ 로그인 체크
	        if (principal == null) {
	            return "redirect:/login";
	        }

	        // 2️⃣ 파일 유효성 체크
	        if (files == null || files.isEmpty()) {
	            ra.addFlashAttribute("saveMsg", "파일을 선택해 주세요.");
	            return "redirect:/receipt/receiptRegisterPage";
	        }


	        // 3️⃣ Service 호출 (OCR + 저장 + 부분 성공 처리)
	        UploadResult result =
	                receiptService.uploadReceipts(files, principal.getName());

	        // 4️⃣ 결과 분기
	        if (result.isAllFailed()) {
	            ra.addFlashAttribute("saveMsg", "모든 영수증 분석에 실패했습니다.");
	            return "redirect:/receipt/receiptRegisterPage";
	        }

	        // 성공한 영수증 번호 세션 저장
	        session.setAttribute("recentReceiptNos",
	                result.getSuccessReceiptNos());

	        if (result.isPartialSuccess()) {
	            ra.addFlashAttribute(
	                "saveMsg",
	                "일부 영수증만 분석되었습니다. (성공 "
	                + result.getSuccessCount() + "건)"
	            );
	        } else {
	            ra.addFlashAttribute("saveMsg", "영수증 분석이 완료되었습니다.");
	        }

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
	     List<Long> recentNos =
	             (List<Long>) session.getAttribute("recentReceiptNos");

	     if (recentNos == null || recentNos.isEmpty()) {
	         model.addAttribute("receipts", Collections.emptyList());
	         return "receipt/receiptRegisterPage";
	     }

	     // 여기서 받아오는 receipts 안에, 저장된 “택시” 상품이 포함되어 있어야 함
	     List<ReceiptDTO> receipts = receiptQueryService.getRecentReceipts(recentNos);

	     model.addAttribute("receipts", receipts);
	     return "receipt/receiptRegisterPage";
	 }


	 @GetMapping("/writeReceipt")
	 public String writeReceiptForm(Model model, Principal principal) {

	     if (principal == null) {
	         return "redirect:/login";
	     }

	     ReceiptDTO receipt = new ReceiptDTO();
	     receipt.setR_u(principal.getName());
	     receipt.setR_place("");
	     receipt.setR_date(LocalDate.now());

	     model.addAttribute("receipt", receipt);
	     return "home";
	 }
    
	 @PostMapping("/analyze")
	 public String analyze(@RequestParam("receipt") MultipartFile file,
	                       Principal principal,
	                       HttpSession session,
	                       RedirectAttributes ra) {

	     if (principal == null) {
	         return "redirect:/login";
	     }

	     Long r_no =
	             receiptAnalyzeService.analyzeReceipt(
	                 file,
	                 principal.getName()
	             );

	     if (r_no == null) {
	         ra.addFlashAttribute("saveMsg", "OCR 결과를 만들지 못했습니다.");
	         return "redirect:/receipt";
	     }

	     @SuppressWarnings("unchecked")
	     List<Long> recent =
	             (List<Long>) session.getAttribute("recentReceiptNos");

	     if (recent == null) {
	         recent = new ArrayList<>();
	     }
	     recent.add(r_no);
	     session.setAttribute("recentReceiptNos", recent);

	     return "redirect:/receipt/receiptRegisterPage";
	 }
	 
	private final ReceiptMapper receiptWriteMapper;
	    
    private static final int RECENT_LIMIT = 50; // 최근 50개만 유지
    private final ConcurrentMap<String, Deque<Long>> recentNosByUser = new ConcurrentHashMap<>();
 
	 
    @PostMapping("/saveOne")
    @Transactional
    public String saveOneString(
            @RequestParam("r_place") String r_place,
            @RequestParam("r_price") String r_price,
            @RequestParam("r_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate r_date,

            @RequestParam("item_name") List<String> itemNames,
            @RequestParam("item_price") List<Integer> itemPrices,
            @RequestParam("item_category") List<String> itemCategories,

            Principal principal,
            HttpSession session,
            RedirectAttributes ra
    ) {
        // 1️⃣ 로그인 체크 (업로드와 동일한 스타일)
        if (principal == null) {
            return "redirect:/login";
        }
        String userId = principal.getName();

        // 2️⃣ 입력값 검증
        int n = (itemNames == null) ? 0 : itemNames.size();
        if (n == 0) {
            ra.addFlashAttribute("saveMsg", "상품이 1개 이상 필요합니다.");
            return "redirect:/receipt/receiptRegisterPage";
        }
        if (itemPrices == null || itemPrices.size() != n ||
            itemCategories == null || itemCategories.size() != n) {
            ra.addFlashAttribute("saveMsg", "상품 정보(이름/가격/카테고리) 개수가 맞지 않습니다.");
            return "redirect:/receipt/receiptRegisterPage";
        }

        // 3️⃣ ReceiptDTO 생성
        ReceiptDTO receipt = new ReceiptDTO();
        receipt.setR_u(userId);
        receipt.setR_place(r_place);
        receipt.setR_date(r_date);

        int price = Integer.parseInt(r_price.replaceAll("[^0-9]", ""));
        receipt.setR_price(price);

        // 4️⃣ gender 세팅 (Userdto.Gender -> 공용 Gender 변환)
        Userdto user = usermapper.UserSelectById(userId);
        if (user != null && user.getGender() != null) {
            com.routerecipt.project.common.Gender receiptGender =
                    com.routerecipt.project.common.Gender.valueOf(user.getGender().name());
            receipt.setGender(receiptGender);
        }

        // 5️⃣ items 구성
        List<ReceiptItemDTO> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ReceiptItemDTO it = new ReceiptItemDTO();
            it.setItem_name(itemNames.get(i));

            Integer p = itemPrices.get(i);
            it.setItem_price(p == null ? 0 : p);

            String cat = itemCategories.get(i);
            if (cat == null || cat.isBlank()) cat = ItemCategory.ETC.name();
            it.setItem_category(cat);

            items.add(it);
        }
        receipt.setItems(items);

        // (선택) 부모 category가 ReceiptDTO에 있을 때만 사용
        // receipt.setCategory(pickReceiptCategoryByMode(receipt));

        // 6️⃣ DB 저장 (부모 -> 자식)
        receiptmapper.insertReceipt(receipt);
        Long rNo = receipt.getR_no();

        if (rNo != null && !items.isEmpty()) {
            for (ReceiptItemDTO it : items) {
                it.setR_no(rNo);
            }
            receiptmapper.insertReceiptItems(rNo, items);
        }

        // 7️⃣ 업로드와 동일하게 세션 recentReceiptNos 갱신
        @SuppressWarnings("unchecked")
        List<Long> recentNos = (List<Long>) session.getAttribute("recentReceiptNos");
        if (recentNos == null) recentNos = new ArrayList<>();

        if (rNo != null) {
            // 중복 제거 후 맨 앞에 추가
            recentNos.remove(rNo);
            recentNos.add(0, rNo);

            // 너무 커지면 제한 (upload와 동일 정책 원하면 여기 숫자만 조정)
            while (recentNos.size() > 50) {
                recentNos.remove(recentNos.size() - 1);
            }

            session.setAttribute("recentReceiptNos", recentNos);
            ra.addFlashAttribute("saveMsg", "수기 영수증 저장이 완료되었습니다.");
        } else {
            ra.addFlashAttribute("saveMsg", "저장에 실패했습니다.");
        }

        return "redirect:/receipt/receiptRegisterPage";
    }
    
    private final ReceiptApplicationService receiptApplicationService;
    
    @PostMapping("/confirm")
    public String confirmReceipt(
            @RequestParam("r_no") Long r_no,
            @RequestParam("r_place") String r_place,
            @RequestParam("r_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate r_date,
            @RequestParam("r_price") Integer r_price,
            @RequestParam("item_names") List<String> item_names,
            @RequestParam("item_prices") List<Integer> item_prices,
            @RequestParam("item_categories") List<String> item_categories,
            RedirectAttributes ra,
            Principal principal
    ) {
        if (principal == null) return "redirect:/login";

        if (item_names.size() != item_prices.size() || item_names.size() != item_categories.size()) {
            ra.addFlashAttribute("saveMsg", "항목 데이터가 올바르지 않습니다.");
            return "redirect:/receipt/receiptRegisterPage";
        }

        receiptApplicationService.confirmReceipt(
                r_no, r_place, r_date, r_price,
                item_names, item_prices, item_categories
        );

        ra.addFlashAttribute("saveMsg", "영수증이 업데이트(확정)되었습니다.");
        return "redirect:/receipt/receiptRegisterPage";
    }





}
