// capstone.voicereport.service.PythonAnalysisClient.java
package capstone.voicereport.service;

import capstone.voicereport.dto.AnalysisReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonAnalysisClient {

    private final WebClient webClient;

    /**
     * 파이썬 분석 서버에 multipart/form-data로 오디오와 메타를 보내고,
     * AnalysisReportDto(JSON)를 받아옵니다.
     *
     * @param audioBytes 업로드 오디오 바이트
     * @param filename   원본 파일명(확장자 포함 권장: .wav)
     * @param subTitle   리포트 소제목
     * @param userIdOrNull  "u123" 같이 프리픽스 포함 문자열 또는 null
     * @return 정상 수신 시 AnalysisReportDto, 실패 시 null
     */
    public AnalysisReportDto analyze(byte[] audioBytes,
                                     String filename,
                                     String subTitle,
                                     String userIdOrNull) {

        MultipartBodyBuilder body = new MultipartBodyBuilder();

        // 파일 파트
        body.part("audio", new ByteArrayResource(audioBytes) {
                    @Override public String getFilename() {
                        return (filename != null && !filename.isBlank()) ? filename : "audio.wav";
                    }
                })
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        // 폼 파트
        body.part("subTitle", subTitle != null ? subTitle : "");
        if (userIdOrNull != null) {
            body.part("user_id", userIdOrNull);
        }

        try {
            return webClient.post()
                    .uri("/analyze")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(body.build())
                    .retrieve()
                    .bodyToMono(AnalysisReportDto.class)
                    // 네트워크/역직렬화 에러는 service에서 graceful 하게 처리할 수 있도록 null로 바꿔줌
                    .onErrorResume(WebClientResponseException.class, e -> {
                        log.warn("Python analysis HTTP error: status={}, body={}",
                                e.getRawStatusCode(), e.getResponseBodyAsString());
                        return Mono.empty();
                    })
                    .onErrorResume(Exception.class, e -> {
                        log.warn("Python analysis call failed: {}", e.toString());
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception e) {
            // block() 중 인터럽트 등 예외
            log.warn("Python analysis unexpected failure: {}", e.toString());
            return null;
        }
    }
}
