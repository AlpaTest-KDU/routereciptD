package com.routerecipt.project.ocr;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.OpenAI.OpenAiOcrAssisService;
import com.routerecipt.project.OpenAI.OpenAiReceiptResult;
import com.routerecipt.project.dto.ByteArrayMultiPartFile;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;


/**
 * OCR 서비스
 *
 * 역할:
 *  1) CLOVA OCR 호출(JSON + Base64)
 *  2) CLOVA 결과를 ReceiptDTO로 파싱
 *  3) 파싱 결과에 누락(상호/날짜/총액/아이템)이 있으면 OpenAI로 보강
 *  4) 업로드 이미지가 WEBP면 JPG로 변환 후 CLOVA로 전달
 *
 * 주로 사용되는 흐름:
 *  - callClovaOCR(file) -> parseReceiptWithAssist(json, file) -> ReceiptDTO 반환
 */
@Service
@RequiredArgsConstructor
public class OcrService {

	// CLOVA OCR API URL (application.yml/properties에서 주입)
    @Value("${clova.url}")
    private String clovaUrl;

    // CLOVA OCR SECRET (application.yml/properties에서 주입)
    @Value("${clova.secret}")
    private String clovaSecret;

    // CLOVA OCR 호출용 RestTemplate (현재는 new로 생성)
    private final RestTemplate clovaRestTemplate;
    // OCR 누락 보강용 OpenAI Assist 서비스
    private final OpenAiOcrAssisService openAiOcrAssistService;


    /**
     * (선택) ImageIO 플러그인 스캔
     * - WEBP 지원 플러그인 존재 여부를 로깅
     * - 운영에서 WEBP 변환이 계속 실패하면 여기 로그로 확인 가능
     */
    public ReceiptDTO parseReceiptWithAssist(JSONObject json, MultipartFile file) {
    	
    	// 1) CLOVA 결과를 ReceiptDTO로 1차 파싱
        ReceiptDTO parsed = parseReceipt(json);
        
        // 2) 핵심 값이 부족하면 OpenAI로 보강 시도
        if (needOpenAiFix(parsed)) {
        	
        	// CLOVA JSON에서 "필드 텍스트" 및 "receipt.result"를 기반으로 rawText 구성
            String rawText = extractRawText(json);
            System.out.println("[RAW_TEXT]\n" + rawText);

            if (!isBlank(rawText)) {

                // 현재 네 서비스가 text 기반 보강을 하도록 되어있다면 그대로 사용
                // (Vision까지 하려면 fixMissingFieldsWithVision(file, rawText)로 변경)
                OpenAiReceiptResult fixed = openAiOcrAssistService.fixMissingFieldsWithVision(file, rawText);

                if (fixed != null) {
                    // (1) 상호 보강
                    if (isBlank(parsed.getR_place()) && !isBlank(fixed.getPlace())) {
                        parsed.setR_place(fixed.getPlace().trim());
                    }

                    // (2) 날짜 보강 (문자열 -> LocalDate 파싱)
                    if (parsed.getR_date() == null && !isBlank(fixed.getDate())) {
                        try {
                            parsed.setR_date(LocalDate.parse(fixed.getDate().trim()));
                        } catch (Exception ignore) {}
                    }

                    // (3) 총액 보강
                    if (parsed.getR_price() <= 0 && fixed.getTotal() != null && fixed.getTotal() > 0) {
                        parsed.setR_price(fixed.getTotal());
                    }

                    // (4) 아이템 보강: CLOVA 아이템이 비었을 때만 AI 아이템을 채움
                    if (parsed.getItems() == null || parsed.getItems().isEmpty()) {
                        List<ReceiptItemDTO> fromAi = convertOpenAiItemsToReceiptItems(fixed);
                        if (!fromAi.isEmpty()) {
                            parsed.setItems(fromAi);
                        }
                    }
                }
            }
        }

        return parsed;
    }
    
