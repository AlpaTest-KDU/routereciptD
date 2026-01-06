package com.routerecipt.project.dto;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.springframework.web.multipart.MultipartFile;



/**
 * byte[] 기반 MultipartFile 구현체
 *
 * 역할:
 *  - 메모리에 있는 byte[] 데이터를 MultipartFile처럼 사용하기 위한 어댑터
 *  - 파일 업로드 없이 기존 MultipartFile 기반 로직(OCR, 분석 등)을 재사용 가능
 *
 * 사용 예:
 *  MultipartFile mf =
 *      new ByteArrayMultiPartFile(bytes, "receipt.jpg", "image/jpeg");
 */
public class ByteArrayMultiPartFile implements MultipartFile {

	/** 실제 파일 데이터 */
    private final byte[] content;
    
    /** 파일 이름 */
    private final String name;
    
    /** MIME 타입 (예: image/jpeg, image/png) */
    private final String contentType;

    
    /**
     * 생성자
     *
     * @param content     파일 데이터(byte[])
     * @param name        파일명
     * @param contentType MIME 타입
     */
    public ByteArrayMultiPartFile(byte[] content, String name, String contentType) {
        this.content = content;
        this.name = name;
        this.contentType = contentType;
    }
    
    
    // 파라미터 이름 반환 (일반적으로 파일명과 동일하게 사용)
    @Override
    public String getName() {
        return name;
    }
    
   
    // 원본 파일명 반환
    @Override
    public String getOriginalFilename() {
        return name;
    }

    // Content-Type 반환
    @Override
    public String getContentType() {
        return contentType;
    }

    // 파일이 비어있는지 여부
    @Override
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    // 파일 크기(byte)
    @Override
    public long getSize() {
        return content.length;
    }

    // 파일 내용을 byte[]로 반환
    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    // 파일 내용을 InputStream으로 반환
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

  
    // 실제 파일로 저장    
    // @param dest 저장할 대상 파일
    @Override
    public void transferTo(File dest) throws IOException {
        Files.write(dest.toPath(), content);
    }
}
