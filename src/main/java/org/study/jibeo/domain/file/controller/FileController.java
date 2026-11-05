package org.study.jibeo.domain.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.study.jibeo.domain.file.dto.FileUploadResponse;
import org.study.jibeo.domain.file.service.FileService;
import org.study.jibeo.global.security.CurrentUser;
import org.study.jibeo.global.storage.FileStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

/** 호출 신고서 첨부 파일. 업로드는 사용 업체(WORKER) 전용, 보기는 서명된 주소로 누구나 */
@Tag(name = "첨부 파일")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    /** 한 구간만 S3 로 넘긴다. 그 밖의 Range 는 무시하고 전체를 보낸다 */
    private static final Pattern SINGLE_RANGE = Pattern.compile("bytes=\\d*-\\d*");

    private final FileService fileService;

    /** multipart/form-data 의 file 필드 1개. 이미지·영상, 20MB 까지 */
    @Operation(summary = "첨부 파일 업로드")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse upload(@RequestPart("file") MultipartFile file) {
        return fileService.uploadRequestAttachment(CurrentUser.companyId(), file);
    }

    /** 업로드·호출 상세 응답의 url 이 이 주소다. 직접 만들지 말고 받은 url 을 그대로 쓴다 */
    @Operation(summary = "첨부 파일 보기",
            description = "응답의 url 을 <img>/<video> src 에 그대로 넣는다. 로그인 없이 서명으로 확인하며, 만료되면 403 이니 상세를 다시 조회한다. 영상 구간 요청(Range)을 지원한다.")
    @GetMapping("/view")
    public void view(@RequestParam String key, @RequestParam long exp, @RequestParam String sig,
                     @RequestHeader(value = HttpHeaders.RANGE, required = false) String range,
                     HttpServletResponse response) throws IOException {
        String s3Range = range != null && SINGLE_RANGE.matcher(range.trim()).matches() ? range.trim() : null;
        FileService.FileView view = fileService.open(key, exp, sig, s3Range);
        FileStorage.FileStream file = view.stream();

        try (InputStream body = file.body()) {
            response.setStatus(file.contentRange() == null ? HttpServletResponse.SC_OK
                    : HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setContentType(file.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                    : file.contentType());
            if (file.contentLength() != null) {
                response.setContentLengthLong(file.contentLength());
            }
            if (file.contentRange() != null) {
                response.setHeader(HttpHeaders.CONTENT_RANGE, file.contentRange());
            }
            if (file.eTag() != null) {
                response.setHeader(HttpHeaders.ETAG, file.eTag());
            }
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");
            response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=" + view.maxAgeSeconds());
            body.transferTo(response.getOutputStream());
        }
    }
}