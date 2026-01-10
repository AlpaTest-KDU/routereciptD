package com.routerecipt.project.service;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.dto.UploadResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImp implements ReceiptService {

    private final ReceiptCommandService receiptCommandService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${receipt.upload.temp-dir}")
    private String uploadDir;

    @Override
    @Transactional
    public UploadResult uploadReceipts(List<MultipartFile> files, String userId) {

        UploadResult result = new UploadResult();
        List<Long> successReceiptNos = new ArrayList<>();

        int success = 0;
        int fail = 0;

        log.info("[UPLOAD] START userId={}, fileCount={}",
                userId, (files == null ? 0 : files.size()));

        if (files == null || files.isEmpty()) {
            result.setSuccessReceiptNos(successReceiptNos);
            result.setSuccessCount(0);
            result.setFailCount(0);
            return result;
        }

        try {
            // 1️⃣ 파일 저장 (IO 전용)
            List<String> imagePaths = saveTempFiles(files);

         // 2️⃣ PENDING receipt 생성 (DB 최소 작업)
            List<Long> receiptNos = new ArrayList<>();

            // 3️⃣ Redis Stream 발행 (비동기 OCR 트리거)
            for (int i = 0; i < receiptNos.size(); i++) {

                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", userId);
                payload.put("receiptNo", receiptNos.get(i));
                payload.put("imagePath", imagePaths.get(i));

                redisTemplate.opsForStream()
                        .add("receipt-ocr-stream", payload);
            }
        } catch (Exception e) {
            log.error("[UPLOAD] FAIL userId={}", userId, e);
            fail = (files == null ? 0 : files.size());
        }

        log.info("[UPLOAD] END userId={}, success={}, fail={}",
                userId, success, fail);

        result.setSuccessReceiptNos(successReceiptNos);
        result.setSuccessCount(success);
        result.setFailCount(fail);

        return result;
    }

    @Override
    public List<String> saveTempFiles(List<MultipartFile> files) {

        List<String> paths = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try {
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path target = Paths.get(uploadDir, filename);

                Files.createDirectories(target.getParent());
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

                paths.add(target.toString());

            } catch (Exception e) {
                throw new RuntimeException("파일 저장 실패", e);
            }
        }
        return paths;
    }
}

