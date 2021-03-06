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

import top.datawork.core.exception.ForbiddenException;
import top.datawork.core.exception.NotFoundException;
import top.datawork.core.exception.ServerException;
import top.datawork.core.exception.UnAuthorizedException;
import top.datawork.core.model.Paginate;
import top.datawork.fastbi.dto.userDto.UserLogin;
import top.datawork.fastbi.dto.viewDto.DistinctParam;
import top.datawork.fastbi.dto.viewDto.ViewExecuteParam;
import top.datawork.fastbi.model.User;
import top.datawork.fastbi.dto.shareDto.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface ShareService {
    ShareWidget getShareWidget(User user) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException;

    String generateShareToken(Long shareEntityId, String username, Long userId) throws ServerException;

    User shareLogin(UserLogin userLogin) throws NotFoundException, ServerException, UnAuthorizedException;

    ShareDisplay getShareDisplay(User user) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException;

    ShareDashboard getShareDashboard(User user) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException;

    Paginate<Map<String, Object>> getShareData(ViewExecuteParam executeParam, User user) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException, SQLException;

    List<Map<String, Object>> getDistinctValue(DistinctParam param, User user);

    void formatShareParam(Long projectId, ShareEntity entity);

    Map<String, Object> checkShareToken() throws ServerException, ForbiddenException;

    Map<String, Object> getSharePermissions() throws ServerException, ForbiddenException;

    @Deprecated
    ShareInfo getShareInfo(String token, User user) throws ServerException, ForbiddenException;

    @Deprecated
    void verifyShareUser(User user, ShareInfo shareInfo) throws ServerException, ForbiddenException;
}
