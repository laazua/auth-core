package com.authcore.service;

import com.authcore.common.BusinessException;
import com.authcore.dto.module.ModuleCreateDTO;
import com.authcore.dto.module.ModuleQueryDTO;
import com.authcore.dto.module.ModuleUpdateDTO;
import com.authcore.dto.module.ModuleVO;
import com.authcore.dto.permission.PermissionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 模块业务服务接口（modules/001）。
 */
public interface ModuleService {

    /**
     * 分页条件查询模块。
     *
     * @param dto 查询参数
     * @return 分页结果，含 list/total/page/size
     */
    IPage<ModuleVO> queryModules(ModuleQueryDTO dto);

    /**
     * 根据 ID 查询模块详情。
     *
     * @param id 模块 ID
     * @return 模块视图对象
     * @throws BusinessException code=1001 模块不存在
     */
    ModuleVO getModuleById(Long id);

    /**
     * 创建模块。
     *
     * @param dto 创建参数
     * @return 创建后的模块视图对象
     * @throws BusinessException code=1301 name 已存在；code=1302 code 已存在
     */
    ModuleVO createModule(ModuleCreateDTO dto);

    /**
     * 更新模块（仅 name、base_url、description、status，不改 code）。
     *
     * @param id  模块 ID
     * @param dto 更新参数
     * @return 更新后的模块视图对象
     * @throws BusinessException code=1001 模块不存在；code=1301 name 已存在
     */
    ModuleVO updateModule(Long id, ModuleUpdateDTO dto);

    /**
     * 查询模块下的权限级联列表。
     *
     * @param moduleId 模块 ID
     * @return 权限视图对象列表
     */
    List<PermissionVO> queryModulePermissions(Long moduleId);

    /**
     * 删除模块（引用保护：存在 sys_permission 则拒绝）。
     *
     * @param id 模块 ID
     * @throws BusinessException code=1001 模块不存在；code=1302 存在权限引用
     */
    void deleteModule(Long id);
}