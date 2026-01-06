package com.routerecipt.project.ocr;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;          // ✅ Spring MediaType
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.mapper.ReceiptMapper;


/**
 * CLOVA OCR 호출 전용 Service
 *  - 이미지 파일을 Base64(JSON 방식)로 변환
 *  - CLOVA OCR API 호출
 *  - OCR 결과(JSON)를 그대로 반환
 */
@Service
public class ClovaOcrService {
	
	/** CLOVA OCR Secret Key (X-OCR-SECRET 헤더에 사용) */
    @Value("${clova.secret}")
    private String secretKey;
    
    /** CLOVA OCR API URL */
    @Value("${clova.url}")
    private String apiUrl;

    /** REST API 호출용 */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * ✅ CLOVA OCR 호출 (JSON + Base64 방식)
     */
    public JSONObject callClovaOCR(MultipartFile file) {
        try {
        	// ---------------- 1. 파일 유효성 검사 ----------------
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("업로드된 파일이 비어있습니다.");
            }
            
            // ---------------- 2. 환경 변수 검증 ----------------
            String url = (apiUrl == null) ? "" : apiUrl.trim();
            String secret = (secretKey == null) ? "" : secretKey.trim();

            if (url.isEmpty()) throw new IllegalStateException("clova.url 설정이 비어있습니다.");
            if (secret.isEmpty()) throw new IllegalStateException("clova.secret 설정이 비어있습니다.");

            URI uri = URI.create(url);
            
            // ---------------- 3. 이미지 포맷 판별 (jpg/png) ----------------
            String format = detectFormat(file); 

            // ---------------- 4. 이미지 Base64 인코딩 ----------------
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            
            // ---------------- 5. CLOVA OCR 요청 JSON 구성 ----------------
            JSONObject body = new JSONObject();
            body.put("version", "V2");
            body.put("requestId", UUID.randomUUID().toString());
            body.put("timestamp", System.currentTimeMillis());

            JSONObject image = new JSONObject();
            image.put("format", format);
            image.put("name", "receipt");
            image.put("data", base64); // ⚠️ data:image/... prefix 넣지 말기

            JSONArray images = new JSONArray();
            images.put(image);
            body.put("images", images);
            
            // ---------------- 6. HTTP Header 설정 ----------------
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-OCR-SECRET", secret);

            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            // ---------------- 7. REST API 호출 ----------------
            ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);

            String respBody = response.getBody();
            System.out.println("[ReceiptService CLOVA JSON] status=" + response.getStatusCode());
            System.out.println("[ReceiptService CLOVA JSON] resp=" + respBody);
            
            // ---------------- 8. 응답 JSON 변환 ----------------
            return (respBody == null || respBody.isBlank()) ? null : new JSONObject(respBody);

        } 
        // ---------------- HTTP 오류 (401, 403, 400 등) ----------------
        catch (HttpClientErrorException e) {
            System.out.println("[ReceiptService CLOVA JSON] HTTP ERROR status=" + e.getStatusCode());
            System.out.println("[ReceiptService CLOVA JSON] HTTP ERROR body=" + e.getResponseBodyAsString());
            e.printStackTrace();
            return null;

        } 
        // ---------------- 기타 예외 ----------------
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ---------------- DB Mapper (OCR 결과 저장용) ----------------

    @Autowired
    private ReceiptMapper mapper;


    // ---------------- 이미지 포맷 판별 유틸 메서드 ----------------

    private String detectFormat(MultipartFile file) {
        String ct = file.getContentType();
        
        // Content-Type 기반 판별
        if ("image/png".equalsIgnoreCase(ct)) return "png";
        if ("image/jpeg".equalsIgnoreCase(ct) || "image/jpg".equalsIgnoreCase(ct)) return "jpg";
        
     // 파일 확장자 기반 판별 (보조 수단)
        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".png")) return "png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "jpg";
        }
        
        // 지원하지 않는 타입
        throw new IllegalArgumentException("지원하지 않는 이미지 타입: contentType=" + ct + ", filename=" + name);
    }
}
