package com.routerecipt.project.OpenAI;



import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class OpenAiReceiptItem {
    private String name;
    private Integer price;  // 없을 수 있음
    private Integer count;  // 너는 안 쓰지만 OpenAI가 줄 수 있으니 받아둠
}