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

package top.datawork.fastbi.service;

import top.datawork.core.exception.NotFoundException;
import top.datawork.core.exception.ServerException;
import top.datawork.core.exception.UnAuthorizedException;
import top.datawork.core.model.DBTables;
import top.datawork.fastbi.dto.sourceDto.DatasourceType;
import top.datawork.core.model.TableInfo;
import top.datawork.fastbi.core.service.CheckEntityService;
import top.datawork.fastbi.model.Source;
import top.datawork.fastbi.model.User;
import org.springframework.web.multipart.MultipartFile;
import top.datawork.fastbi.dto.sourceDto.*;

import java.util.List;

public interface SourceService extends CheckEntityService {

    List<Source> getSources(Long projectId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    Source createSource(SourceCreate sourceCreate, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    Source updateSource(SourceInfo sourceInfo, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean deleteSource(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean testSource(SourceTest sourceTest) throws ServerException;

    void validCsvmeta(Long sourceId, UploadMeta uploadMeta, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    Boolean dataUpload(Long sourceId, SourceDataUpload sourceDataUpload, MultipartFile file, User user, String type) throws NotFoundException, UnAuthorizedException, ServerException;

    List<String> getSourceDbs(Long id, User user) throws NotFoundException, ServerException;

    DBTables getSourceTables(Long id, String dbName, User user) throws NotFoundException;

    TableInfo getTableInfo(Long id, String dbName, String tableName, User user) throws NotFoundException;

    SourceDetail getSourceDetail(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    List<DatasourceType> getDatasources();

    boolean reconnect(Long id, DbBaseInfo dbBaseInfo, User user) throws NotFoundException, UnAuthorizedException, ServerException;
}
