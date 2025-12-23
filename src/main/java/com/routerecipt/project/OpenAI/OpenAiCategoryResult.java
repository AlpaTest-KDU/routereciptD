package com.routerecipt.project.OpenAI;

import java.util.List;

import lombok.Data;

@Data
public class OpenAiCategoryResult {

    private List<ItemCategoryPair> categories;

    @Data
    public static class ItemCategoryPair {
        private String name;      // item_name과 매칭
        private String category;  // FOOD/CLOTHES/MEDICAL/HOME/LIVING/CULTURE/TRAFFIC/ETC
    }
}
