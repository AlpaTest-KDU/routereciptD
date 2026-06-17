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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.UploadResult;
import com.routerecipt.project.mapper.ReceiptMapper;
import com.routerecipt.project.stream.OcrStreamProducer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReceiptServiceImp implements ReceiptService {

    private final ReceiptCommandService receiptCommandService;
    private final RedisTemplate<String, String> redisTemplate;
    private final OcrStreamProducer ocrStreamProducer;
    private final ReceiptMapper receiptMapper;
    
    public ReceiptServiceImp(ReceiptCommandService receiptCommandService,
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
            OcrStreamProducer ocrStreamProducer,
            ReceiptMapper receiptMapper) {
    	this.receiptCommandService = receiptCommandService;
    	this.redisTemplate = redisTemplate;
    	this.ocrStreamProducer = ocrStreamProducer;
    	this.receiptMapper = receiptMapper;
    }

    @Value("${receipt.upload.temp-dir}")
    private String uploadDir;

    /**
     * 영수증 업로드
     * - 파일 저장
     * - PENDING receipt 생성
     * - Redis Stream 발행 (비동기 OCR 트리거)
     */
    @Override
    @Transactional
    public UploadResult uploadReceipts(List<MultipartFile> files, String userId) {

        UploadResult result = new UploadResult();
        List<Long> successReceiptNos = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            result.setSuccessReceiptNos(successReceiptNos);
            result.setSuccessCount(0);
            result.setFailCount(0);
            return result;
        }

        try {
            // 1️⃣ 파일 저장
            List<String> imagePaths = saveTempFiles(files);

            // 2️⃣ receipt 생성 + image_path 확정
            List<Long> receiptNos = new ArrayList<>();

            for (String imagePath : imagePaths) {

                ReceiptDTO receipt = new ReceiptDTO();
                receipt.setR_u(userId);
                receipt.setImagePath(imagePath);
                receipt.setOcr_status("PENDING");

                // 🔥 여기서 image_path가 DB에 확정됨
                receiptCommandService.insertPendingReceipt(receipt);

                receiptNos.add(receipt.getR_no());
                successReceiptNos.add(receipt.getR_no());
            }

            // 3️⃣ DB 확정 이후 Redis Stream 발행 (🔥 A안 핵심)
            for (int i = 0; i < receiptNos.size(); i++) {
                ocrStreamProducer.publishOcrEvent(
                    userId,
                    imagePaths.get(i),
                    receiptNos.get(i)
                );
            }

            result.setSuccessCount(receiptNos.size());
            result.setFailCount(0);

        } catch (Exception e) {
            log.error("[UPLOAD] FAIL userId={}", userId, e);
            result.setFailCount(files.size());
        }

        result.setSuccessReceiptNos(successReceiptNos);
        return result;
    }

    /**
     * 임시 파일 저장
     * - OCR 처리 전용
     * - 삭제는 OCR Consumer에서 처리
     */
    @Override
    public List<String> saveTempFiles(List<MultipartFile> files) {

        List<String> paths = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try {
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path target = Paths.get(uploadDir, filename);

                Files.createDirectories(target.getParent());
                Files.copy(
                        file.getInputStream(),
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );

                paths.add(target.toString());

            } catch (Exception e) {
                throw new RuntimeException("파일 저장 실패", e);
            }
        }
        return paths;
    }
    
    @Override
    public String getImagePathByReceiptNo(Long receiptNo) {
    	return receiptMapper.selectImagePathByReceiptNo(receiptNo);
    }
    
    @Override
    public ReceiptDTO getReceiptByNo(Long receiptNo) {
    	return receiptMapper.selectReceiptByNo(receiptNo);
    }
}
