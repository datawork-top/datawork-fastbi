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

import com.google.common.collect.Lists;
import top.datawork.core.exception.UnAuthorizedException;
import top.datawork.core.utils.CollectionUtils;
import top.datawork.core.utils.DateUtils;
import top.datawork.core.utils.TokenUtils;
import top.datawork.fastbi.core.common.ErrorMsg;
import top.datawork.fastbi.core.enums.DownloadType;
import top.datawork.fastbi.dao.DashboardMapper;
import top.datawork.fastbi.dao.MemDashboardWidgetMapper;
import top.datawork.fastbi.dao.WidgetMapper;
import top.datawork.fastbi.dto.projectDto.ProjectDetail;
import top.datawork.fastbi.dto.projectDto.ProjectPermission;
import top.datawork.fastbi.dto.viewDto.DownloadViewExecuteParam;
import top.datawork.fastbi.dto.viewDto.ViewExecuteParam;
import top.datawork.fastbi.model.Dashboard;
import top.datawork.fastbi.model.MemDashboardWidget;
import top.datawork.fastbi.model.User;
import top.datawork.fastbi.model.Widget;
import top.datawork.fastbi.service.ProjectService;
import top.datawork.fastbi.service.excel.WidgetContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.datawork.core.consts.Consts;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DownloadCommonService {

    @Autowired
    protected MemDashboardWidgetMapper memDashboardWidgetMapper;

    @Autowired
    protected ProjectService projectService;

    @Autowired
    protected WidgetMapper widgetMapper;

    @Autowired
    protected DashboardMapper dashboardMapper;

    @Autowired
    protected TokenUtils tokenUtils;

    @Value("${source.result-limit:1000000}")
    protected int resultLimit;

    protected List<WidgetContext> getWidgetContextListByDashBoardId(List<Long> dashboardIds, List<DownloadViewExecuteParam> params) {
        List<WidgetContext> widgetList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(dashboardIds)) {
            return widgetList;
        }
        for (Long dashboardId : dashboardIds) {
            if (dashboardId == null || dashboardId <= 0) {
                continue;
            }
            Dashboard dashboard = dashboardMapper.getById(dashboardId);
            if (dashboard == null) {
                continue;
            }
            List<MemDashboardWidget> mdw = memDashboardWidgetMapper.getByDashboardId(dashboardId);
            if (CollectionUtils.isEmpty(mdw)) {
                continue;
            }
            Set<Long> widgetIds = mdw.stream().filter(Objects::nonNull).map(MemDashboardWidget::getWidgetId).collect(Collectors.toSet());
            List<Widget> widgets = widgetMapper.getByIds(widgetIds);
            if (!CollectionUtils.isEmpty(widgets)) {
                // order by mem_dashboard_widget create_time
                widgets = orderBy(mdw, widgets);
                Map<Long, MemDashboardWidget> map = mdw.stream().collect(Collectors.toMap(o -> o.getWidgetId(), o -> o, (oldV, newV) -> oldV));
                widgets.stream().forEach(t -> {
                    ViewExecuteParam executeParam = null;
                    if (!CollectionUtils.isEmpty(params) && map.containsKey(t.getId())) {
                        MemDashboardWidget memDashboardWidget = map.get(t.getId());
                        try {
                            executeParam = params.stream().filter(p -> null != p.getParam() && p.getId().equals(memDashboardWidget.getId())).findFirst().get().getParam();
                        } catch (Exception e) {
                        }
                    }
                    widgetList.add(new WidgetContext(t, dashboard, map.get(t.getId()), executeParam));
                });
            }
        }
        return widgetList;
    }

    private List<Widget> orderBy(List<MemDashboardWidget> memDashboardWidgets, List<Widget> widgets) {
        List<Widget> list = new ArrayList<>();
        memDashboardWidgets.forEach(m -> {
            list.add(widgets.stream().filter(w -> w.getId().equals(m.getWidgetId())).findFirst().get());
        });
        return list;
    }

    protected List<WidgetContext> getWidgetContextListByFolderDashBoardId(Long id) {
        List<WidgetContext> widgetList = Lists.newArrayList();
        if (id == null || id.longValue() < 0L) {
            return widgetList;
        }
        List<Dashboard> dashboardList = dashboardMapper.getSubDashboardById(id);
        if (CollectionUtils.isEmpty(dashboardList)) {
            return widgetList;
        }
        List<Long> dashboardIds = dashboardList.stream().filter(Objects::nonNull).map(Dashboard::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dashboardIds)) {
            return widgetList;
        }
        return getWidgetContextListByDashBoardId(dashboardIds, null);
    }

    protected String getDownloadFileName(DownloadType downloadType, Long id) {
        String fileName;
        switch (downloadType) {
            case Widget:
                Widget widget = widgetMapper.getById(id);
                fileName = widget.getName();
                break;
            case DashBoard:
            case DashBoardFolder:
                Dashboard dashboard = dashboardMapper.getById(id);
                fileName = dashboard.getName();
                break;
            default:
                throw new IllegalArgumentException("Unsupported download type:" + downloadType.name());
        }
        return fileName + Consts.UNDERLINE + DateUtils.toyyyyMMddHHmmss(System.currentTimeMillis());
    }

    protected List<WidgetContext> getWidgetContexts(DownloadType downloadType, Long id, User user, List<DownloadViewExecuteParam> params) {
        List<WidgetContext> widgetList = Lists.newArrayList();
        switch (downloadType) {
            case Widget:
                Widget widget = widgetMapper.getById(id);
                if (widget != null) {
                    ViewExecuteParam executeParam = null;
                    if (!CollectionUtils.isEmpty(params)) {
                        try {
                            executeParam = params.stream()
                                    .filter(p -> null != p.getParam() && p.getId().equals(widget.getId())).findFirst()
                                    .get().getParam();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    widgetList.add(new WidgetContext(widget, null, null, executeParam));
                }
                break;
            case DashBoard:
                List<WidgetContext> widgets = getWidgetContextListByDashBoardId(Lists.newArrayList(id), params);
                if (!CollectionUtils.isEmpty(widgets)) {
                    widgetList.addAll(widgets);
                }
                break;
            case DashBoardFolder:
                List<WidgetContext> widgets1 = getWidgetContextListByFolderDashBoardId(id);
                if (!CollectionUtils.isEmpty(widgets1)) {
                    widgetList.addAll(widgets1);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported download type:" + downloadType.name());
        }
        if (CollectionUtils.isEmpty(widgetList)) {
            throw new IllegalArgumentException("There is no widget to download");
        }
        for (WidgetContext context : widgetList) {
            ProjectDetail projectDetail = projectService.getProjectDetail(context.getWidget().getProjectId(), user, false);
            ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
            //校验权限
            if (!projectPermission.getDownloadPermission()) {
                log.error("User({}) have not permission to download the widget({})", user.getUsername(), id);
                throw new UnAuthorizedException(ErrorMsg.ERR_MSG_PERMISSION);
            }
            context.setIsMaintainer(projectService.isMaintainer(projectDetail, user));
        }
        return widgetList;
    }
}
