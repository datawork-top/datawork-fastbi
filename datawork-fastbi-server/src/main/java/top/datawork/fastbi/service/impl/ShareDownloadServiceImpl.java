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

import top.datawork.core.exception.ServerException;
import top.datawork.core.exception.UnAuthorizedException;
import top.datawork.fastbi.core.common.ErrorMsg;
import top.datawork.fastbi.core.enums.ActionEnum;
import top.datawork.fastbi.core.enums.DownloadTaskStatus;
import top.datawork.fastbi.core.enums.DownloadType;
import top.datawork.fastbi.dao.ShareDownloadRecordMapper;
import top.datawork.fastbi.dto.projectDto.ProjectDetail;
import top.datawork.fastbi.dto.projectDto.ProjectPermission;
import top.datawork.fastbi.dto.viewDto.DownloadViewExecuteParam;
import top.datawork.fastbi.model.ShareDownloadRecord;
import top.datawork.fastbi.service.ShareDownloadService;
import top.datawork.fastbi.service.ShareService;
import top.datawork.fastbi.service.excel.ExecutorUtil;
import top.datawork.fastbi.service.excel.MsgWrapper;
import top.datawork.fastbi.service.excel.WidgetContext;
import top.datawork.fastbi.service.excel.WorkBookContext;
import top.datawork.fastbi.service.share.ShareFactor;
import top.datawork.fastbi.service.share.aspect.ShareAuthAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ShareDownloadServiceImpl extends DownloadCommonService implements ShareDownloadService {

    @Autowired
    private ShareDownloadRecordMapper shareDownloadRecordMapper;

    @Autowired
    private ShareService shareService;

    @Override
    public boolean submit(DownloadType downloadType, String uuid, List<DownloadViewExecuteParam> params) {
        ShareFactor shareFactor = ShareAuthAspect.SHARE_FACTOR_THREAD_LOCAL.get();
        try {
            List<WidgetContext> widgetList = getWidgetContexts(downloadType, shareFactor.getEntityId(), shareFactor.getUser(), params);

            ShareDownloadRecord record = new ShareDownloadRecord();
            record.setUuid(uuid);
            record.setName(getDownloadFileName(downloadType, shareFactor.getEntityId()));
            record.setStatus(DownloadTaskStatus.PROCESSING.getStatus());
            record.setCreateTime(new Date());
            shareDownloadRecordMapper.insertSelective(record);

            MsgWrapper wrapper = new MsgWrapper(record, ActionEnum.SHAREDOWNLOAD, uuid);
            WorkBookContext workBookContext = WorkBookContext.WorkBookContextBuilder.newBuilder()
                    .withWrapper(wrapper)
                    .withWidgets(widgetList)
                    .withUser(shareFactor.getUser())
                    .withResultLimit(resultLimit)
                    .withTaskKey("ShareDownload_" + uuid)
                    .build();
            ExecutorUtil.submitWorkbookTask(workBookContext, null);
            log.info("Share download task submit:{}", wrapper);
            return true;
        } catch (UnAuthorizedException | ServerException e) {
            throw e;
        } catch (Exception e) {
            log.error("Submit download task error", e);
            return false;
        }
    }


    @Override
    public List<ShareDownloadRecord> queryDownloadRecordPage(String uuid) {
        ShareFactor shareFactor = ShareAuthAspect.SHARE_FACTOR_THREAD_LOCAL.get();
        ProjectDetail projectDetail = shareFactor.getProjectDetail();
        if (projectDetail == null) {
            return null;
        }
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, shareFactor.getUser());
        if (!projectPermission.getDownloadPermission()) {
            return null;
        }
        return shareDownloadRecordMapper.getShareDownloadRecordsByUuid(uuid);
    }

    @Override
    public ShareDownloadRecord downloadById(String id, String uuid) throws UnAuthorizedException {
        ShareFactor shareFactor = ShareAuthAspect.SHARE_FACTOR_THREAD_LOCAL.get();
        ProjectDetail projectDetail = shareFactor.getProjectDetail();
        if (projectDetail == null) {
            throw new UnAuthorizedException(ErrorMsg.ERR_MSG_PERMISSION);
        }
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, shareFactor.getUser());
        if (!projectPermission.getDownloadPermission()) {
            throw new UnAuthorizedException(ErrorMsg.ERR_MSG_PERMISSION);
        }
        ShareDownloadRecord record = shareDownloadRecordMapper.getShareDownloadRecordBy(Long.valueOf(id), uuid);
        if (record != null) {
            record.setLastDownloadTime(new Date());
            record.setStatus(DownloadTaskStatus.DOWNLOADED.getStatus());
            shareDownloadRecordMapper.updateById(record);
            return record;
        } else {
            return null;
        }
    }
}
