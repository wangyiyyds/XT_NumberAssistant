package com.xianting.chat.controller;

import com.xianting.chat.common.UserContext;
import com.xianting.chat.entity.User;
import com.xianting.chat.service.UserService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-ai")
public class TestDeepseekController {

    private static final String SYSTEM_TEMPLATE = """
            你是一个资深的短视频文案编导。
            当前服务的用户信息：
            姓名:{name}
            你擅长针对不同特征的主题视频，撰写极具吸引力和网感的文案。
            你的文案风格要符合各大短视频平台（如抖音、小红书、视频号）的爆款逻辑。
            """;

    private final ChatClient chatClient;
    private final UserService userService;

    public TestDeepseekController(ChatClient.Builder builder, UserService userService) {
        this.chatClient = builder.build();
        this.userService = userService;
    }

    @GetMapping
    public String testPromptTemplate(@RequestParam String userText) {
        try {
            // 当前登录用户：AuthInterceptor 解析 Bearer token 后把 userId 放进 UserContext（ThreadLocal）
            Long userId = UserContext.require();
            User user = userService.requireUser(userId);
            String content = chatClient.prompt()
                    // system：模板文本和它的参数必须一起给，.param() 渲染的就是这里的 .text()
                    .system(s -> s.text(SYSTEM_TEMPLATE)
                            .param("name", user.getName()))
                    .user(u -> u.text("{question}").param("question", userText))
                    .call()
                    .content();
            System.out.println(content);
            return content;
        } catch (Exception e) {
            e.printStackTrace();
            return "调用大模型失败: " + e.getMessage();
        }
    }

    /**
     * 流式版本：.stream().content() 返回 Flux<String>，模型每吐一段就推一段 SSE data。
     * Spring MVC 原生支持返回 Flux（经 ReactiveTypeHandler 适配），无需 webflux starter。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testPromptTemplateStream(@RequestParam String userText) {
        // UserContext 是 ThreadLocal，只在请求线程上有值；必须在返回 Flux 之前同步取好用户，
        // 因为 Flux 真正执行时已经在异步线程上（与 ChatController 传 userId 参数同一个道理）
        Long userId = UserContext.require();
        User user = userService.requireUser(userId);
        return chatClient.prompt()
                .system(s -> s.text(SYSTEM_TEMPLATE).param("name", user.getName()))
                .user(u -> u.text("{question}").param("question", userText))
                .stream()
                .content();
    }
}
