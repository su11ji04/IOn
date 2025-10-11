package capstone.support.userprofile;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileLoader {

    private final UserProfileProperties props;

    @PostConstruct
    void init() {
        log.info("[USERPROFILE] csvPath={}", props.getCsvPath());
    }

    public Optional<UserProfile> find(String userId) {
        if (userId == null || userId.isBlank()) return Optional.empty();
        String path = props.getCsvPath();
        if (path == null || path.isBlank()) {
            log.warn("UserProfile CSV path not configured.");
            return Optional.empty();
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] arr = line.split(",", -1);
                if (arr.length < 8) continue;
                String uid = arr[0].trim();
                if (!userId.equals(uid)) continue;

                return Optional.of(UserProfile.builder()
                        .userId(uid)
                        .childAge(arr[1].trim())
                        .parentingStyle(arr[2].trim())
                        .parentingGoal(arr[3].trim())
                        .childTraits(arr[4].trim())
                        .preferredTone(arr[5].trim())
                        .language(arr[6].trim())
                        .healthIssues(arr[7].trim())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to read CSV {}: {}", path, e.toString());
        }
        return Optional.empty();
    }

    public Map<String, Object> toPythonMap(UserProfile p) {
        return p == null ? Map.of() : p.toPythonMap();
    }
}
