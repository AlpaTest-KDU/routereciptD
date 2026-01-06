package com.routerecipt.project.OpenAI;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * OpenAI Vision / OCR 보조 결과 DTO
 *
 * 역할:
 *  - OpenAI가 반환한 "순수 JSON" 결과를 자바 객체로 변환
 *  - CLOVA OCR 파싱 결과의 누락 필드를 보강하기 위한 중간 결과 모델
 *
 * 주의:
 *  - 이 클래스는 DB 저장용이 아님
 *  - ReceiptDTO에 병합하기 위한 보조 데이터 컨테이너
 */
public class OpenAiReceiptResult {

    private String place;	// 가게명
    private String date;	// 거래 날짜 (yyyy-MM-dd 문자열)
    private Integer total;	// 영수증 총액

    // ✅ items 타입을 내부 Item과 통일
    private List<Item> items = new ArrayList<>();

    // =========================
    // Inner DTO: Item
    // =========================
    // OpenAI 응답의 items 배열 요소 1개를 표현
    public static class Item {
        private String name;	// 상품명
        private int price;		// 상품 가격
        private int count;		// 상품 수량 (현재는 미사용이지만 확장 대비)

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getPrice() { return price; }
        public void setPrice(int price) { this.price = price; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    // =========================
    // getters / setters
    // =========================
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }

    public List<Item> getItems() { return items; }
    
    // items setter(null이 들어와도 빈 리스트로 안전 처리)
    public void setItems(List<Item> items) {
        this.items = (items == null) ? new ArrayList<>() : items;
    }

    // =========================
    // Vision JSON -> Result
    // =========================
    // OpenAI Vision API가 반환한 JSON을 OpenAiReceiptResult로 변환
    public static OpenAiReceiptResult fromVisionJson(JSONObject j) {
        OpenAiReceiptResult r = new OpenAiReceiptResult();
        if (j == null) return r;
       
        // 상호명 / 날짜 / 총액
        r.place = j.optString("place", "").trim();
        r.date  = j.optString("date", "").trim();
        r.total = j.has("total") ? j.optInt("total", 0) : 0;
        
        // 아이템 목록 파싱
        JSONArray arr = j.optJSONArray("items");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject it = arr.optJSONObject(i);
                if (it == null) continue;

                Item item = new Item();
                item.name  = it.optString("name", "").trim();
                item.price = it.optInt("price", 0);
                item.count = it.optInt("count", 1);

                // 상품명이 없는 항목은 무시 (노이즈 제거)
                if (item.name != null && !item.name.isBlank()) {
                    r.items.add(item);
                }
            }
        }

        return r;
    }
}
