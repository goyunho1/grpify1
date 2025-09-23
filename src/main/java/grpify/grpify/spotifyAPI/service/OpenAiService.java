package grpify.grpify.spotifyAPI.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환용 ObjectMapper

    private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

    @Value("${openai.api-key}")
    private String apiKey;

    public OpenAiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com/v1").build();
    }

    public List<String> processImage(MultipartFile file) throws IOException {
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String systemContent = "Extract 'track' and 'artist' names from the image and return the result as a JSON array.";
        String userContent = "Respond only with a JSON array, formatted as follows: " +
                "[{\"track\": \"Song Name\", \"artist\": \"Artist Name\"}, {\"track\": \"Another Song\", \"artist\": \"Another Artist\"}]";
        // OpenAI API 요청 페이로드
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4.1-mini",

                "messages", List.of(
                        Map.of("role", "system",
                                "content", systemContent),

                        Map.of("role", "user",
                                "content", List.of(
                                        Map.of("type", "text",
                                                "text", userContent),
                                        Map.of("type", "image_url",
                                                "image_url", Map.of("url", "data:image/png;base64," + base64Image))
                                )
                        )
                ),
                "max_tokens", 1000
        );


//         OpenAI API 호출 및 응답 처리 -> JSON 변환 후 URL 인코딩 수행
        return webClient.post()
                .uri("/chat/completions")
                .headers(headers -> headers.setBearerAuth(apiKey))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Response.class)
                .map(response -> {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    logger.info("🔹 OpenAI API Raw Response: {}", content); // 응답 확인
                    try {
                        // 코드 블록(````json`)이 감싸고 있는지 확인 후 제거
                        if (content.startsWith("```json")) {
                            content = content.substring(7, content.length() - 3).trim();
                        }
                        List<TrackArtist> trackArtists = objectMapper.readValue(content, new TypeReference<>() {});
                        return trackArtists.stream()
                                .map(trackArtist -> trackArtist.track + " " + trackArtist.artist)
                                .toList();

                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse track & artist response", e);
                    }
                })
                .block();
    }

//      //응답 확인용
//         webClient.post()
//                .uri("/chat/completions")
//                .headers(headers -> headers.setBearerAuth(apiKey))
//                .bodyValue(requestBody)
//                .retrieve()
//                .bodyToMono(String.class) // 응답을 String으로 변환
//                .doOnNext(response -> logger.info(" OpenAI API Response: {}", response)) // 응답 출력
//                .doOnError(WebClientResponseException.class, error ->
//                        logger.error(" OpenAI API Error: {}", error.getResponseBodyAsString())) // 에러 메시지 출력
//                .block();
//
//        return List.of("asd");
//    }

    public static class Response {
        @Getter
        @JsonProperty("choices")
        private List<Choice> choices;
    }

    public static class Choice {
        @Getter
        @JsonProperty("message")
        private Message message;
    }

    public static class Message {
        @Getter
        @JsonProperty("content")
        private String content; // JSON 문자열로 응답받음
    }

    @Getter
    public static class TrackArtist {
        @JsonProperty("track")
        private String track;

        @JsonProperty("artist")
        private String artist;
    }
}

