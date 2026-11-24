package com.xianting.manage.config;

import com.xianting.manage.common.PasswordUtil;
import com.xianting.manage.entity.Admin;
import com.xianting.manage.entity.Department;
import com.xianting.manage.entity.Menu;
import com.xianting.manage.entity.Plan;
import com.xianting.manage.entity.PlanDepartment;
import com.xianting.manage.repository.AdminRepository;
import com.xianting.manage.repository.DepartmentRepository;
import com.xianting.manage.repository.MenuRepository;
import com.xianting.manage.repository.PlanDepartmentRepository;
import com.xianting.manage.repository.PlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 管理端幂等种子：超管账号 + 3 个套餐 + 套餐-部门默认关系 + 左侧导航菜单。
 * 部门数据由客户端 DataSeeder 负责；若管理端先于客户端启动（t_department 为空），
 * 跳过关系绑定，后续由管理员在套餐编辑页手工勾选。
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PlanRepository planRepository;
    private final PlanDepartmentRepository planDepartmentRepository;
    private final DepartmentRepository departmentRepository;
    private final MenuRepository menuRepository;

    public DataSeeder(AdminRepository adminRepository, PlanRepository planRepository,
                      PlanDepartmentRepository planDepartmentRepository,
                      DepartmentRepository departmentRepository,
                      MenuRepository menuRepository) {
        this.adminRepository = adminRepository;
        this.planRepository = planRepository;
        this.planDepartmentRepository = planDepartmentRepository;
        this.departmentRepository = departmentRepository;
        this.menuRepository = menuRepository;
    }

    @Override
    public void run(String... args) {
        if (adminRepository.count() == 0) {
            seedAdmin();
        }
        if (planRepository.count() == 0) {
            seedPlans();
        }
        if (menuRepository.count() == 0) {
            seedMenus();
        }
    }

    private void seedAdmin() {
        Admin admin = new Admin();
        admin.setPublicId(UUID.randomUUID().toString());
        admin.setUsername("admin");
        admin.setPassword(PasswordUtil.hash("admin123456"));
        admin.setName("超级管理员");
        admin.setDisabled(false);
        adminRepository.save(admin);
    }

    private void seedPlans() {
        Plan basic = seedPlan("基础版", 1, "流量与视频基础能力");
        Plan pro = seedPlan("专业版", 2, "增加文案、直播、销售能力");
        Plan enterprise = seedPlan("旗舰版", 3, "全部部门能力开放");

        if (planDepartmentRepository.count() > 0 || departmentRepository.count() == 0) {
            return;
        }
        // 按部门 sort_order 升序绑定：基础版前 2 个、专业版前 5 个、旗舰版全部
        List<Department> departments = departmentRepository.listAllOrderBySortOrder();
        bindDepartments(basic, departments, 2);
        bindDepartments(pro, departments, 5);
        bindDepartments(enterprise, departments, departments.size());
    }

    /** 左侧导航初始菜单，与原前端硬编码 MENU_ITEMS 一致，另加「菜单管理」自身 */
    private void seedMenus() {
        seedMenu("智能体管理", "/agents", "Cpu", 1);
        seedMenu("套餐管理", "/plans", "Goods", 2);
        seedMenu("部门管理", "/departments", "OfficeBuilding", 3);
        seedMenu("用户管理", "/users", "User", 4);
        seedMenu("菜单管理", "/menus", "Menu", 5);
        seedMenu("管理员管理", "/admins", "Key", 6);
    }

    private void seedMenu(String name, String path, String icon, int sortOrder) {
        Menu menu = new Menu();
        menu.setPublicId(UUID.randomUUID().toString());
        menu.setName(name);
        menu.setPath(path);
        menu.setIcon(icon);
        menu.setSortOrder(sortOrder);
        menuRepository.save(menu);
    }

    private Plan seedPlan(String name, int level, String description) {
        Plan plan = new Plan();
        plan.setPublicId(UUID.randomUUID().toString());
        plan.setName(name);
        plan.setLevel(level);
        plan.setDescription(description);
        planRepository.save(plan);
        return plan;
    }

    private void bindDepartments(Plan plan, List<Department> departments, int count) {
        for (Department dept : departments.subList(0, Math.min(count, departments.size()))) {
            PlanDepartment rel = new PlanDepartment();
            rel.setPlanId(plan.getId());
            rel.setDepartmentId(dept.getId());
            planDepartmentRepository.save(rel);
        }
    }
}
