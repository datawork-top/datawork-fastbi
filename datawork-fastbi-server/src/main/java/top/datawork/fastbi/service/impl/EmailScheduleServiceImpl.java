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

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import top.datawork.core.common.quartz.ScheduleService;
import top.datawork.core.enums.MailContentTypeEnum;
import top.datawork.core.exception.ServerException;
import top.datawork.core.model.MailAttachment;
import top.datawork.core.model.MailContent;
import top.datawork.core.utils.CollectionUtils;
import top.datawork.core.utils.MailUtils;
import top.datawork.fastbi.core.common.Constants;
import top.datawork.fastbi.core.enums.ActionEnum;
import top.datawork.fastbi.core.enums.CronJobMediaType;
import top.datawork.fastbi.core.enums.FileTypeEnum;
import top.datawork.fastbi.core.enums.LogNameEnum;
import top.datawork.fastbi.dto.cronJobDto.CronJobConfig;
import top.datawork.fastbi.dto.cronJobDto.CronJobContent;
import top.datawork.fastbi.dto.cronJobDto.ExcelContent;
import top.datawork.fastbi.dto.cronJobDto.MsgMailExcel;
import top.datawork.fastbi.dto.dashboardDto.DashboardWithPortal;
import top.datawork.fastbi.dto.projectDto.ProjectDetail;
import top.datawork.fastbi.dto.widgetDto.WidgetWithRelationDashboardId;
import top.datawork.fastbi.dto.widgetDto.WidgetWithVizId;
import top.datawork.fastbi.model.*;
import top.datawork.fastbi.service.ProjectService;
import top.datawork.fastbi.service.excel.ExecutorUtil;
import top.datawork.fastbi.service.excel.MsgWrapper;
import top.datawork.fastbi.service.excel.WidgetContext;
import top.datawork.fastbi.service.excel.WorkBookContext;
import top.datawork.fastbi.service.screenshot.ImageContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.datawork.core.consts.Consts;
import top.datawork.fastbi.dao.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service("emailScheduleService")
public class EmailScheduleServiceImpl extends BaseScheduleService implements ScheduleService {

    private static final Logger scheduleLogger = LoggerFactory.getLogger(LogNameEnum.BUSINESS_SCHEDULE.getName());

    @Autowired
    private CronJobMapper cronJobMapper;

    @Autowired
    private MailUtils mailUtils;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WidgetMapper widgetMapper;

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private MemDashboardWidgetMapper memDashboardWidgetMapper;

    @Autowired
    private DisplayMapper displayMapper;

    @Autowired
    private ProjectService projectService;

    @Value("${source.result-limit:1000000}")
    private int resultLimit;

    @Override
    public void execute(long jobId) throws Exception {
        CronJob cronJob = cronJobMapper.getById(jobId);
        if (null == cronJob || StringUtils.isEmpty(cronJob.getConfig())) {
            scheduleLogger.error("CronJob({}) config is empty", jobId);
            return;
        }
        cronJobMapper.updateExecLog(jobId, "");
        CronJobConfig cronJobConfig = null;
        try {
            cronJobConfig = JSONObject.parseObject(cronJob.getConfig(), CronJobConfig.class);
        } catch (Exception e) {
        	scheduleLogger.error("Cronjob({}) parse config({}) error:{}", jobId, cronJob.getConfig(), e.getMessage());
            return;
        }

        if (StringUtils.isEmpty(cronJobConfig.getType())) {
            scheduleLogger.error("Cronjob({}) config type is empty", jobId);
            return;
        }

        scheduleLogger.info("CronJob({}) is start! --------------", jobId);

        List<ExcelContent> excels = null;
        List<ImageContent> images = null;

        User creator = userMapper.getById(cronJob.getCreateBy());

        if (cronJobConfig.getType().equals(CronJobMediaType.IMAGE.getType())) {
            images = generateImages(jobId, cronJobConfig, creator.getId());
        }

        if (cronJobConfig.getType().equals(CronJobMediaType.EXCEL.getType())) {
			excels = generateExcels(jobId, cronJobConfig, creator);
        }

        if (cronJobConfig.getType().equals(CronJobMediaType.IMAGEANDEXCEL.getType())) {
            images = generateImages(jobId, cronJobConfig, creator.getId());
            excels = generateExcels(jobId, cronJobConfig, creator);
        }

        List<MailAttachment> attachmentList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(excels)) {
            excels.forEach(excel -> attachmentList.add(new MailAttachment(excel.getName() + FileTypeEnum.XLSX.getFormat(), excel.getFile())));
        }