    // CLOVA JSON에서 raw 텍스트를 최대한 추출
    // OpenAI 보강 단계에서 텍스트 힌트를 최대한 제공하기 위함
    @PostConstruct
    public void initImageIO() {
        ImageIO.scanForPlugins();
        boolean webpReadable = ImageIO.getImageReadersByFormatName("webp").hasNext();
        System.out.println("[ImageIO] webp reader available = " + webpReadable);
    }

    private String extractRawText(JSONObject json) {
        if (json == null) return "";

        StringBuilder sb = new StringBuilder();

        JSONObject img0 = null;
        JSONArray images = json.optJSONArray("images");
        if (images != null) img0 = images.optJSONObject(0);
        if (img0 == null) return "";

        // 1) fields 기반으로 전체 인식 텍스트 수집
        JSONArray fields = img0.optJSONArray("fields");
        if (fields != null) {
            for (int i = 0; i < fields.length(); i++) {
                JSONObject f = fields.optJSONObject(i);
                if (f == null) continue;
                String t = f.optString("inferText", "");
                if (!t.isBlank()) sb.append(t).append("\n");
            }
        }

        // 2) receipt.result 기반 보강 (구조화된 값들)
        JSONObject receipt = img0.optJSONObject("receipt");
        JSONObject result = (receipt == null) ? null : receipt.optJSONObject("result");
        if (result != null) {
            String store = optTextString(result, "storeInfo", "name");
            String dateText = optTextString(result, "paymentInfo", "date");
            String totalText = optTextString(result, "totalPrice", "price");

            if (!store.isBlank()) sb.append(store).append("\n");
            if (!dateText.isBlank()) sb.append(dateText).append("\n");
            if (!totalText.isBlank()) sb.append(totalText).append("\n");
            
            // subResults -> items 반복
            JSONArray subResults = result.optJSONArray("subResults");
            if (subResults != null) {
                for (int b = 0; b < subResults.length(); b++) {
                    JSONObject block = subResults.optJSONObject(b);
                    if (block == null) continue;
                    JSONArray items = block.optJSONArray("items");
                    if (items == null) continue;

                    for (int j = 0; j < items.length(); j++) {
                        JSONObject item = items.optJSONObject(j);
                        if (item == null) continue;

                        String name = optTextString(item, "name");
                        String price = optTextString(item, "price");
                        if (!name.isBlank() || !price.isBlank()) {
                            sb.append(name).append(" ").append(price).append("\n");
                        }
                    }
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * OpenAI 보강이 필요한지 판단
     * - 상호/날짜/총액/아이템 중 하나라도 누락이면 true
     */
    private boolean needOpenAiFix(ReceiptDTO r) {
        if (r == null) return true;
        return isBlank(r.getR_place())
                || r.getR_date() == null
                || r.getR_price() <= 0
                || r.getItems() == null
                || r.getItems().isEmpty();
    }


    // 2) CLOVA JSON -> ReceiptDTO 파싱
    public ReceiptDTO parseReceipt(JSONObject json) {
        ReceiptDTO dto = new ReceiptDTO();
        if (json == null) return dto;

        try {
            JSONObject result = json
                    .getJSONArray("images")
                    .getJSONObject(0)
                    .getJSONObject("receipt")
                    .getJSONObject("result");

            // 1) 상호명
            String place = optTextString(result, "storeInfo", "name");
            dto.setR_place(isBlank(place) ? null : place.trim());
            
            // 2) 날짜 (formatted 우선, 없으면 text 기반 파싱)
            LocalDate date = null;

            JSONObject paymentInfo = result.optJSONObject("paymentInfo");
            JSONObject dateObj = (paymentInfo == null) ? null : paymentInfo.optJSONObject("date");
            JSONObject formatted = (dateObj == null) ? null : dateObj.optJSONObject("formatted");

            if (formatted != null) {
                String y = formatted.optString("year", "");
                String m = formatted.optString("month", "");
                String d = formatted.optString("day", "");
                if (!y.isEmpty() && !m.isEmpty() && !d.isEmpty()) {
                    date = LocalDate.of(Integer.parseInt(y), Integer.parseInt(m), Integer.parseInt(d));
                }
            }

            if (date == null) {
                String dateStr = optTextString(result, "paymentInfo", "date");
                date = parseLocalDateFlexible(dateStr);
            }

            dto.setR_date(date);
            
            // 3) 총액
            String totalStr = optTextString(result, "totalPrice", "price");
            dto.setR_price(toIntMoney(totalStr));
            
            // 4) 아이템 목록
            List<ReceiptItemDTO> itemList = new ArrayList<>();
            List<String> goodsNames = new ArrayList<>();

            JSONArray subResults = result.optJSONArray("subResults");
            if (subResults != null && subResults.length() > 0) {
                for (int b = 0; b < subResults.length(); b++) {
                    JSONObject block = subResults.optJSONObject(b);
                    if (block == null) continue;

                    JSONArray items = block.optJSONArray("items");
                    if (items == null) continue;

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject itemObj = items.optJSONObject(i);
                        if (itemObj == null) continue;
                        
                        // 상품명
                        String name = optTextString(itemObj, "name");
                        if (isBlank(name)) continue;
                        name = name.trim();
                        
                        // 가격(구조가 복잡할 수 있어 최대한 안전하게 접근)
                        String priceStr = "";
                        JSONObject priceWrap = itemObj.optJSONObject("price");
                        if (priceWrap != null) {
                            JSONObject priceObj = priceWrap.optJSONObject("price");
                            if (priceObj != null) {
                                JSONObject formattedObj2 = priceObj.optJSONObject("formatted");
                                if (formattedObj2 != null) priceStr = formattedObj2.optString("value", "");
                                if (isBlank(priceStr)) priceStr = priceObj.optString("text", "");
                            }
                        }
                        int price = toIntMoney(priceStr);

                        ReceiptItemDTO rid = new ReceiptItemDTO();
                        rid.setItem_name(name);
                        rid.setItem_price(price);
                        
                        // OCR 단계에서는 category를 확정하지 않고 null로 둠 (AI 분류/사용자 확정 단계에서 채움)
                        rid.setItem_category(null);

                        itemList.add(rid);
                        goodsNames.add(name);
                    }
                }
            }

            dto.setItems(itemList);


        } catch (Exception e) {
            System.out.println("OCR 파싱 오류: " + e.getMessage());
        }

        return dto;
    }
    
    // 3) 파싱/보강 유틸
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // CLOVA JSON에서 중첩된 text/value를 안전하게 꺼내는 유틸
    private String optTextString(JSONObject parent, String... path) {
        if (parent == null || path == null || path.length == 0) return "";
        JSONObject cur = parent;

        for (int i = 0; i < path.length - 1; i++) {
            cur = cur.optJSONObject(path[i]);
            if (cur == null) return "";
        }

        String lastKey = path[path.length - 1];
        Object v = cur.opt(lastKey);
        if (v == null) return "";

        if (v instanceof JSONObject) {
            JSONObject obj = (JSONObject) v;
            String t = obj.optString("text", "");
            if (!t.isEmpty()) return t;
            return obj.optString("value", "");
        }

        return cur.optString(lastKey, "");
    }
    
    // 금액 문자열 -> int 변환 유틸
    private int toIntMoney(String s) {
        if (s == null) return 0;
        String digits = s.replaceAll(",", "").replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try { return Integer.parseInt(digits); } catch (Exception e) { return 0; }
    }
    
    // 다양한 날짜 포맷을 LocalDate로 파싱
    private LocalDate parseLocalDateFlexible(String dateStr) {
        if (dateStr == null) return null;
        String s = dateStr.trim();
        if (s.isEmpty()) return null;
        
        // "(월)" 같은 괄호 문구 제거
        s = s.replaceAll("\\(.*?\\)", "").trim();

        try { return LocalDate.parse(s); } catch (Exception ignore) {}
        try { return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy.MM.dd")); } catch (Exception ignore) {}
        try { return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy/MM/dd")); } catch (Exception ignore) {}
        try { return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd")); } catch (Exception ignore) {}

        return null;
    }

   
    // 4) CLOVA OCR 호출부(JSON + Base64 방식)
    public JSONObject callClovaOCR(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("업로드된 파일이 비어있습니다.");
            }

            String url = (clovaUrl == null) ? "" : clovaUrl.trim();
            String secret = (clovaSecret == null) ? "" : clovaSecret.trim();
            if (url.isEmpty()) throw new IllegalStateException("clova.url 설정이 비어있습니다.");
            if (secret.isEmpty()) throw new IllegalStateException("clova.secret 설정이 비어있습니다.");

            URI uri = URI.create(url);
            
            // 업로드 파일 -> 바이트
            byte[] bytes = file.getBytes();
            
            // 매직바이트로 실제 포맷 판별(확장자/Content-Type 신뢰 X)
            String actual = detectFormatByMagic(bytes);

            System.out.println("[UPLOAD] name=" + file.getOriginalFilename()
                    + ", contentType=" + file.getContentType()
                    + ", actual=" + actual
                    + ", size=" + bytes.length
                    + ", head=" + toHex(bytes, 16));
            
            // CLOVA에 넘길 포맷
            String formatForClova = actual;
            
            // WEBP는 CLOVA가 직접 지원하지 않는 경우가 있어 JPG 변환
            if ("webp".equals(actual)) {
                System.out.println("[CONVERT] WEBP detected. Converting to JPG... name=" + file.getOriginalFilename());
                bytes = convertWebpToJpg(bytes);
                formatForClova = "jpg";
            }
            
            // 최종 전송 포맷이 진짜 jpg/png인지 검증
            validateImage(bytes, formatForClova);
            
            // CLOVA는 base64만 받도록 구성 (data:image/... prefix 없이)
            String base64 = Base64.getEncoder().encodeToString(bytes);

            // 요청 바디 구성
            JSONObject body = new JSONObject();
            body.put("version", "V2");
            body.put("requestId", UUID.randomUUID().toString());
            body.put("timestamp", System.currentTimeMillis());

            JSONObject image = new JSONObject();
            image.put("format", formatForClova);
            image.put("name", "receipt");
            image.put("data", base64);

            JSONArray images = new JSONArray();
            images.put(image);
            body.put("images", images);
            
            // 요청 헤더 구성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-OCR-SECRET", secret);

            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
            
            // POST 호출
            ResponseEntity<String> response = clovaRestTemplate.postForEntity(uri, request, String.class);

            String respBody = response.getBody();
            System.out.println("[CLOVA JSON] status=" + response.getStatusCode());

            return (respBody == null || respBody.isBlank()) ? null : new JSONObject(respBody);

        } catch (HttpClientErrorException e) {
            System.out.println("[CLOVA JSON] HTTP ERROR status=" + e.getStatusCode());
            System.out.println("[CLOVA JSON] HTTP ERROR body=" + e.getResponseBodyAsString());
            e.printStackTrace();
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 5) OpenAI 결과 -> ReceiptItemDTO 변환
    private List<ReceiptItemDTO> convertOpenAiItemsToReceiptItems(OpenAiReceiptResult fixed) {
        List<ReceiptItemDTO> list = new ArrayList<>();
        if (fixed == null || fixed.getItems() == null) return list;

        for (OpenAiReceiptResult.Item it : fixed.getItems()) {
            if (it == null) continue;
            if (isBlank(it.getName())) continue;

            ReceiptItemDTO rid = new ReceiptItemDTO();
            rid.setItem_name(it.getName().trim());

            int p = (it.getPrice() <= 0) ? 0 : it.getPrice();
            rid.setItem_price(p);

         // OCR 단계에서는 category 확정 ❌ (AI 카테고리 분류/사용자 확정 단계에서 결정)
            rid.setItem_category(null);

            list.add(rid);
        }
        return list;
    }


    // ✅ WEBP -> JPG 변환(누락되어 있던 메서드 추가)
    private byte[] convertWebpToJpg(byte[] webpBytes) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(webpBytes));
            if (src == null) {
                throw new IllegalArgumentException("WEBP 디코딩 실패: ImageIO가 WEBP를 읽지 못했습니다. (WEBP ImageIO 의존성 필요)");
            }

            BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            boolean ok = ImageIO.write(rgb, "jpg", out);
            if (!ok) throw new IllegalArgumentException("JPG 인코딩 실패: ImageIO.write가 false 반환");

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("WEBP → JPG 변환 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 매직바이트로 실제 이미지 포맷 판별
     * - 확장자/Content-Type을 신뢰하지 않고 파일 헤더로 판단
     *
     * @return "jpg" / "png" / "webp" / "unknown"
     */
    private String detectFormatByMagic(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return "unknown";
        
        // JPG: FF D8 FF
        boolean isJpg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        if (isJpg) return "jpg";
        
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        boolean isPng = (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A;
        if (isPng) return "png";
        
        // WEBP: RIFF....WEBP
        boolean isRiff = bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46;
        boolean isWebp = bytes[8] == 0x57 && bytes[9] == 0x45 && (bytes[10] == 0x42) && (bytes[11] == 0x50);
        if (isRiff && isWebp) return "webp";

        return "unknown";
    }
    
    // CLOVA 전송 전에 이미지가 정말 jpg/png인지 1차 검증
    // 잘못된 바이트가 들어가면 CLOVA에서 400이 날 수 있어 미리 차단
    private void validateImage(byte[] bytes, String format) {
        if (bytes == null || bytes.length < 8) {
            throw new IllegalArgumentException("이미지 파일이 너무 작거나 비어있음");
        }
        if ("jpg".equals(format)) {
            boolean isJpg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
            if (!isJpg) throw new IllegalArgumentException("최종 전송 포맷이 JPG인데, 바이트 시그니처가 JPG가 아닙니다.");
        } else if ("png".equals(format)) {
            boolean isPng = (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                    && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A;
            if (!isPng) throw new IllegalArgumentException("최종 전송 포맷이 PNG인데, 바이트 시그니처가 PNG가 아닙니다.");
        } else {
            throw new IllegalArgumentException("CLOVA 전송 포맷은 jpg/png만 허용하도록 처리 중입니다. format=" + format);
        }
    }
    
    // 디버깅용: 바이트 앞부분을 16진수 문자열로 출력 
    private static String toHex(byte[] b, int n) {
        if (b == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, b.length); i++) {
            sb.append(String.format("%02X ", b[i]));
        }
        return sb.toString().trim();
    }
    
    // 7) byte[] 로부터 OCR 처리하기(내부 재사용용)
    // byte[]를 MultipartFile로 감싸서 CLOVA OCR -> 파싱/보강까지 수행
    public ReceiptDTO parseReceiptFromBytes(byte[] bytes, String filename) {

        try {
        	// byte[] -> MultipartFile 어댑터로 래핑
            MultipartFile multipartFile =
                    new ByteArrayMultiPartFile(
                            bytes,          // ✅ byte[] 반드시 필요
                            filename,       // name
                            "image/jpeg"    // contentType
                    );
            
            // CLOVA OCR 호출
            JSONObject json = callClovaOCR(multipartFile);
            if (json == null) return null;
            
            // 파싱 + 누락 보강
            return parseReceiptWithAssist(json, multipartFile);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
