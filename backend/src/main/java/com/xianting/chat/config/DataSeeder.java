package com.xianting.chat.config;

import com.xianting.chat.common.PasswordUtil;
import com.xianting.chat.entity.*;
import com.xianting.chat.repository.*;
import com.xianting.chat.entity.*;
import com.xianting.chat.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Seeds demo departments/agents/test user on first startup. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BalanceRepository balanceRepository;
    private final DepartmentRepository departmentRepository;
    private final AgentRepository agentRepository;
    private final InspirationKeywordRepository keywordRepository;
    private final InspirationVideoRepository videoRepository;

    @Value("${app.balance.start}")
    private long startBalance;

    public DataSeeder(UserRepository userRepository, BalanceRepository balanceRepository,
                      DepartmentRepository departmentRepository, AgentRepository agentRepository,
                      InspirationKeywordRepository keywordRepository, InspirationVideoRepository videoRepository) {
        this.userRepository = userRepository;
        this.balanceRepository = balanceRepository;
        this.departmentRepository = departmentRepository;
        this.agentRepository = agentRepository;
        this.keywordRepository = keywordRepository;
        this.videoRepository = videoRepository;
    }

    // v
    @Override
    public void run(String... args) {
        if (departmentRepository.count() == 0) {
            seedDepartmentsAndAgents();
        }
        if (userRepository.count() == 0) {
            seedUser();
        }
    }

    private void seedUser() {
        User user = new User();
        user.setPublicId(UUID.randomUUID().toString());
        user.setBrand("xianting");
        user.setEmail("test@xianting.cn");
        user.setPassword(PasswordUtil.hash("123456"));
        user.setName("测试用户");
        user.setEmailVerified(true);
        user.setDisabled(false);
        user.setContractReadAt(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        Balance balance = new Balance();
        balance.setUserId(user.getId());
        balance.setTokenCredits(startBalance);
        balance.setAutoRefillEnabled(true);
        balance.setExpireAt(LocalDateTime.now().plusYears(1));
        balanceRepository.save(balance);

        InspirationKeyword kw = new InspirationKeyword();
        kw.setUserId(user.getId());
        kw.setKeyword("Aigc培训");
        kw.setSortOrder(0);
        keywordRepository.save(kw);
        seedVideos(kw.getId());
    }

    private void seedVideos(Long keywordId) {
        record V(String author, String fans, String duration, String date, String title, String likes, boolean lowFans) {}
        List<V> videos = List.of(
                new V("老A教AI视频", "4.1w", "28:40", "2026-05-03", "【AI漫剧】保姆级教程，揭秘爆款AI漫剧制作工作流！第一期：剧本创作技巧+生成分镜图", "23.3w", false),
                new V("玲娜惫儿", "2.9w", "8:33", "2026-06-26", "AI漫剧0-1全流程教学~从脚本到发布，碎片时间攒起来，一周也能攒出一部剧。#AI漫剧 #AI视频", "18.2w", false),
                new V("小A教你学AI", "4.8w", "48:14", "2026-03-03", "一口气彻底教你AI漫剧，30分钟快速掌握AI漫剧制作 #AI漫剧 #AI动画 #AIGC", "9.5w", false),
                new V("宁说宁话", "6000", "3:10", "2026-05-09", "公司要求学AI，你也想考AI证书，给简历加分，到底考哪个？都有啥类型？", "8.9w", true),
                new V("小羊同学", "8000", "4:25", "2026-06-07", "「VibeCoding大赏」codex制作个人作品集网站，搭建个人网站的心得", "7.0w", true),
                new V("老炸AIGC", "9000", "51:32", "2026-07-10", "#AI漫剧 #AI短剧 #AI漫剧制作 #AI视频制作 全流程讲解", "5.5w", true),
                new V("小梨蛋包", "8.2w", "1:49", "2026-04-19", "力竭了...《AI真人短剧教程》也被我搞定 #AI短剧三步制作法 #AIGC", "4.9w", false),
                new V("AIComfyui", "1.9w", "23:26", "2026-06-16", "AI短剧实操全步骤，新人上手必看！#ai视频制作 #ai视频学习", "4.3w", false),
                new V("心流", "2.5w", "0:55", "2026-06-13", "想要做出爆款，你的资产就必须完整且优质，今天拆解大佬的资产提示词", "2.7w", false),
                new V("青苹果", "300", "0:17", "2026-06-25", "宝子们有空可以考这个证，看着很冷门，未来很吃香#人工智能#知识分享", "1.6w", true)
        );
        for (V v : videos) {
            InspirationVideo video = new InspirationVideo();
            video.setKeywordId(keywordId);
            video.setAuthor(v.author());
            video.setFans(v.fans());
            video.setDuration(v.duration());
            video.setPublishDate(v.date());
            video.setTitle(v.title());
            video.setLikes(v.likes());
            video.setLowFansHighLikes(v.lowFans());
            videoRepository.save(video);
        }
    }

    private void seedDepartmentsAndAgents() {
        record D(String name, String... agents) {}
        List<D> departments = List.of(
                new D("流量部门", "爆款选题助手", "标题优化大师", "热点追踪员"),
                new D("视频部门", "AI漫剧编剧", "分镜生成师", "视频剪辑顾问"),
                new D("文案编导组", "短视频脚本专家", "带货文案写手", "口播稿优化师", "文案提取助手"),
                new D("直播增长组", "直播话术教练", "直播间策划师"),
                new D("AI销售部门", "客户跟进助手", "成交话术专家", "私域运营顾问"),
                new D("职能成本部门", "合同审查助手", "财务分析助理", "HR面试助手"),
                new D("BOSS决策部门", "商业参谋", "数据分析决策师", "战略规划助手")
        );
        int order = 0;
        for (D d : departments) {
            Department dept = new Department();
            dept.setPublicId(UUID.randomUUID().toString());
            dept.setName(d.name());
            dept.setEnabled(true);
            dept.setBrandOnly(false);
            dept.setSortOrder(order++);
            departmentRepository.save(dept);

            for (String agentName : d.agents()) {
                seedAgent(dept, agentName, d.name(), "通用".equals(d.name()) ? "通用" : d.name());
            }
        }
        // a few extra agents visible in 智能体中心 (no department filter)
        Department boss = departmentRepository.findByNameOrPublicId("BOSS决策部门");
        for (String name : List.of("先霆全能助手", "灵感激发器")) {
            Agent agent = new Agent();
            agent.setPublicId(UUID.randomUUID().toString());
            agent.setDepartmentId(boss.getId());
            agent.setName(name);
            agent.setDescription("通用型智能体：" + name);
            agent.setCategory("通用");
            agent.setIsPreset(true);
            agent.setConversationStarters("[\"今天有什么可以帮你的？\"]");
            agent.setInstructions("你是通用助手「" + name + "」。");
            agent.setCreatedAt(LocalDateTime.now());
            agentRepository.save(agent);
        }
    }

    private void seedAgent(Department dept, String agentName, String deptName, String category) {
        Agent agent = new Agent();
        agent.setPublicId(UUID.randomUUID().toString());
        agent.setDepartmentId(dept.getId());
        agent.setName(agentName);
        boolean isCopywritingExtractor = "文案提取助手".equals(agentName);
        agent.setDescription(isCopywritingExtractor
                ? "粘贴视频链接，提取其中的口播与字幕文案。"
                : "我是" + deptName + "的" + agentName + "，可以帮你完成相关专业任务。");
        agent.setCategory(category);
        agent.setIsPreset(true);
        agent.setConversationStarters(isCopywritingExtractor ? "[]"
                : "[\"你好，请介绍一下你能做什么\",\"帮我出一个工作方案\",\"我遇到了一个问题，想请你分析\"]");
        agent.setInstructions(isCopywritingExtractor
                ? "你是视频文案提取助手。用户发送视频链接时，系统会自动提取口播和字幕文案；未提供链接时，请提示用户直接粘贴公开视频链接。"
                : "你是「" + agentName + "」，隶属于" + deptName + "。请用专业、友好的中文回答用户问题。");
        agent.setCreatedAt(LocalDateTime.now());
        agentRepository.save(agent);
    }
}
