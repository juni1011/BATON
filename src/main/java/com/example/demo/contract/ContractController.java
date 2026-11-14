package com.example.demo.contract;

import com.example.demo.common.ContractInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** 담당 A: 계약서 텍스트(파일 또는 붙여넣기)는 규칙으로, 사진은 Gemini로 추출한다. */
@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final ContractTextParser parser;
    private final ContractImageAnalyzer imageAnalyzer;

    public ContractController(ContractTextParser parser, ContractImageAnalyzer imageAnalyzer) {
        this.parser = parser;
        this.imageAnalyzer = imageAnalyzer;
    }

    /**
     * 파일이 있으면 파일을, 없으면 text를 읽는다. 둘 다 비어 있으면 400이다.
     * PNG·JPEG·WEBP·GIF 사진은 Gemini로 분석하며, 사진이 외부 AI로 전송되므로 {@code consent=true}가 없으면 400이다.
     * 텍스트는 기본적으로 규칙으로 읽고, {@code consent=true}를 주면 Gemini로 읽는다(문장형 계약서용).
     * AI 분석이 실패하면 규칙 결과로 바꿔치지 않고 오류를 돌려준다. 그 밖의 텍스트가 아닌 파일(PDF 등)은 415를 돌려준다.
     */
    @PostMapping("/extract")
    public ContractInfo extract(@RequestParam(name = "file", required = false) MultipartFile file,
                                @RequestParam(name = "text", required = false) String text,
                                @RequestParam(name = "consent", required = false) Boolean consent) {
        String source = text;
        if (file != null && !file.isEmpty()) {
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일을 읽을 수 없습니다.", e);
            }
            String imageType = ContractImageAnalyzer.imageExtension(bytes);
            if (imageType != null) {
                if (!Boolean.TRUE.equals(consent)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진 전송에 동의해야 분석할 수 있습니다.");
                }
                if (bytes.length > MAX_IMAGE_BYTES) {
                    throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "사진이 너무 큽니다.");
                }
                return imageAnalyzer.analyze(bytes, imageType);
            }
            if (!isPlainText(file)) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "텍스트(.txt) 파일이나 PNG·JPEG·WEBP·GIF 사진만 지원합니다.");
            }
            source = new String(bytes, StandardCharsets.UTF_8);
        }
        if (source == null || source.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "계약서 파일 또는 텍스트가 필요합니다.");
        }
        if (Boolean.TRUE.equals(consent)) return imageAnalyzer.analyzeText(source);
        return parser.parse(source);
    }

    @GetMapping(value = "/samples/{number}", produces = "text/plain;charset=UTF-8")
    public String sample(@PathVariable int number) throws IOException {
        if (number < 1 || number > 3) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new ClassPathResource("samples/contract" + number + ".txt")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private static boolean isPlainText(MultipartFile file) {
        String type = file.getContentType();
        String name = file.getOriginalFilename();
        return (type != null && type.startsWith("text/"))
                || (name != null && name.toLowerCase().endsWith(".txt"));
    }
}
