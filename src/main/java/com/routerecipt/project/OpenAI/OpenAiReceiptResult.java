package com.routerecipt.project.OpenAI;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class OpenAiReceiptResult {

    private String place;
    private String date;
    private Integer total;

    // ✅ items 타입을 내부 Item과 통일
    private List<Item> items = new ArrayList<>();

    // =========================
    // Inner DTO: Item
    // =========================
    public static class Item {
        private String name;
        private int price;
        private int count;

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
    public void setItems(List<Item> items) {
        this.items = (items == null) ? new ArrayList<>() : items;
    }

    // =========================
    // Vision JSON -> Result
    // =========================
    public static OpenAiReceiptResult fromVisionJson(JSONObject j) {
        OpenAiReceiptResult r = new OpenAiReceiptResult();
        if (j == null) return r;

        r.place = j.optString("place", "").trim();
        r.date  = j.optString("date", "").trim();
        r.total = j.has("total") ? j.optInt("total", 0) : 0;

        JSONArray arr = j.optJSONArray("items");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject it = arr.optJSONObject(i);
                if (it == null) continue;

                Item item = new Item();
                item.name  = it.optString("name", "").trim();
                item.price = it.optInt("price", 0);
                item.count = it.optInt("count", 1);

                // 이름 비어있으면 스킵
                if (item.name != null && !item.name.isBlank()) {
                    r.items.add(item);
                }
            }
        }

        return r;
    }
}
