package com.zhufeng.chat.controller;

import com.zhufeng.chat.common.UserContext;
import com.zhufeng.chat.dto.InspirationKeywordDto;
import com.zhufeng.chat.service.InspirationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inspiration")
public class InspirationController {

    private final InspirationService inspirationService;

    public InspirationController(InspirationService inspirationService) {
        this.inspirationService = inspirationService;
    }

    @GetMapping
    public Map<String, Object> inspiration() {
        return inspirationService.list(UserContext.require());
    }

    @PostMapping("/keywords")
    public InspirationKeywordDto addKeyword(@RequestBody Map<String, String> body) {
        return inspirationService.addKeyword(UserContext.require(), body.get("keyword"));
    }

    @DeleteMapping("/keywords/{id}")
    public Map<String, Object> deleteKeyword(@PathVariable Long id) {
        return inspirationService.deleteKeyword(UserContext.require(), id);
    }

    /** Manual refresh: costs credits, bumps the cache window (videos come from the seeded pool). */
    @PostMapping("/keywords/{id}/refresh")
    public InspirationKeywordDto refresh(@PathVariable Long id) {
        return inspirationService.refresh(UserContext.require(), id);
    }
}
