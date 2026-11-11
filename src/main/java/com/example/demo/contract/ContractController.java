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

/** 담당 A: 계약서 텍스트(파일 또는 붙여넣기)를 받아 규칙 기반으로 추출한다. */
@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractTextParser parser;

    public ContractController(ContractTextParser parser) {
        this.parser = parser;
    }

    /**
     * 파일이 있으면 파일을, 없으면 text를 읽는다. 둘 다 비어 있으면 400,
     * 텍스트가 아닌 파일(PDF·이미지 등)은 외부 인식 API가 없으므로 415를 돌려준다.
     */
    @PostMapping("/extract")
    public ContractInfo extract(@RequestParam(name = "file", required = false) MultipartFile file,
                                @RequestParam(name = "text", required = false) String text) {
        String source = text;
        if (file != null && !file.isEmpty()) {
            if (!isPlainText(file)) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "텍스트(.txt) 파일만 지원합니다.");
            }
            try {
                source = new String(file.getBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일을 읽을 수 없습니다.", e);
            }
        }
        if (source == null || source.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "계약서 파일 또는 텍스트가 필요합니다.");
        }
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
