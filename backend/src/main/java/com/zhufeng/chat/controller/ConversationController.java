package com.zhufeng.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhufeng.chat.common.ApiException;
import com.zhufeng.chat.common.UserContext;
import com.zhufeng.chat.entity.Conversation;
import com.zhufeng.chat.entity.Message;
import com.zhufeng.chat.mapper.ConversationMapper;
import com.zhufeng.chat.mapper.MessageMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/convos")
public class ConversationController {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final AgentController agentController;

    public ConversationController(ConversationMapper conversationMapper, MessageMapper messageMapper,
                                  AgentController agentController) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.agentController = agentController;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(name = "agent_id", required = false) String agentPublicId) {
        Long userId = UserContext.require();
        QueryWrapper<Conversation> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        if (agentPublicId != null && !agentPublicId.isBlank()) {
            qw.eq("agent_id", agentController.findByPublicId(agentPublicId).getId());
        }
        qw.orderByDesc("updated_at");
        List<Map<String, Object>> convos = new ArrayList<>();
        for (Conversation c : conversationMapper.selectList(qw)) {
            convos.add(toVo(c));
        }
        return Map.of("conversations", convos);
    }

    @GetMapping("/{publicId}/messages")
    public List<Map<String, Object>> messages(@PathVariable String publicId) {
        Conversation c = requireOwn(publicId);
        QueryWrapper<Message> qw = new QueryWrapper<>();
        qw.eq("conversation_id", c.getId()).orderByAsc("id");
        List<Map<String, Object>> out = new ArrayList<>();
        for (Message m : messageMapper.selectList(qw)) {
            out.add(toVo(m, c.getPublicId()));
        }
        return out;
    }

    @PatchMapping("/{publicId}")
    public Map<String, Object> rename(@PathVariable String publicId, @RequestBody Map<String, String> body) {
        Conversation c = requireOwn(publicId);
        String title = body.get("title");
        if (title == null || title.isBlank()) throw new ApiException(400, "title is required");
        c.setTitle(title.trim());
        c.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(c);
        return toVo(c);
    }

    @DeleteMapping("/{publicId}")
    public Map<String, Object> delete(@PathVariable String publicId) {
        Conversation c = requireOwn(publicId);
        QueryWrapper<Message> mqw = new QueryWrapper<>();
        mqw.eq("conversation_id", c.getId());
        messageMapper.delete(mqw);
        conversationMapper.deleteById(c.getId());
        return Map.of("success", true);
    }

    private Conversation requireOwn(String publicId) {
        Long userId = UserContext.require();
        QueryWrapper<Conversation> qw = new QueryWrapper<>();
        qw.eq("public_id", publicId).eq("user_id", userId);
        Conversation c = conversationMapper.selectOne(qw);
        if (c == null) throw new ApiException(404, "conversation not found");
        return c;
    }

    static Map<String, Object> toVo(Conversation c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", c.getPublicId());
        m.put("conversationId", c.getPublicId());
        m.put("title", c.getTitle());
        m.put("endpoint", c.getEndpoint());
        m.put("createdAt", c.getCreatedAt());
        m.put("updatedAt", c.getUpdatedAt());
        return m;
    }

    static Map<String, Object> toVo(Message msg, String conversationPublicId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", msg.getPublicId());
        m.put("messageId", msg.getPublicId());
        m.put("conversationId", conversationPublicId);
        m.put("parentMessageId", msg.getParentMessageId());
        m.put("sender", msg.getSender());
        m.put("isCreatedByUser", "user".equals(msg.getSender()));
        m.put("text", msg.getText());
        m.put("error", Boolean.TRUE.equals(msg.getError()));
        m.put("createdAt", msg.getCreatedAt());
        return m;
    }
}
