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

import top.datawork.fastbi.core.enums.DownloadType;
import top.datawork.fastbi.dto.viewDto.DownloadViewExecuteParam;
import top.datawork.fastbi.model.ShareDownloadRecord;

import java.util.List;

public interface ShareDownloadService {

    /**
     * 提交分享下载任务
     *
     * @param downloadType
     * @param uuid
     * @param downloadViewExecuteParams
     * @return
     */
    boolean submit(DownloadType downloadType, String uuid, List<DownloadViewExecuteParam> downloadViewExecuteParams);

    /**
     * 获取分享下载列表
     *
     * @param uuid
     * @return
     */
    List<ShareDownloadRecord> queryDownloadRecordPage(String uuid);


    /**
     * 下载
     *
     * @param id
     * @param uuid
     * @return
     */
    ShareDownloadRecord downloadById(String id, String uuid);
}
