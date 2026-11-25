package com.xianting.manage.service.impl;

import com.xianting.manage.converter.MenuConverter;
import com.xianting.manage.dto.MenuDto;
import com.xianting.manage.repository.MenuRepository;
import com.xianting.manage.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
