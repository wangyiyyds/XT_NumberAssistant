package com.xianting.chat.controller;

import com.xianting.chat.common.Result;
import com.xianting.chat.common.UserContext;
import com.xianting.chat.dto.ConversationDto;
import com.xianting.chat.dto.MessageDto;
import com.xianting.chat.service.ConversationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/convos")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(name = "agent_id", required = false) String agentPublicId) {
        return Result.ok(conversationService.list(UserContext.require(), agentPublicId));
    }

    @GetMapping("/{publicId}/messages")
    public Result<List<MessageDto>> messages(@PathVariable String publicId) {
        return Result.ok(conversationService.messages(UserContext.require(), publicId));
    }

    @PatchMapping("/{publicId}")
    public Result<ConversationDto> rename(@PathVariable String publicId, @RequestBody Map<String, String> body) {
        return Result.ok(conversationService.rename(UserContext.require(), publicId, body.get("title")));
    }

    @DeleteMapping("/{publicId}")
    public Result<Map<String, Object>> delete(@PathVariable String publicId) {
        return Result.ok(conversationService.delete(UserContext.require(), publicId));
    }
}
