package com.xianting.manage.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xianting.manage.common.ApiException;
import com.xianting.manage.common.PageResult;
import com.xianting.manage.common.PasswordUtil;
import com.xianting.manage.converter.UserAdminConverter;
import com.xianting.manage.dto.CreateUserRequest;
import com.xianting.manage.dto.UserAdminDto;
import com.xianting.manage.entity.Balance;
import com.xianting.manage.entity.Plan;
import com.xianting.manage.entity.User;
import com.xianting.manage.repository.BalanceRepository;
import com.xianting.manage.repository.PlanRepository;
import com.xianting.manage.repository.UserRepository;
import com.xianting.manage.service.UserAdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final BalanceRepository balanceRepository;
    private final PlanRepository planRepository;
    private final UserAdminConverter userAdminConverter;

    /** 新用户初始积分，与客户端 app.balance.start 保持一致 */
    @Value("${app.balance.start}")
    private long startBalance;

    public UserAdminServiceImpl(UserRepository userRepository, BalanceRepository balanceRepository,
                                PlanRepository planRepository, UserAdminConverter userAdminConverter) {
        this.userRepository = userRepository;
        this.balanceRepository = balanceRepository;
        this.planRepository = planRepository;
        this.userAdminConverter = userAdminConverter;
    }

    @Override
    public PageResult<UserAdminDto> page(long page, long size, String keyword, String planPublicId) {
        Long planIdFilter = null;
        if (planPublicId != null && !planPublicId.isBlank()) {
            planIdFilter = requirePlan(planPublicId).getId();
        }
        Page<User> p = userRepository.pageByKeyword(page, size, keyword, planIdFilter);
        List<User> users = p.getRecords();
        if (users.isEmpty()) return PageResult.of(p, List.of());

        // 分页后批量组装积分与套餐，避免 N+1
        List<Long> userIds = users.stream().map(User::getId).toList();
        Map<Long, Balance> balanceByUser = balanceRepository.listByUserIds(userIds).stream()
                .collect(Collectors.toMap(Balance::getUserId, Function.identity(), (a, b) -> a));
        Set<Long> planIds = new HashSet<>();
        for (User u : users) {
            if (u.getPlanId() != null) planIds.add(u.getPlanId());
        }
        Map<Long, Plan> planById = planIds.isEmpty() ? Map.of()
                : planRepository.listByIds(planIds).stream()
                        .collect(Collectors.toMap(Plan::getId, Function.identity()));

        List<UserAdminDto> rows = new ArrayList<>();
        for (User u : users) {
            Plan plan = u.getPlanId() == null ? null : planById.get(u.getPlanId());
            rows.add(userAdminConverter.toDto(u, balanceByUser.get(u.getId()),
                    plan == null ? null : plan.getPublicId(),
                    plan == null ? null : plan.getName()));
        }
        return PageResult.of(p, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAdminDto create(CreateUserRequest request) {
        // 邮箱/密码的必填、格式与长度由 CreateUserRequest 注解校验，此处只做归一化与唯一性
        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword();
        if (userRepository.existsByEmail(email)) throw new ApiException(409, "该邮箱已注册");

        User user = new User();
        user.setPublicId(UUID.randomUUID().toString());
        user.setBrand("xianting");
        user.setEmail(email);
        user.setPassword(PasswordUtil.hash(password));
        user.setName(request.getName() == null || request.getName().isBlank() ? "新用户" : request.getName().trim());
        user.setEmailVerified(false);
        user.setDisabled(false);
        if (request.getPlanId() != null && !request.getPlanId().isBlank()) {
            user.setPlanId(requirePlan(request.getPlanId()).getId());
        }
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        // 与客户端注册一致：建号同时初始化积分记录
        Balance balance = new Balance();
        balance.setUserId(user.getId());
        balance.setTokenCredits(startBalance);
        balance.setAutoRefillEnabled(true);
        balance.setExpireAt(LocalDateTime.now().plusYears(1));
        balanceRepository.save(balance);

        return toDto(user);
    }

    @Override
    public UserAdminDto assignPlan(String userPublicId, String planPublicId) {
        User user = requireUser(userPublicId);
        Long planId = null;
        if (planPublicId != null && !planPublicId.isBlank()) {
            planId = requirePlan(planPublicId).getId();
        }
        user.setPlanId(planId);
        userRepository.updateById(user);
        return toDto(user);
    }

    @Override
    public UserAdminDto setDisabled(String userPublicId, boolean disabled) {
        User user = requireUser(userPublicId);
        user.setDisabled(disabled);
        userRepository.updateById(user);
        return toDto(user);
    }

    /** 单行组装：积分与套餐各一次点查（仅编辑后回显用，列表页走批量） */
    private UserAdminDto toDto(User user) {
        List<Balance> balances = balanceRepository.listByUserIds(List.of(user.getId()));
        Balance balance = balances.isEmpty() ? null : balances.get(0);
        Plan plan = user.getPlanId() == null ? null : planRepository.getById(user.getPlanId());
        return userAdminConverter.toDto(user, balance,
                plan == null ? null : plan.getPublicId(),
                plan == null ? null : plan.getName());
    }

    private User requireUser(String publicId) {
        User user = userRepository.findByPublicId(publicId);
        if (user == null) throw new ApiException(404, "用户不存在");
        return user;
    }

    private Plan requirePlan(String publicId) {
        Plan plan = planRepository.findByPublicId(publicId);
        if (plan == null) throw new ApiException(404, "套餐不存在");
        return plan;
    }
}
