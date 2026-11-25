package com.xianting.manage.service.impl;

import com.xianting.manage.common.ApiException;
import com.xianting.manage.converter.MenuConverter;
import com.xianting.manage.dto.MenuDto;
import com.xianting.manage.dto.MenuSaveRequest;
import com.xianting.manage.entity.Menu;
import com.xianting.manage.repository.MenuRepository;
import com.xianting.manage.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final MenuConverter menuConverter;

    public MenuServiceImpl(MenuRepository menuRepository, MenuConverter menuConverter) {
        this.menuRepository = menuRepository;
        this.menuConverter = menuConverter;
    }

    @Override
    public List<MenuDto> list() {
        return menuRepository.listAllOrderBySortOrder().stream()
                .map(menuConverter::toDto)
                .toList();
    }

    @Override
    public MenuDto detail(String publicId) {
        return menuConverter.toDto(requireByPublicId(publicId));
    }

    @Override
    public MenuDto create(MenuSaveRequest request) {
        // 必填/格式/长度由 MenuSaveRequest 注解校验，此处只查 path 全表唯一
        if (menuRepository.countByPath(request.getPath().trim()) > 0) {
            throw new ApiException(409, "路由路径 " + request.getPath().trim() + " 已被其他菜单占用");
        }
        Menu menu = menuConverter.toEntity(request);
        menu.setName(request.getName().trim());
        menu.setPath(request.getPath().trim());
        menu.setPublicId(UUID.randomUUID().toString());
        menuRepository.save(menu);
        return detail(menu.getPublicId());
    }

    @Override
    public MenuDto update(String publicId, MenuSaveRequest request) {
        Menu menu = requireByPublicId(publicId);
        // uk_menu_path 全表唯一：路径变化时确认没有被别的菜单占用
        String newPath = request.getPath().trim();
        if (!menu.getPath().equals(newPath)) {
            Menu holder = menuRepository.listAllOrderBySortOrder().stream()
                    .filter(m -> m.getPath().equals(newPath))
                    .findFirst().orElse(null);
            if (holder != null && !holder.getId().equals(menu.getId())) {
                throw new ApiException(409, "路由路径 " + newPath + " 已被「" + holder.getName() + "」占用");
            }
        }
        menu.setName(request.getName().trim());
        menu.setPath(newPath);
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder());
        menuRepository.updateById(menu);
        return detail(publicId);
    }

    @Override
    public void delete(String publicId) {
        Menu menu = requireByPublicId(publicId);
        menuRepository.removeById(menu.getId());
    }

    private Menu requireByPublicId(String publicId) {
        Menu menu = menuRepository.findByPublicId(publicId);
        if (menu == null) throw new ApiException(404, "菜单不存在");
        return menu;
    }
}
