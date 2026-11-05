package org.study.jibeo.domain.file.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.study.jibeo.domain.file.dto.FileUploadResponse;
import org.study.jibeo.domain.file.service.FileService;
import org.study.jibeo.global.security.CurrentUser;

/** 사용 업체(WORKER) 전용: 호출 신고서 첨부 파일 업로드 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /** multipart/form-data 의 file 필드 1개. 이미지·영상, 20MB 까지 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse upload(@RequestPart("file") MultipartFile file) {
        return fileService.uploadRequestAttachment(CurrentUser.companyId(), file);
    }
}
