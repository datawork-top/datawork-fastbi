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
import top.datawork.fastbi.model.User;
import org.springframework.web.multipart.MultipartFile;
import top.datawork.fastbi.dto.organizationDto.*;

import java.util.List;
import java.util.Map;

public interface OrganizationService extends CheckEntityService {

    List<OrganizationInfo> getOrganizations(User user);

    boolean updateOrganization(OrganizationPut organizationPut, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    OrganizationBaseInfo createOrganization(OrganizationCreate organizationCreate, User user) throws ServerException;

    Map<String, String> uploadAvatar(Long id, MultipartFile file, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean deleteOrganization(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    OrganizationInfo getOrganization(Long id, User user) throws NotFoundException, UnAuthorizedException;

    List<OrganizationMember> getOrgMembers(Long id);

    void inviteMember(Long orgId, Long memId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    BatchInviteMemberResult batchInviteCustomMembers(Long orgId, InviteMembers inviteMembers, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    OrganizationInfo confirmInvite(String token, User user) throws ServerException;

    boolean deleteOrgMember(Long relationId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean updateMemberRole(Long relationId, User user, int role) throws NotFoundException, UnAuthorizedException, ServerException;

    void confirmInviteNoLogin(String token) throws NotFoundException, ServerException;
}

