/*
 * <<
 *  EDP
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package top.datawork.fastbi.service.impl;

import top.datawork.core.exception.NotFoundException;
import top.datawork.core.exception.ServerException;
import top.datawork.core.exception.UnAuthorizedException;
import top.datawork.core.utils.CollectionUtils;
import top.datawork.fastbi.core.enums.LogNameEnum;
import top.datawork.fastbi.core.enums.UserOrgRoleEnum;
import top.datawork.fastbi.core.enums.UserPermissionEnum;
import top.datawork.fastbi.core.enums.VizVisibilityEnum;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.datawork.fastbi.dao.*;
import top.datawork.fastbi.dto.roleDto.*;
import top.datawork.fastbi.model.*;
import top.datawork.fastbi.service.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service("roleService")
public class RoleServiceImpl implements RoleService {

    private static final Logger optLogger = LoggerFactory.getLogger(LogNameEnum.BUSINESS_OPERATION.getName());

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private RelUserOrganizationMapper relUserOrganizationMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RelRoleUserMapper relRoleUserMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private RelRoleProjectMapper relRoleProjectMapper;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private RelRoleViewMapper relRoleViewMapper;

    @Autowired
    private RelRolePortalMapper relRolePortalMapper;

    @Autowired
    private RelRoleDashboardMapper relRoleDashboardMapper;

    @Autowired
    private RelRoleDisplayMapper relRoleDisplayMapper;

    @Autowired
    private RelRoleSlideMapper relRoleSlideMapper;

    @Autowired
    private RelRoleDashboardWidgetMapper relRoleDashboardWidgetMapper;

    @Autowired
    private RelRoleDisplaySlideWidgetMapper relRoleDisplaySlideWidgetMapper;

    @Autowired
    private DisplayService displayService;

    @Autowired
    private DisplaySlideService displaySlideService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private DashboardPortalService dashboardPortalService;


    /**
     * 新建Role
     *
     * @param roleCreate
     * @param user
     * @return
     */
    @Override
    @Transactional
    public Role createRole(RoleCreate roleCreate, User user) throws ServerException, UnAuthorizedException, NotFoundException {
        Organization organization = organizationMapper.getById(roleCreate.getOrgId());
        if (null == organization) {
            log.info("Organization({}) is not found", roleCreate.getOrgId());
            throw new NotFoundException("Organization is not found");
        }

        RelUserOrganization rel = relUserOrganizationMapper.getRel(user.getId(), organization.getId());
        if (null == rel || !rel.getRole().equals(UserOrgRoleEnum.OWNER.getRole())) {
            log.info("User({}) have not permission to create role in organization({})", user.getId(),
                    organization.getId());
            throw new UnAuthorizedException("Insufficient permissions");
        }

        Role role = new Role().createdBy(user.getId());
        BeanUtils.copyProperties(roleCreate, role);

        int insert = roleMapper.insert(role);
        if (insert > 0) {
            optLogger.info("Role({}) is create by user({})", role.toString(), user.getId());
            organization.setRoleNum(organization.getRoleNum() + 1);
            organizationMapper.updateRoleNum(organization);
            return role;
        } else {
            log.info("Create role fail, role:{}", role.toString());
            throw new ServerException("Create role fail, unspecified error");
        }
    }

    /**
     * 删除 Role
     *
     * @param id
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean deleteRole(Long id, User user) throws ServerException, UnAuthorizedException, NotFoundException {

        Role role = null;
        try {
            role = getRole(id, user, true);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            log.info("User({}) have not permission to delete role", user.getId());
            throw new UnAuthorizedException("You have not permission to delete this role");
        }

        int delete = roleMapper.deleteById(id);
        if (delete > 0) {
            optLogger.info("Role({}) is delete by user({})", role.toString(), user.getId());

            Organization organization = organizationMapper.getById(role.getOrgId());
            if (null != organization) {
                int roleNum = organization.getRoleNum() - 1;
                organization.setRoleNum(roleNum > 0 ? roleNum : 0);
                organizationMapper.updateRoleNum(organization);
            }

            //删除Role关联project
            relRoleProjectMapper.deleteByRoleId(id);

            //删除Role关联view
            relRoleViewMapper.deleteByRoleId(id);

            relRoleUserMapper.deleteByRoleId(id);

            relRolePortalMapper.deleteByRoleId(id);

            relRoleDashboardMapper.deleteByRoleId(id);

            relRoleDisplayMapper.deleteByRoleId(id);

            relRoleSlideMapper.deleteByRoleId(id);

            relRoleDashboardWidgetMapper.deleteByRoleId(id);

            relRoleDisplaySlideWidgetMapper.deleteByRoleId(id);

            return true;
        } else {
            log.info("Delete role fail, role:{}", role.toString());
            throw new ServerException("Delete role fail, unspecified error");
        }
    }

    /**
     * 修改Role
     *
     * @param id
     * @param roleUpdate
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean updateRole(Long id, RoleUpdate roleUpdate, User user) throws ServerException, UnAuthorizedException, NotFoundException {

        Role role = null;
        try {
            role = getRole(id, user, true);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            throw new UnAuthorizedException("You have not permission to update this role");
        }

        String originInfo = role.toString();

        BeanUtils.copyProperties(roleUpdate, role);

        role.updatedBy(user.getId());

        int update = roleMapper.update(role);
        if (update > 0) {
            optLogger.info("Role({}) is update by user({}), origin:{}", role.toString(), user.getId(), originInfo);
            return true;
        } else {
            log.info("Update role fail, role:{}", role.toString());
            throw new ServerException("Update role fail, unspecified error");
        }
    }

    /**
     * 获取单条Role详情
     *
     * @param id
     * @param user
     * @return
     * @throws ServerException
     * @throws UnAuthorizedException
     * @throws NotFoundException
     */
    @Override
    public Role getRoleInfo(Long id, User user) throws ServerException, UnAuthorizedException, NotFoundException {
        return getRole(id, user, false);
    }

    @Override
    public List<Role> getRoleInfo(Long orgId, Long userId) {
        return roleMapper.getRolesByOrgAndUser(orgId, userId);
    }


    /**
     * 添加Role与User关联
     *
     * @param id
     * @param memberIds
     * @param user
     * @return
     * @throws ServerException
     * @throws UnAuthorizedException
     * @throws NotFoundException
     */
    @Override
    @Transactional
    public List<RelRoleMember> addMembers(Long id, List<Long> memberIds, User user) throws ServerException, UnAuthorizedException, NotFoundException {

        try {
            getRole(id, user, true);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            throw new UnAuthorizedException("Insufficient permissions");
        }

        if (CollectionUtils.isEmpty(memberIds)) {
            relRoleUserMapper.deleteByRoleId(id);
            return null;
        }

        List<User> members = userMapper.getByIds(memberIds);
        if (CollectionUtils.isEmpty(members)) {
            log.info("Members({}) are not found", memberIds);
            throw new NotFoundException("Members is not found");
        }

        relRoleUserMapper.deleteByRoleId(id);

        List<RelRoleUser> relRoleUsers = members.stream()
                .map(m -> new RelRoleUser(m.getId(), id).createdBy(user.getId()))
                .collect(Collectors.toList());

        int i = relRoleUserMapper.insertBatch(relRoleUsers);
        if (i > 0) {
            Map<Long, User> map = new HashMap<>();
            members.forEach(m -> map.put(m.getId(), m));
            return relRoleUsers.stream().map(r -> new RelRoleMember(r.getId(), map.get(r.getUserId()))).collect(Collectors.toList());
        } else {
            log.error("Add role member fail, role:{}, members:{}", id, memberIds.toString());
            throw new ServerException("Unspecified error");
        }

    }


    /**
     * 删除Role与User关联
     *
     * @param relationId
     * @param user
     * @return
     * @throws ServerException
     * @throws UnAuthorizedException
     * @throws NotFoundException
     */
    @Override
    @Transactional
    public boolean deleteMember(Long relationId, User user) throws ServerException, UnAuthorizedException, NotFoundException {
        RelRoleUser relRoleUser = relRoleUserMapper.getById(relationId);

        if (null == relRoleUser) {
            log.error("RelRoleUser({}) is not found", relationId);
            throw new NotFoundException("Member not found");
        }

        try {
            getRole(relRoleUser.getRoleId(), user, true);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            log.error("User({}) have not permission to delete relRoleUser({})", user.getId(), relationId);
            throw new UnAuthorizedException("Insufficient permissions");
        }

        if (user.getId().equals(relRoleUser.getUserId())) {
            throw new ServerException("You cannot remove yourself");
        }

        int i = relRoleUserMapper.deleteById(relationId);
        if (i > 0) {
            optLogger.info("RelRoleUser({}) is delete by user({})", relRoleUser.toString(), user.getId());
            return true;
        } else {
            log.error("Delete role member fail, relationId:{}", relationId);
            throw new ServerException("Unspecified error");
        }
    }


    @Override
    @Transactional
    public List<RelRoleMember> updateMembers(Long id, List<Long> memberIds, User user) throws ServerException, UnAuthorizedException, NotFoundException {

        getRole(id, user, true);

        List<User> users = userMapper.getByIds(memberIds);
        if (CollectionUtils.isEmpty(users)) {
            throw new ServerException("Members are not found");
        }

        List<Long> userIds = users.stream().map(u -> u.getId()).collect(Collectors.toList());

        List<Long> members = relRoleUserMapper.getUserIdsByRoleId(id);

        List<Long> deleteIds = members.stream().filter(mId -> !userIds.contains(mId)).collect(Collectors.toList());

        List<RelRoleUser> collect = userIds.stream().map(uId -> new RelRoleUser(uId, id)).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(deleteIds)) {
            relRoleUserMapper.deleteByRoleIdAndMemberIds(id, deleteIds);
        }
        relRoleUserMapper.insertBatch(collect);

        optLogger.info("Replace role({}) member by user({})", id, user.getId());
        return relRoleUserMapper.getMembersByRoleId(id);
    }

    /**
     * 获取单条Role所关联的Users
     *
     * @param id
     * @param user
     * @return
     * @throws ServerException
     * @throws UnAuthorizedException
     * @throws NotFoundException
     */
    @Override
    public List<RelRoleMember> getMembers(Long id, User user) throws ServerException, UnAuthorizedException, NotFoundException {
        try {
            getRole(id, user, false);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            log.error("User({}) have not permission to delete relRoleUser({})", user.getId(), id);
            throw new UnAuthorizedException("Insufficient permissions");
        }

        return relRoleUserMapper.getMembersByRoleId(id);
    }


    /**
     * 添加Role与Project关联
     *
     * @param id
     * @param projectId
     * @param user
     * @return
     * @throws ServerException
     * @throws UnAuthorizedException
     * @throws NotFoundException
     */
    @Override
    @Transactional
    public RoleProject addProject(Long id, Long projectId, User user) throws ServerException, UnAuthorizedException, NotFoundException {
        try {
            getRole(id, user, true);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            log.error("User({}}) have not permission to add relRoleProject, role:{}, project:{}", user.getId(), id, projectId);
            throw new UnAuthorizedException("Insufficient permissions");
        }

        Project project = projectMapper.getById(projectId);
        if (null == project) {
            log.error("Project({}) is not found", projectId);
            throw new NotFoundException("Project is not found");
        }

        RelRoleProject rel = relRoleProjectMapper.getByRoleAndProject(id, projectId);
        if (null != rel) {
            log.warn("RelRoleProject role:{}, project:{} is already exist", id, projectId);
            throw new ServerException("Role already exist");
        }

        RelRoleProject relRoleProject = new RelRoleProject(projectId, id).createdBy(user.getId());

        relRoleProjectMapper.insert(relRoleProject);
        if (null != relRoleProject.getId() && relRoleProject.getId().longValue() > 0L) {
            optLogger.info("Create relRoleProject({}) is update by user({})", relRoleProject.toString(), user.getId());
            RoleProject roleProject = new RoleProject(project);
            BeanUtils.copyProperties(relRoleProject, roleProject);
            return roleProject;
        } else {
            log.error("Add relRoleProject fail, role:{}, project:{}", id, projectId);
            throw new ServerException("Unspecified error");
        }
    }

    /**
     * 删除Role与Project关联
     *
     * @param roleId
     * @param projectId
     * @param user
     * @return
     * @throws ServerException
     * @throws UnAuthorizedException
     * @throws NotFoundException
     */
    @Override
    @Transactional
    public boolean deleteProject(Long roleId, Long projectId, User user) throws ServerException, UnAuthorizedException, NotFoundException {
        RelRoleProject relRoleProject = relRoleProjectMapper.getByRoleAndProject(roleId, projectId);

        if (null == relRoleProject) {
            log.error("RelRoleProject roleId:{}, projectId:{} is not found", roleId, projectId);
            throw new NotFoundException("Not found");
        }

        Role role = null;
        try {
            role = getRole(relRoleProject.getRoleId(), user, true);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            log.error("User({}) have not permission to delete relRoleProject({})", user.getId(), relRoleProject.getId());
            throw new UnAuthorizedException("Insufficient permissions");
        }

        int i = relRoleProjectMapper.deleteByRoleAndProject(roleId, projectId);
        if (i > 0) {
            relRoleDashboardMapper.deleteByRoleAndProject(roleId, projectId);
            relRoleDashboardWidgetMapper.deleteByRoleAndProject(roleId, projectId);
            relRoleDisplayMapper.deleteByRoleAndProject(roleId, projectId);
            relRoleDisplaySlideWidgetMapper.deleteByRoleAndProject(roleId, projectId);
            relRolePortalMapper.deleteByRoleAndProject(roleId, projectId);
            relRoleSlideMapper.deleteByRoleAndProject(roleId, projectId);
            relRoleViewMapper.deleteByRoleAndProject(roleId, projectId);
            
            optLogger.info("RelRoleProject({}) is delete by user({})", relRoleProject.toString(), user.getId());
            return true;
        } else {
            log.error("Delete role project fail, relationId:", role);
            throw new ServerException("Unspecified error");
        }
    }


    /**
     * 修改Role与Project关联信息
     *
     * @param roleId
     * @param projectId
     * @param user
     * @param projectRoleDto
     * @return
     * @throws ServerException
     * @throws UnAuthorizedException
     * @throws NotFoundException
     */
    @Override
    @Transactional
    public boolean updateProjectRole(Long roleId, Long projectId, User user, RelRoleProjectDto projectRoleDto) throws ServerException, UnAuthorizedException, NotFoundException {
        RelRoleProject relRoleProject = relRoleProjectMapper.getByRoleAndProject(roleId, projectId);
        if (null == relRoleProject) {
            log.warn("RelRoleProject is not found, roleId:{}, projectId:{}", roleId, projectId);
            throw new NotFoundException("Role not found");
        }

        String origin = relRoleProject.toString();

        Role role = null;
        try {
            role = getRole(roleId, user, false);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            log.error("User({}) have not permission to update relRoleProject, roleId:{}, projectId:{}", user.getId(), roleId, projectId);
            throw new UnAuthorizedException("Insufficient permissions");
        }

        //校验Project Admin权限
        projectService.getProjectDetail(relRoleProject.getProjectId(), user, true);

        //校验数据合法性
        UserPermissionEnum sourceP = UserPermissionEnum.permissionOf(relRoleProject.getSourcePermission());
        if (null == sourceP) {
            log.warn("Invalid source permission:{}", relRoleProject.getSourcePermission());
            throw new UnAuthorizedException("Invalid source permission");
        }

        UserPermissionEnum viewP = UserPermissionEnum.permissionOf(relRoleProject.getViewPermission());
        if (null == viewP) {
            log.warn("Invalid view permission:{}", relRoleProject.getSourcePermission());
            throw new UnAuthorizedException("Invalid view permission");
        }

        UserPermissionEnum widgetP = UserPermissionEnum.permissionOf(relRoleProject.getWidgetPermission());
        if (null == widgetP) {
            log.warn("Invalid widget permission:{}", relRoleProject.getSourcePermission());
            throw new UnAuthorizedException("Invalid widget permission");
        }

        UserPermissionEnum vizP = UserPermissionEnum.permissionOf(relRoleProject.getVizPermission());
        if (null == vizP) {
            log.warn("Invalid viz permission:{}", relRoleProject.getSourcePermission());
            throw new UnAuthorizedException("Invalid viz permission");
        }

        UserPermissionEnum scheduleP = UserPermissionEnum.permissionOf(relRoleProject.getSchedulePermission());
        if (null == scheduleP) {
            log.warn("Invalid schedule permission:{}", relRoleProject.getSourcePermission());
            throw new UnAuthorizedException("Invalid schedule permission");
        }

        BeanUtils.copyProperties(projectRoleDto, relRoleProject);
        relRoleProject.updatedBy(user.getId());
        int i = relRoleProjectMapper.update(relRoleProject);

        if (i > 0) {
            optLogger.info("RelRoleProject({}) is update by user({}), origin:{}", relRoleProject.toString(), user.getId(), origin);
            return true;
        } else {
            log.info("Update role fail, role:{}", role.toString());
            throw new ServerException("Update role fail, unspecified error");
        }

    }

    /**
     * 获取单个Organization里的Role列表
     *
     * @param orgId
     * @param user
     * @return
     * @throws ServerException
     * @throws UnAuthorizedException
     * @throws NotFoundException
     */
    @Override
    public List<RoleBaseInfo> getRolesByOrgId(Long orgId, User user) throws ServerException, UnAuthorizedException, NotFoundException {
        Organization organization = organizationMapper.getById(orgId);
        if (null == organization) {
            log.info("Organization({}) is not found", orgId);
            throw new NotFoundException("Organization is not found");
        }

        RelUserOrganization rel = relUserOrganizationMapper.getRel(user.getId(), organization.getId());
        if (null == rel) {
            log.info("User({}) have not permission to create role in organization({})", user.getId(), organization.getId());
            throw new UnAuthorizedException("Insufficient permissions");
        }

        return roleMapper.getBaseInfoByOrgId(orgId);
    }


    /**
     * 获取单个关联的Role列表
     *
     * @param projectId
     * @param user
     * @return
     * @throws ServerException
     * @throws UnAuthorizedException
     * @throws NotFoundException
     */
    @Override
    public List<RoleBaseInfo> getRolesByProjectId(Long projectId, User user) throws ServerException, UnAuthorizedException, NotFoundException {
        projectService.getProjectDetail(projectId, user, false);
        List<RoleBaseInfo> list = relRoleProjectMapper.getRoleBaseInfoByProject(projectId);
        return list;
    }

    @Override
    public RoleWithProjectPermission getRoleByProject(Long projectId, Long roleId, User user) {
        projectService.getProjectDetail(projectId, user, false);
        RoleWithProjectPermission projectPermission = relRoleProjectMapper.getPermission(projectId, roleId);
        return projectPermission;
    }

    @Override
    public VizPermission getVizPermission(Long id, Long projectId, User user) throws ServerException, UnAuthorizedException, NotFoundException {
        VizPermission vizPermission = new VizPermission();
        try {
            getRole(id, user, true);
            projectService.getProjectDetail(projectId, user, true);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException unAuthorizedException) {
            return vizPermission;
        }

        vizPermission.setPortals(relRolePortalMapper.getExcludePortals(id, projectId));
        vizPermission.setDashboards(relRoleDashboardMapper.getExcludeDashboards(id, projectId));
        vizPermission.setDisplays(relRoleDisplayMapper.getExcludeDisplays(id, projectId));
        vizPermission.setSlides(relRoleSlideMapper.getExcludeSlides(id, projectId));

        return vizPermission;
    }


    @Override
    public boolean postVizVisibility(Long id, VizVisibility vizVisibility, User user) throws ServerException, UnAuthorizedException, NotFoundException {
        VizVisibilityEnum visibilityEnum = VizVisibilityEnum.vizOf(vizVisibility.getViz());
        if (null == visibilityEnum) {
            throw new ServerException("Invalid viz");
        }

        Role role = getRole(id, user, true);

        boolean result = false;
        switch (visibilityEnum) {
            case PORTAL:
                result = dashboardPortalService.postPortalVisibility(role, vizVisibility, user);
                break;
            case DASHBOARD:
                result = dashboardService.postDashboardVisibility(role, vizVisibility, user);
                break;
            case DISPLAY:
                result = displayService.postDisplayVisibility(role, vizVisibility, user);
                break;
            case SLIDE:
                result = displaySlideService.postSlideVisibility(role, vizVisibility, user);
                break;
        }
        return result;
    }

    @Override
    public List<Role> getMemberRoles(Long orgId, Long memberId, User user) throws ServerException, UnAuthorizedException, NotFoundException {
        Organization organization = organizationMapper.getById(orgId);
        if (organization == null) {
            throw new NotFoundException("Organization is not found");
        }

        RelUserOrganization rel = relUserOrganizationMapper.getRel(user.getId(), orgId);
        if (null == rel) {
            throw new UnAuthorizedException();
        }

        return roleMapper.selectByOrgIdAndMemberId(orgId, memberId);
    }

    private Role getRole(Long id, User user, boolean modify) throws NotFoundException, UnAuthorizedException {
        Role role = roleMapper.getById(id);
        if (null == role) {
            log.warn("Role({}) is not found", id);
            throw new NotFoundException("Role is not found");
        }

        RelUserOrganization rel = relUserOrganizationMapper.getRel(user.getId(), role.getOrgId());
        if (null == rel) {
            throw new UnAuthorizedException();
        }

        if (modify && !rel.getRole().equals(UserOrgRoleEnum.OWNER.getRole())) {
            throw new UnAuthorizedException();
        }

        return role;
    }

}
