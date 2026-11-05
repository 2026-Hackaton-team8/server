package org.study.jibeo.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.study.jibeo.domain.file.dto.FileUploadResponse;
import org.study.jibeo.global.exception.ErrorCode;
import org.study.jibeo.global.storage.FileStorage;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * 호출 신고서 첨부 파일 (이미지·영상). 키는 회사별 경로에 두어, 호출 제출 때 우리 회사가 올린 파일인지 확인한다.
 */
@Service
@RequiredArgsConstructor
public class FileService {

    /** 허용 형식 → 저장 확장자 */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/heic", "heic",
            "video/mp4", "mp4",
            "video/quicktime", "mov",
            "video/webm", "webm");

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final FileStorage fileStorage;

    /** 이 회사가 올린 호출 첨부 파일의 키 접두어 */
    public static String requestAttachmentPrefix(Long companyId) {
        return "uploads/requests/" + companyId + "/";
    }

    public FileUploadResponse uploadRequestAttachment(Long companyId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ErrorCode.EMPTY_FILE.toException();
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String extension = ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw ErrorCode.UNSUPPORTED_FILE_TYPE.toException();
        }

        LocalDate today = LocalDate.now(KST);
        String key = "%s%d/%02d/%s.%s".formatted(requestAttachmentPrefix(companyId),
                today.getYear(), today.getMonthValue(), UUID.randomUUID(), extension);

        try (InputStream in = file.getInputStream()) {
            fileStorage.upload(key, in, file.getSize(), contentType);
        } catch (IOException e) {
            throw ErrorCode.UNREADABLE_FILE.toException();
        }
        return new FileUploadResponse(key, contentType, file.getSize(), fileStorage.viewUrl(key));
    }
}
