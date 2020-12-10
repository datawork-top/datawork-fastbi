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

import com.alibaba.fastjson.JSON;
import top.datawork.core.utils.QuartzHandler;
import top.datawork.fastbi.core.enums.CronJobStatusEnum;
import top.datawork.fastbi.core.enums.LogNameEnum;
import top.datawork.fastbi.core.service.RedisMessageHandler;
import top.datawork.fastbi.dao.CronJobMapper;
import top.datawork.fastbi.model.CronJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class CronJobMessageHandler implements RedisMessageHandler {

	private static final Logger scheduleLogger = LoggerFactory.getLogger(LogNameEnum.BUSINESS_SCHEDULE.getName());

	@Autowired
	private CronJobMapper cronJobMapper;

	@Autowired
	private QuartzHandler quartzHandler;

	@Override
	public void handle(Object message, String flag) {

		// the flag is deprecated
		log.info("CronJobHandler received stop message({}), flag:{}", message, flag);

		if (!(message instanceof String)) {
			return;
		}

		CronJob cronJob = JSON.parseObject((String) message, CronJob.class);

		quartzHandler.removeJob(cronJob);
		scheduleLogger.info("CronJob({}) is stopped", cronJob.getId());
		cronJob.setJobStatus(CronJobStatusEnum.STOP.getStatus());
		cronJob.setUpdateTime(new Date());
		cronJobMapper.update(cronJob);
	}
}
