/*
 * <<
 *  Davinci
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import top.datawork.core.utils.CollectionUtils;
import top.datawork.fastbi.core.enums.VizEnum;
import top.datawork.fastbi.core.model.RoleDisableViz;
import top.datawork.fastbi.dao.DashboardMapper;
import top.datawork.fastbi.dao.DashboardPortalMapper;
import top.datawork.fastbi.dao.DisplayMapper;
import top.datawork.fastbi.dao.DisplaySlideMapper;
import top.datawork.fastbi.dao.RelRoleDashboardMapper;
import top.datawork.fastbi.dao.RelRoleDisplayMapper;
import top.datawork.fastbi.dao.RelRolePortalMapper;
import top.datawork.fastbi.dao.RelRoleSlideMapper;
import top.datawork.fastbi.dao.RoleMapper;
import top.datawork.fastbi.dto.projectDto.ProjectPermission;
import top.datawork.fastbi.model.Dashboard;
import top.datawork.fastbi.model.DashboardPortal;
import top.datawork.fastbi.model.Display;
import top.datawork.fastbi.model.DisplaySlide;
import top.datawork.fastbi.model.User;


@Component
public class VizCommonService extends BaseEntityService {

    @Autowired
    protected DashboardPortalMapper dashboardPortalMapper;

    @Autowired
    protected DashboardMapper dashboardMapper;

    @Autowired
    protected DisplayMapper displayMapper;

    @Autowired
    protected DisplaySlideMapper displaySlideMapper;

    @Autowired
    protected RelRolePortalMapper relRolePortalMapper;

    @Autowired
    protected RelRoleDashboardMapper relRoleDashboardMapper;

    @Autowired
    protected RelRoleDisplayMapper relRoleDisplayMapper;

    @Autowired
    protected RelRoleSlideMapper relRoleSlideMapper;

    @Autowired
    protected RoleMapper roleMapper;
    
	protected boolean isDisableVizs(ProjectPermission projectPermission, List<Long> disableVizs, Long id) {
        return projectPermission == null || (!projectPermission.isProjectMaintainer() && disableVizs.contains(id));
   }

	protected boolean isDisablePortal(Long portalId, Long projectId, User user, ProjectPermission projectPermission) {
        List<Long> disableVizs = getDisableVizs(user.getId(), projectId, null, VizEnum.PORTAL);
        return isDisableVizs(projectPermission, disableVizs, portalId);
   }
	
	protected boolean isDisableDashboard(Long dashboardId, Long portalId, User user, ProjectPermission projectPermission) {
        List<Long> disableVizs = getDisableVizs(user.getId(), portalId, null, VizEnum.DASHBOARD);
        return isDisableVizs(projectPermission, disableVizs, dashboardId);
   }
	
	protected boolean isDisableDisplay(Long displayId, Long projectId, User user, ProjectPermission projectPermission) {
        List<Long> disableVizs = getDisableVizs(user.getId(), projectId, null, VizEnum.DISPLAY);
        return isDisableVizs(projectPermission, disableVizs, displayId);
   }
	
	protected boolean isDisableDisplaySlide(Long slideId, Long displayId, User user, ProjectPermission projectPermission) {
        List<Long> disableVizs = getDisableVizs(user.getId(), displayId, null, VizEnum.SLIDE);
        return isDisableVizs(projectPermission, disableVizs, slideId);
   }

    /**
     * 获取当前用户被禁viz
     *
     * @param userId
     * @param featureId
     * @param allVizs
     * @param vizEnum
     * @return
     */
    protected List<Long> getDisableVizs(Long userId, Long featureId, List<Long> allVizs, VizEnum vizEnum) {
        List<RoleDisableViz> disables = null;
        List<Long> allRoles = null;
        switch (vizEnum) {
            case PORTAL:
                disables = relRolePortalMapper.getDisablePortalByUser(userId, featureId);
                if (null == allVizs) {
                    List<DashboardPortal> dashboardPortals = dashboardPortalMapper.getByProject(featureId);
                    if (!CollectionUtils.isEmpty(dashboardPortals)) {
                        allVizs = dashboardPortals.stream().map(DashboardPortal::getId).collect(Collectors.toList());
                    }
                }
                allRoles = roleMapper.getRolesByUserAndProject(userId, featureId);
                break;
            case DASHBOARD:
                disables = relRoleDashboardMapper.getDisableByUser(userId, featureId);
                if (null == allVizs) {
                    List<Dashboard> dashboardList = dashboardMapper.getByPortalId(featureId);
                    if (!CollectionUtils.isEmpty(dashboardList)) {
                        allVizs = dashboardList.stream().map(Dashboard::getId).collect(Collectors.toList());
                    }
                }
                allRoles = roleMapper.getRolesByUserAndPortal(userId, featureId);
                break;
            case DISPLAY:
                disables = relRoleDisplayMapper.getDisableDisplayByUser(userId, featureId);
                if (null == allVizs) {
                    List<Display> displayList = displayMapper.getByProject(featureId);
                    if (!CollectionUtils.isEmpty(displayList)) {
                        allVizs = displayList.stream().map(Display::getId).collect(Collectors.toList());
                    }
                }
                allRoles = roleMapper.getRolesByUserAndProject(userId, featureId);
                break;
            case SLIDE:
                disables = relRoleSlideMapper.getDisableSlides(userId, featureId);
                if (null == allVizs) {
                    List<DisplaySlide> slideList = displaySlideMapper.selectByDisplayId(featureId);
                    if (!CollectionUtils.isEmpty(slideList)) {
                        allVizs = slideList.stream().map(DisplaySlide::getId).collect(Collectors.toList());
                    }
                }
                allRoles = roleMapper.getRolesByUserAndDisplay(userId, featureId);
                break;
            default:
                throw new IllegalArgumentException("Unknown viz type");
        }

        if (!CollectionUtils.isEmpty(disables) && !CollectionUtils.isEmpty(allVizs)) {
            Map<Long, List<RoleDisableViz>> map = disables.stream().collect(Collectors.groupingBy(RoleDisableViz::getRoleId));

            if (!CollectionUtils.isEmpty(allRoles)) {
                allRoles.forEach(r -> {
                    if (!map.containsKey(r)) {
                        map.put(r, null);
                    }
                });
            }

            if (map.size() == 1) {
                List<Long> list = new ArrayList<>();
                map.forEach((k, v) -> {
                    if (!CollectionUtils.isEmpty(v)) {
                        list.addAll(v.stream().map(RoleDisableViz::getVizId).collect(Collectors.toSet()));
                    }
                });
                return list;
            } else {
                Set<Long> visibleSet = new HashSet<>();
                List<Long> finalAllVizs = allVizs;
                map.forEach((k, v) -> {
                    if (!CollectionUtils.isEmpty(v)) {
                        Set<Long> roleDisables = v.stream().map(RoleDisableViz::getVizId).collect(Collectors.toSet());
                        visibleSet.addAll(finalAllVizs.stream().filter(l -> !roleDisables.contains(l)).collect(Collectors.toSet()));
                    } else {
                        visibleSet.addAll(finalAllVizs);
                    }
                });
                allVizs.removeAll(visibleSet);
                return allVizs;
            }
        }
        return new ArrayList<>();
    }
}
