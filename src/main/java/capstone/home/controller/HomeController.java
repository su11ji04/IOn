package capstone.home.controller;

import capstone.home.dto.UserProfileDto;
import capstone.home.service.HomeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/api/home")
    public ResponseEntity<UserProfileDto> getUserProfile(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        UserProfileDto dto = homeService.getUserProfileContent(userId);
        return ResponseEntity.ok(dto);
    }
}