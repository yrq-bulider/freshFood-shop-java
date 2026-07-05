package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.SaManager;
import com.yan.freshfood.admin.dto.BannerCreateDTO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.user.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.model.entity.product.BannerDO;
import com.yan.freshfood.model.entity.product.CategoryDO;
import com.yan.freshfood.user.mapper.BannerMapper;
import com.yan.freshfood.admin.vo.AdminCategoryTreeVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentAdminServiceImplTest {

    @Mock private BannerMapper bannerMapper;
    @Mock private com.yan.freshfood.user.mapper.HotWordMapper hotWordMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks private ContentAdminServiceImpl service;

    @Test
    void banner_create_inserts_with_default_sort() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            BannerCreateDTO dto = new BannerCreateDTO();
            dto.setTitle("618 大促");
            dto.setImage("https://img.example.com/b1.jpg");
            dto.setLinkType("CATEGORY");
            dto.setLinkTarget("1");
            dto.setEnabled(1);
            // sort 故意不设，应默认为 0

            service.bannerCreate(dto);

            org.mockito.ArgumentCaptor<BannerDO> captor =
                    org.mockito.ArgumentCaptor.forClass(BannerDO.class);
            org.mockito.Mockito.verify(bannerMapper).insert(captor.capture());
            BannerDO captured = captor.getValue();
            assertEquals("618 大促", captured.getTitle());
            assertEquals(0, captured.getSort()); // 默认值
        }
    }

    @Test
    void banner_delete_throws_when_not_found() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            when(bannerMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.bannerDelete(99L));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Test
    void category_tree_builds_nested_structure() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            CategoryDO top = new CategoryDO();
            top.setId(1L); top.setParentId(0L); top.setName("水果"); top.setSort(1); top.setStatus(1);
            CategoryDO child = new CategoryDO();
            child.setId(11L); child.setParentId(1L); child.setName("车厘子"); child.setSort(1); child.setStatus(1);
            when(categoryMapper.selectList(any())).thenReturn(List.of(top, child));

            List<AdminCategoryTreeVO> tree = service.categoryTree();

            assertEquals(1, tree.size());
            AdminCategoryTreeVO root = tree.get(0);
            assertEquals("水果", root.getName());
            assertEquals(1, root.getChildren().size());
            assertEquals("车厘子", root.getChildren().get(0).getName());
        }
    }

    @Test
    void category_delete_throws_when_has_children() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            CategoryDO c = new CategoryDO();
            c.setId(1L);
            when(categoryMapper.selectById(1L)).thenReturn(c);
            when(categoryMapper.selectCount(any())).thenReturn(2L); // 有 2 个子分类

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.categoryDelete(1L));
            assertEquals(ErrorCode.CATEGORY_IN_USE.getCode(), ex.getCode());
        }
    }
}
