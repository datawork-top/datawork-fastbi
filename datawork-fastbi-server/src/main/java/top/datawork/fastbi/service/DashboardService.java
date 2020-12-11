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

package top.datawork.fastbi.service;

import top.datawork.core.exception.NotFoundException;
import top.datawork.core.exception.ServerException;
import top.datawork.core.exception.UnAuthorizedException;
import top.datawork.fastbi.core.service.CheckEntityService;
import top.datawork.fastbi.dto.roleDto.VizVisibility;
import top.datawork.fastbi.dto.shareDto.ShareEntity;
import top.datawork.fastbi.model.Dashboard;
import top.datawork.fastbi.model.MemDashboardWidget;
import top.datawork.fastbi.model.Role;
import top.datawork.fastbi.model.User;
import top.datawork.fastbi.service.share.ShareResult;
import top.datawork.fastbi.dto.dashboardDto.*;

import java.util.List;

public interface DashboardService extends CheckEntityService {

    List<Dashboard> getDashboards(Long portalId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    DashboardWithMem getDashboardMemWidgets(Long portalId, Long dashboardId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    Dashboard createDashboard(DashboardCreate dashboardCreate, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    void updateDashboards(Long portalId, DashboardDto[] dashboards, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean deleteDashboard(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    List<MemDashboardWidget> createMemDashboardWidget(Long portalId, Long dashboardId, MemDashboardWidgetCreate[] memDashboardWidgetCreates, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean updateMemDashboardWidgets(Long portalId, User user, MemDashboardWidgetDto[] memDashboardWidgets) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean deleteMemDashboardWidget(Long relationId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    ShareResult shareDashboard(Long dashboardId, User user, ShareEntity shareEntity) throws NotFoundException, UnAuthorizedException, ServerException;

    void deleteDashboardAndPortalByProject(Long projectId) throws RuntimeException;

    List<Long> getExcludeRoles(Long id);

    boolean postDashboardVisibility(Role role, VizVisibility vizVisibility, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean updateMemDashboardWidgetAlias(Long relationId, String alias, User user) throws NotFoundException, UnAuthorizedException, ServerException;
}
