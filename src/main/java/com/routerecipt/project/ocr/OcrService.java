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

@Service
@RequiredArgsConstructor
public class OcrService {

    @Value("${clova.url}")
    private String clovaUrl;

    @Value("${clova.secret}")
    private String clovaSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    // OCR 누락 보강
    private final OpenAiOcrAssisService openAiOcrAssistService;


    /**
     * 1) CLOVA 파싱
     * 2) 누락 시 OpenAI로 보강
     * 3) items 카테고리 OpenAI 분류
     */
    public ReceiptDTO parseReceiptWithAssist(JSONObject json, MultipartFile file) {

        ReceiptDTO parsed = parseReceipt(json);

        if (needOpenAiFix(parsed)) {

            String rawText = extractRawText(json);
            System.out.println("[RAW_TEXT]\n" + rawText);

            if (!isBlank(rawText)) {

                // 현재 네 서비스가 text 기반 보강을 하도록 되어있다면 그대로 사용
                // (Vision까지 하려면 fixMissingFieldsWithVision(file, rawText)로 변경)
                OpenAiReceiptResult fixed = openAiOcrAssistService.fixMissingFieldsWithVision(file, rawText);

                if (fixed != null) {
                    // place
                    if (isBlank(parsed.getR_place()) && !isBlank(fixed.getPlace())) {
                        parsed.setR_place(fixed.getPlace().trim());
                    }

                    // date
                    if (parsed.getR_date() == null && !isBlank(fixed.getDate())) {
                        try {
                            parsed.setR_date(LocalDate.parse(fixed.getDate().trim()));
                        } catch (Exception ignore) {}
                    }

                    // total
                    if (parsed.getR_price() <= 0 && fixed.getTotal() != null && fixed.getTotal() > 0) {
                        parsed.setR_price(fixed.getTotal());
                    }

                    // items 보강 (CLOVA items가 비었을 때만)
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

        // fields 기반
        JSONArray fields = img0.optJSONArray("fields");
        if (fields != null) {
            for (int i = 0; i < fields.length(); i++) {
                JSONObject f = fields.optJSONObject(i);
                if (f == null) continue;
                String t = f.optString("inferText", "");
                if (!t.isBlank()) sb.append(t).append("\n");
            }
        }

        // receipt.result 기반 보강
        JSONObject receipt = img0.optJSONObject("receipt");
        JSONObject result = (receipt == null) ? null : receipt.optJSONObject("result");
        if (result != null) {
            String store = optTextString(result, "storeInfo", "name");
            String dateText = optTextString(result, "paymentInfo", "date");
            String totalText = optTextString(result, "totalPrice", "price");

            if (!store.isBlank()) sb.append(store).append("\n");
            if (!dateText.isBlank()) sb.append(dateText).append("\n");
            if (!totalText.isBlank()) sb.append(totalText).append("\n");

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

    // ✅ 오타 수정(pprivate -> private)
    private boolean needOpenAiFix(ReceiptDTO r) {
        if (r == null) return true;
        return isBlank(r.getR_place())
                || r.getR_date() == null
                || r.getR_price() <= 0
                || r.getItems() == null
                || r.getItems().isEmpty();
    }



    public ReceiptDTO parseReceipt(JSONObject json) {
        ReceiptDTO dto = new ReceiptDTO();
        if (json == null) return dto;

        try {
            JSONObject result = json
                    .getJSONArray("images")
                    .getJSONObject(0)
                    .getJSONObject("receipt")
                    .getJSONObject("result");

            String place = optTextString(result, "storeInfo", "name");
            dto.setR_place(isBlank(place) ? null : place.trim());

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

            String totalStr = optTextString(result, "totalPrice", "price");
            dto.setR_price(toIntMoney(totalStr));

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

                        String name = optTextString(itemObj, "name");
                        if (isBlank(name)) continue;
                        name = name.trim();

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

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

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

    private int toIntMoney(String s) {
        if (s == null) return 0;
        String digits = s.replaceAll(",", "").replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try { return Integer.parseInt(digits); } catch (Exception e) { return 0; }
    }

    private LocalDate parseLocalDateFlexible(String dateStr) {
        if (dateStr == null) return null;
        String s = dateStr.trim();
        if (s.isEmpty()) return null;

        s = s.replaceAll("\\(.*?\\)", "").trim();

        try { return LocalDate.parse(s); } catch (Exception ignore) {}
        try { return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy.MM.dd")); } catch (Exception ignore) {}
        try { return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy/MM/dd")); } catch (Exception ignore) {}
        try { return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd")); } catch (Exception ignore) {}

        return null;
    }

    // ===================== CLOVA 호출부 =====================

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

            byte[] bytes = file.getBytes();

            String actual = detectFormatByMagic(bytes);

            System.out.println("[UPLOAD] name=" + file.getOriginalFilename()
                    + ", contentType=" + file.getContentType()
                    + ", actual=" + actual
                    + ", size=" + bytes.length
                    + ", head=" + toHex(bytes, 16));

            String formatForClova = actual;
            if ("webp".equals(actual)) {
                System.out.println("[CONVERT] WEBP detected. Converting to JPG... name=" + file.getOriginalFilename());
                bytes = convertWebpToJpg(bytes);
                formatForClova = "jpg";
            }

            validateImage(bytes, formatForClova);

            String base64 = Base64.getEncoder().encodeToString(bytes);

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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-OCR-SECRET", secret);

            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);

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

            // OCR 단계에서는 category 확정 ❌
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

    private String detectFormatByMagic(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return "unknown";

        boolean isJpg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        if (isJpg) return "jpg";

        boolean isPng = (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A;
        if (isPng) return "png";

        boolean isRiff = bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46;
        boolean isWebp = bytes[8] == 0x57 && bytes[9] == 0x45 && (bytes[10] == 0x42) && (bytes[11] == 0x50);
        if (isRiff && isWebp) return "webp";

        return "unknown";
    }

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

    private static String toHex(byte[] b, int n) {
        if (b == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, b.length); i++) {
            sb.append(String.format("%02X ", b[i]));
        }
        return sb.toString().trim();
    }
    
    public ReceiptDTO parseReceiptFromBytes(byte[] bytes, String filename) {

        try {
            MultipartFile multipartFile =
                    new ByteArrayMultiPartFile(
                            bytes,          // ✅ byte[] 반드시 필요
                            filename,       // name
                            "image/jpeg"    // contentType
                    );

            JSONObject json = callClovaOCR(multipartFile);
            if (json == null) return null;

            return parseReceiptWithAssist(json, multipartFile);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
