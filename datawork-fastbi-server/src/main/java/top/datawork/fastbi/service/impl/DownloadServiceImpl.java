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

import com.alibaba.druid.util.StringUtils;
import top.datawork.core.exception.UnAuthorizedException;
import top.datawork.fastbi.core.enums.ActionEnum;
import top.datawork.fastbi.core.enums.DownloadTaskStatus;
import top.datawork.fastbi.core.enums.DownloadType;
import top.datawork.fastbi.core.enums.LogNameEnum;
import top.datawork.fastbi.dao.DownloadRecordMapper;
import top.datawork.fastbi.dao.UserMapper;
import top.datawork.fastbi.dto.viewDto.DownloadViewExecuteParam;
import top.datawork.fastbi.model.DownloadRecord;
import top.datawork.fastbi.model.User;
import top.datawork.fastbi.service.DownloadService;
import top.datawork.fastbi.service.excel.ExecutorUtil;
import top.datawork.fastbi.service.excel.MsgWrapper;
import top.datawork.fastbi.service.excel.WidgetContext;
import top.datawork.fastbi.service.excel.WorkBookContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 *
 * @Author daemon
 * @Date 19/5/28 10:04
 * To change this template use File | Settings | File Templates.
 */
@Service
@Slf4j
public class DownloadServiceImpl extends DownloadCommonService implements DownloadService {

    private static final Logger downloadLogger = LoggerFactory.getLogger(LogNameEnum.BUSINESS_DOWNLOAD.getName());

    @Autowired
    private DownloadRecordMapper downloadRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<DownloadRecord> queryDownloadRecordPage(Long userId) {
        return downloadRecordMapper.getDownloadRecordsByUser(userId);
    }

    @Override
    public DownloadRecord downloadById(Long id, String token) throws UnAuthorizedException {
        if (StringUtils.isEmpty(token)) {
            throw new UnAuthorizedException();
        }

        String username = tokenUtils.getUsername(token);
        if (StringUtils.isEmpty(username)) {
            throw new UnAuthorizedException();
        }

        User user = userMapper.selectByUsername(username);
        if (null == user) {
            throw new UnAuthorizedException();
        }

        DownloadRecord record = downloadRecordMapper.getById(id);

        if (!record.getUserId().equals(user.getId())) {
            throw new UnAuthorizedException();
        }

        record.setLastDownloadTime(new Date());
        record.setStatus(DownloadTaskStatus.DOWNLOADED.getStatus());
        downloadRecordMapper.updateById(record);
        return record;
    }

    @Override
    public Boolean submit(DownloadType type, Long id, User user, List<DownloadViewExecuteParam> params) {
        try {
            List<WidgetContext> widgetList = getWidgetContexts(type, id, user, params);
            DownloadRecord record = new DownloadRecord();
            record.setName(getDownloadFileName(type, id));
            record.setUserId(user.getId());
            record.setCreateTime(new Date());
            record.setStatus(DownloadTaskStatus.PROCESSING.getStatus());
            downloadRecordMapper.insert(record);
            MsgWrapper wrapper = new MsgWrapper(record, ActionEnum.DOWNLOAD, record.getId());

            WorkBookContext workBookContext = WorkBookContext.WorkBookContextBuilder.newBuilder()
                    .withWrapper(wrapper)
                    .withWidgets(widgetList)
                    .withUser(user)
                    .withResultLimit(resultLimit)
                    .withTaskKey("DownloadTask_" + id)
                    .withCustomLogger(downloadLogger)
                    .build();

            ExecutorUtil.submitWorkbookTask(workBookContext, downloadLogger);
            log.info("Download task submit:{}", wrapper);
        } catch (Exception e) {
            log.error("Submit download task error", e);
            return false;
        }
        return true;
    }
}
