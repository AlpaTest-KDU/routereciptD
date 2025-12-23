package com.routerecipt.project.receipt;

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

@Service
public class ReceiptService {

    @Value("${clova.secret}")
    private String secretKey;

    @Value("${clova.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * ✅ CLOVA OCR 호출 (JSON + Base64 방식)
     */
    public JSONObject callClovaOCR(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("업로드된 파일이 비어있습니다.");
            }

            String url = (apiUrl == null) ? "" : apiUrl.trim();
            String secret = (secretKey == null) ? "" : secretKey.trim();

            if (url.isEmpty()) throw new IllegalStateException("clova.url 설정이 비어있습니다.");
            if (secret.isEmpty()) throw new IllegalStateException("clova.secret 설정이 비어있습니다.");

            URI uri = URI.create(url);

            String format = detectFormat(file); // jpg/png
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());

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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-OCR-SECRET", secret);

            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);

            String respBody = response.getBody();
            System.out.println("[ReceiptService CLOVA JSON] status=" + response.getStatusCode());
            System.out.println("[ReceiptService CLOVA JSON] resp=" + respBody);

            return (respBody == null || respBody.isBlank()) ? null : new JSONObject(respBody);

        } catch (HttpClientErrorException e) {
            System.out.println("[ReceiptService CLOVA JSON] HTTP ERROR status=" + e.getStatusCode());
            System.out.println("[ReceiptService CLOVA JSON] HTTP ERROR body=" + e.getResponseBodyAsString());
            e.printStackTrace();
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ---------------- DB 저장은 그대로 ----------------

    @Autowired
    private ReceiptMapper mapper;


    // ---------------- helper ----------------

    private String detectFormat(MultipartFile file) {
        String ct = file.getContentType();

        if ("image/png".equalsIgnoreCase(ct)) return "png";
        if ("image/jpeg".equalsIgnoreCase(ct) || "image/jpg".equalsIgnoreCase(ct)) return "jpg";

        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".png")) return "png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "jpg";
        }

        throw new IllegalArgumentException("지원하지 않는 이미지 타입: contentType=" + ct + ", filename=" + name);
    }
}
