package com.xianting.chat.controller;

import com.xianting.chat.common.Result;
import com.xianting.chat.common.UserContext;
import com.xianting.chat.dto.InspirationKeywordDto;
import com.xianting.chat.service.InspirationService;
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
    public Result<Map<String, Object>> inspiration() {
        return Result.ok(inspirationService.list(UserContext.require()));
    }

    @PostMapping("/keywords")
    public Result<InspirationKeywordDto> addKeyword(@RequestBody Map<String, String> body) {
        return Result.ok(inspirationService.addKeyword(UserContext.require(), body.get("keyword")));
    }

    @DeleteMapping("/keywords/{id}")
    public Result<Map<String, Object>> deleteKeyword(@PathVariable Long id) {
        return Result.ok(inspirationService.deleteKeyword(UserContext.require(), id));
    }

    /** Manual refresh: costs credits, bumps the cache window (videos come from the seeded pool). */
    @PostMapping("/keywords/{id}/refresh")
    public Result<InspirationKeywordDto> refresh(@PathVariable Long id) {
        return Result.ok(inspirationService.refresh(UserContext.require(), id));
    }
}
