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

import com.github.pagehelper.PageInfo;
import top.datawork.core.exception.NotFoundException;
import top.datawork.core.exception.ServerException;
import top.datawork.core.exception.UnAuthorizedException;
import top.datawork.fastbi.core.service.CheckEntityService;
import top.datawork.fastbi.dto.roleDto.RoleProject;
import top.datawork.fastbi.model.Project;
import top.datawork.fastbi.model.User;
import top.datawork.fastbi.dto.projectDto.*;

import java.util.List;

public interface ProjectService extends CheckEntityService {

    ProjectInfo getProjectInfo(Long id, User user);

    List<ProjectInfo> getProjects(User user) throws ServerException;

    ProjectInfo createProject(ProjectCreat projectCreat, User user) throws ServerException, UnAuthorizedException, NotFoundException;

    Project updateProject(Long id, ProjectUpdate projectUpdate, User user) throws ServerException, UnAuthorizedException, NotFoundException;

    boolean deleteProject(Long id, User user) throws ServerException, UnAuthorizedException, NotFoundException;

    Project transferProject(Long id, Long orgId, User user) throws ServerException, UnAuthorizedException, NotFoundException;

    PageInfo<ProjectWithCreateBy> searchProjects(String keywords, User user, int pageNum, int pageSize);

    boolean favoriteProject(Long id, User user) throws ServerException, UnAuthorizedException, NotFoundException;

    List<ProjectInfo> getFavoriteProjects(User user);

    boolean removeFavoriteProjects(User user, Long[] projectIds) throws ServerException, UnAuthorizedException, NotFoundException;

    List<RelProjectAdminDto> addAdmins(Long id, List<Long> adminIds, User user) throws ServerException, UnAuthorizedException, NotFoundException;

    boolean removeAdmin(Long relationId, User user) throws ServerException, UnAuthorizedException, NotFoundException;

    ProjectDetail getProjectDetail(Long id, User user, boolean modify) throws NotFoundException, UnAuthorizedException;

    List<RoleProject> postRoles(Long id, List<Long> roleIds, User user) throws ServerException, UnAuthorizedException, NotFoundException;

    PageInfo<ProjectWithCreateBy> getProjectsByOrg(Long id, User user, String keyword, int pageNum, int pageSize);

    ProjectPermission getProjectPermission(ProjectDetail projectDetail, User user);

    boolean allowGetData(ProjectDetail projectDetail, User user);

    List<RelProjectAdminDto> getAdmins(Long id, User user) throws NotFoundException, UnAuthorizedException;

    boolean isMaintainer(ProjectDetail projectDetail, User user);
}
