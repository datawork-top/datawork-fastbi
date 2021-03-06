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

package top.datawork.core.common.quartz;

import com.alibaba.druid.util.StringUtils;
import top.datawork.core.model.ScheduleJob;
import top.datawork.core.utils.DateUtils;
import top.datawork.core.utils.LockFactory;
import top.datawork.core.utils.QuartzHandler;
import top.datawork.fastbi.core.common.Constants;
import top.datawork.fastbi.core.config.SpringContextHolder;
import top.datawork.fastbi.core.enums.CheckEntityEnum;
import top.datawork.fastbi.core.enums.LockType;
import top.datawork.fastbi.core.enums.LogNameEnum;
import top.datawork.fastbi.service.excel.ExecutorUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuartzJobExecutor implements Job {

    private static final Logger scheduleLogger = LoggerFactory.getLogger(LogNameEnum.BUSINESS_SCHEDULE.getName());

    public static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        ExecutorUtil.printThreadPoolStatusLog(executorService, "Cronjob_Executor", scheduleLogger);
        executorService.submit(() -> {
            TriggerKey triggerKey = jobExecutionContext.getTrigger().getKey();
            ScheduleJob scheduleJob = (ScheduleJob) jobExecutionContext.getMergedJobDataMap().get(QuartzHandler.getJobDataKey(triggerKey));
            if (scheduleJob == null) {
            	scheduleLogger.warn("ScheduleJob({}) is not found", triggerKey.getName());
                return;
            }
            
			Long id = scheduleJob.getId();
            if (scheduleJob.getStartDate().getTime() > System.currentTimeMillis()
                    || scheduleJob.getEndDate().getTime() < System.currentTimeMillis()) {
            	 Object[] args = {
            			 id,
                         DateUtils.toyyyyMMddHHmmss(System.currentTimeMillis()),
                         DateUtils.toyyyyMMddHHmmss(scheduleJob.getStartDate()),
                         DateUtils.toyyyyMMddHHmmss(scheduleJob.getEndDate()),
                         scheduleJob.getCronExpression()
                 };
                 scheduleLogger.warn("ScheduleJob({}), currentTime:{} is not within the planned execution time, startTime:{}, endTime:{}, cronExpression:{}", args);
                 return;
            }

			String jobType = scheduleJob.getJobType().trim();
			ScheduleService scheduleService = (ScheduleService) SpringContextHolder
					.getBean(jobType + "ScheduleService");
			if (StringUtils.isEmpty(jobType) || scheduleService == null) {
				scheduleLogger.warn("Unknown job type {}, jobId:{}", jobType, scheduleJob.getId());
				return;
			}
			
			try {
				String lockKey = CheckEntityEnum.CRONJOB.getSource().toUpperCase() + Constants.AT_SYMBOL + id + Constants.AT_SYMBOL + "EXECUTED";
				if (!LockFactory.getLock(lockKey, 500, LockType.REDIS).getLock()) {
					scheduleLogger.warn("ScheduleJob({}) has been executed by other instance", id);
					return;
				}
				scheduleService.execute(id);
			} catch (Exception e) {
				scheduleLogger.error("ScheduleJob({}) execute error:{}", id, e.getMessage());
				scheduleLogger.error(e.getMessage(), e);
			}
        });
    }
}