        if (!CollectionUtils.isEmpty(images)) {
            images.forEach(image -> {
                String contentId = CronJobMediaType.IMAGE.getType() +
                        Constants.UNDERLINE +
                        UUID.randomUUID().toString().replaceAll(Constants.MINUS, Consts.EMPTY);
                attachmentList.add(new MailAttachment(contentId, image.getImageFile(), image.getUrl(), true));
            });
        }

        if (CollectionUtils.isEmpty(attachmentList)) {
            scheduleLogger.warn("CronJob({}) email content is empty", jobId);
            return;
        }

        scheduleLogger.info("CronJob({}) is ready to send email", cronJob.getId());

        MailContent mailContent = null;
        try {
            mailContent = MailContent.MailContentBuilder.builder()
                    .withSubject(cronJobConfig.getSubject())
                    .withTo(cronJobConfig.getTo())
                    .withCc(cronJobConfig.getCc())
                    .withBcc(cronJobConfig.getBcc())
                    .withMainContent(MailContentTypeEnum.HTML)
                    .withHtmlContent(cronJobConfig.getContent())
                    .withTemplate(Constants.SCHEDULE_MAIL_TEMPLATE)
                    .withAttachments(attachmentList)
                    .build();
        } catch (ServerException e) {
            scheduleLogger.error("CronJob({}) build email content error:{}", jobId, e.getMessage());
        }
        mailUtils.sendMail(mailContent, null);
        scheduleLogger.info("CronJob({}) is finish! --------------", jobId);
    }

    /**
     * 根据job配置生成excel
     *
     * @param jobId
     * @param cronJobConfig
     * @return
     * @throws Exception
     */
    private List<ExcelContent> generateExcels(Long jobId, CronJobConfig cronJobConfig, User user) throws Exception {
        scheduleLogger.info("CronJob({}) fetching excel contents", jobId);

        Map<String, WorkBookContext> workBookContextMap = new HashMap<>();

        Map<String, Integer> vizOrderMap = new HashMap<>();
        Map<Long, Map<Long, Integer>> displayPageMap = new HashMap<>();
        Map<String, Integer> excelEntityOrderMap = new HashMap<>();

        List<CronJobContent> jobContentList = getCronJobContents(cronJobConfig, vizOrderMap, displayPageMap);

        if (CollectionUtils.isEmpty(jobContentList)) {
            scheduleLogger.warn("CronJob({}) excel entity is empty", jobId);
            return null;
        }

        for (CronJobContent cronJobContent : jobContentList) {
            int order = 0;
            if (cronJobContent.getContentType().equalsIgnoreCase(DISPLAY)) {
                if (vizOrderMap.containsKey(DISPLAY + Consts.AT_SYMBOL + cronJobContent.getId())) {
                    order = vizOrderMap.get(DISPLAY + Consts.AT_SYMBOL + cronJobContent.getId());
                }
                Display display = displayMapper.getById(cronJobContent.getId());
                List<WidgetWithVizId> widgetsWithSlideIdList = widgetMapper.queryByDisplayId(cronJobContent.getId());
                if (display != null && !CollectionUtils.isEmpty(widgetsWithSlideIdList)) {
                    ProjectDetail projectDetail = projectService.getProjectDetail(display.getProjectId(), user, false);
                    boolean isMaintainer = projectService.isMaintainer(projectDetail, user);
                    Map<Long, Integer> slidePageMap = displayPageMap.get(cronJobContent.getId());

                    Map<Long, List<WidgetWithVizId>> slideWidgetsMap = widgetsWithSlideIdList.stream().collect(Collectors.groupingBy(WidgetWithVizId::getVizId));
                    int slidePageSize = slideWidgetsMap.size();
                    List<Long> slideIds = new ArrayList<>();
                    if (CollectionUtils.isEmpty(cronJobContent.getItems())) {
                        //all of slides in display
                        slideIds.addAll(slideWidgetsMap.keySet());
                    } else {
                        //checked slides in display
                        slideIds = cronJobContent.getItems();
                    }
                    for (Long slideId : slideIds) {
                        List<WidgetWithVizId> widgets = slideWidgetsMap.get(slideId);
                        if (CollectionUtils.isEmpty(widgets)) {
                            continue;
                        }

                        List<WidgetContext> widgetContexts = new ArrayList<>();
                        widgets.forEach(widget -> {
                            widgetContexts.add(new WidgetContext(widget, isMaintainer, null));
                        });

                        WorkBookContext workBookContext = WorkBookContext.WorkBookContextBuilder.newBuilder()
                                .withWidgets(widgetContexts)
                                .withUser(user)
                                .withResultLimit(resultLimit)
                                .withTaskKey("Schedule_" + jobId)
                                .withCustomLogger(scheduleLogger)
                                .build();

                        int page = slidePageMap.get(slideId);
                        String workBookName = slidePageSize == 1 ? display.getName() : display.getName() + "(" + page + ")";
                        workBookContextMap.put(workBookName, workBookContext);
                        excelEntityOrderMap.put(workBookName, order + page);
                    }
                }
            } else {
                if (vizOrderMap.containsKey(DASHBOARD + Consts.AT_SYMBOL + cronJobContent.getId())) {
                    order = vizOrderMap.get(DASHBOARD + Consts.AT_SYMBOL + cronJobContent.getId());
                }
                DashboardWithPortal dashboard = dashboardMapper.getDashboardWithPortalAndProject(cronJobContent.getId());
                excelEntityOrderMap.put(dashboard.getName(), vizOrderMap.get(DASHBOARD + Consts.AT_SYMBOL + cronJobContent.getId()));

                ProjectDetail projectDetail = projectService.getProjectDetail(dashboard.getProject().getId(), user, false);
                boolean isMaintainer = projectService.isMaintainer(projectDetail, user);

                List<WidgetWithRelationDashboardId> widgets = widgetMapper.getByDashboard(cronJobContent.getId());
                if (!CollectionUtils.isEmpty(widgets)) {

                    List<MemDashboardWidget> mdws = memDashboardWidgetMapper.getByDashboardId(dashboard.getId());
                    Map<Long, MemDashboardWidget> mdwMap = mdws.stream().collect(Collectors.toMap(o -> o.getWidgetId(), o -> o, (oldV, newV) -> oldV));

                    List<WidgetContext> widgetContexts = new ArrayList<>();
                    widgets.forEach(w -> {
                        Widget widget = new Widget();
                        BeanUtils.copyProperties(w, widget);
                        WidgetContext context = new WidgetContext(widget, dashboard, mdwMap.get(widget.getId()), null);
                        context.setIsMaintainer(isMaintainer);
                        widgetContexts.add(context);
                    });

                    WorkBookContext workBookContext = WorkBookContext.WorkBookContextBuilder.newBuilder()
                            .withWidgets(widgetContexts)
                            .withUser(user)
                            .withResultLimit(resultLimit)
                            .withTaskKey("Schedule_" + jobId)
                            .withCustomLogger(scheduleLogger)
                            .build();

                    workBookContextMap.put(dashboard.getName(), workBookContext);
                    excelEntityOrderMap.put(dashboard.getName(), order);
                }
            }
        }


        if (CollectionUtils.isEmpty(workBookContextMap)) {
            scheduleLogger.warn("CronJob({}) workbook context is empty", jobId);
            return null;
        }

        List<ExcelContent> excelContents = new CopyOnWriteArrayList<>();
        Map<String, Future<String>> excelPathFutureMap = new LinkedHashMap<>();
        int contextSize = workBookContextMap.size();
        final AtomicInteger index = new AtomicInteger(1);
        workBookContextMap.forEach((name, context) -> {
            scheduleLogger.info("CronJob({}) submit workbook task:{}, thread:{}, total:{}", jobId, name, index, contextSize);
            try {
            	String uuid = UUID.randomUUID().toString().replace("-", Consts.EMPTY);
                context.setWrapper(new MsgWrapper(new MsgMailExcel(jobId), ActionEnum.MAIL, uuid));
                excelPathFutureMap.put(name, ExecutorUtil.submitWorkbookTask(context, scheduleLogger));
            }catch (Exception e) {
            	scheduleLogger.error("Cronjob({}) submit workbook task error, thread:{}", jobId, index.get());
            	scheduleLogger.error(e.getMessage(), e);
			}finally {
                index.incrementAndGet();
			}
        });

        excelPathFutureMap.forEach((name, future) -> {
            String excelPath = null;
            try {
                excelPath = future.get(1, TimeUnit.HOURS);
                scheduleLogger.info("CronJob({}) workbook task:{} finish", jobId, name);
            } catch (Exception e) {
            	scheduleLogger.info("CronJob({}) workbook task:{} error", jobId, name);
            	scheduleLogger.error(e.getMessage(), e);
            }
            if (!StringUtils.isEmpty(excelPath)) {
                excelContents.add(new ExcelContent(excelEntityOrderMap.get(name), name, excelPath));
            }
        });

        excelContents.sort(Comparator.comparing(ExcelContent::getOrder));
        scheduleLogger.info("CronJob({}) fetched excel contents, count:{}", jobId, excelContents.size());
        return excelContents.isEmpty() ? null : excelContents;
    }
}
