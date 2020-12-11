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
import com.alibaba.fastjson.JSON;
import top.datawork.core.exception.NotFoundException;
import top.datawork.core.exception.ServerException;
import top.datawork.core.exception.UnAuthorizedException;
import top.datawork.core.model.PaginateWithQueryColumns;
import top.datawork.core.model.QueryColumn;
import top.datawork.core.utils.BaseLock;
import top.datawork.core.utils.CollectionUtils;
import top.datawork.core.utils.FileUtils;
import top.datawork.core.utils.ServerUtils;
import top.datawork.fastbi.core.common.ErrorMsg;
import top.datawork.fastbi.core.config.SpringContextHolder;
import top.datawork.fastbi.core.enums.CheckEntityEnum;
import top.datawork.fastbi.core.enums.FileTypeEnum;
import top.datawork.fastbi.core.enums.LogNameEnum;
import top.datawork.fastbi.core.enums.UserPermissionEnum;
import top.datawork.fastbi.core.model.SqlEntity;
import top.datawork.fastbi.core.utils.CsvUtils;
import top.datawork.fastbi.core.utils.ExcelUtils;
import top.datawork.fastbi.core.utils.SqlParseUtils;
import top.datawork.fastbi.core.utils.VizUtils;
import top.datawork.fastbi.dao.MemDashboardWidgetMapper;
import top.datawork.fastbi.dao.MemDisplaySlideWidgetMapper;
import top.datawork.fastbi.dao.ViewMapper;
import top.datawork.fastbi.dao.WidgetMapper;
import top.datawork.fastbi.dto.projectDto.ProjectDetail;
import top.datawork.fastbi.dto.projectDto.ProjectPermission;
import top.datawork.fastbi.dto.shareDto.ShareEntity;
import top.datawork.fastbi.dto.viewDto.SimpleView;
import top.datawork.fastbi.dto.viewDto.ViewExecuteParam;
import top.datawork.fastbi.dto.viewDto.ViewWithProjectAndSource;
import top.datawork.fastbi.dto.viewDto.ViewWithSource;
import top.datawork.fastbi.dto.widgetDto.WidgetCreate;
import top.datawork.fastbi.dto.widgetDto.WidgetUpdate;
import top.datawork.fastbi.dto.widgetDto.WidgetWithViewName;
import top.datawork.fastbi.model.SqlVariable;
import top.datawork.fastbi.model.User;
import top.datawork.fastbi.model.Widget;
import top.datawork.fastbi.service.ProjectService;
import top.datawork.fastbi.service.ShareService;
import top.datawork.fastbi.service.ViewService;
import top.datawork.fastbi.service.WidgetService;
import top.datawork.fastbi.service.share.ShareFactor;
import top.datawork.fastbi.service.share.ShareResult;
import top.datawork.fastbi.service.share.ShareType;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.datawork.core.consts.Consts;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static top.datawork.fastbi.common.utils.ScriptUtils.getViewExecuteParam;


@Service("widgetService")
@Slf4j
public class WidgetServiceImpl extends BaseEntityService implements WidgetService {
    private static final Logger optLogger = LoggerFactory.getLogger(LogNameEnum.BUSINESS_OPERATION.getName());

    @Autowired
    private WidgetMapper widgetMapper;

    @Autowired
    private ViewMapper viewMapper;

    @Autowired
    private MemDashboardWidgetMapper memDashboardWidgetMapper;

    @Autowired
    private MemDisplaySlideWidgetMapper memDisplaySlideWidgetMapper;

    @Autowired
    private ShareService shareService;

    @Autowired
    private ViewService viewService;

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private ServerUtils serverUtils;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SqlParseUtils sqlParseUtils;

    @Value("${sql_template_delimiter:$}")
    private String sqlTempDelimiter;

    @Autowired
    private String TOKEN_SECRET;

    private static final CheckEntityEnum entity = CheckEntityEnum.WIDGET;

    @Override
    public boolean isExist(String name, Long id, Long projectId) {
        Long widgetId = widgetMapper.getByNameWithProjectId(name, projectId);
        if (null != id && null != widgetId) {
            return !id.equals(widgetId);
        }
        return null != widgetId && widgetId.longValue() > 0L;
    }

    /**
     * 获取widgets列表
     *
     * @param projectId
     * @param user
     * @return
     */
    @Override
    public List<WidgetWithViewName> getWidgets(Long projectId, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        ProjectDetail projectDetail = null;
        try {
            projectDetail = projectService.getProjectDetail(projectId, user, false);
        } catch (UnAuthorizedException e) {
            return null;
        }

        List<WidgetWithViewName> widgets = widgetMapper.getByProject(projectId);

        if (null != widgets) {
            ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
            if (projectPermission.getVizPermission() == UserPermissionEnum.HIDDEN.getPermission() &&
                    projectPermission.getWidgetPermission() == UserPermissionEnum.HIDDEN.getPermission()) {
                return null;
            }
        }

        return widgets;
    }


    /**
     * 获取单个widget信息
     *
     * @param id
     * @param user
     * @return
     */
    @Override
    public Widget getWidget(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        Widget widget = widgetMapper.getById(id);

        if (null == widget) {
            log.info("Widget({}) is not found", id);
            throw new NotFoundException("Widget is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(widget.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
        if (projectPermission.getWidgetPermission() < UserPermissionEnum.READ.getPermission()) {
            throw new UnAuthorizedException(ErrorMsg.ERR_MSG_PERMISSION);
        }

        return widget;
    }

    /**
     * 创建widget
     *
     * @param widgetCreate
     * @param user
     * @return
     */
    @Override
    @Transactional
    public Widget createWidget(WidgetCreate widgetCreate, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        Long projectId = widgetCreate.getProjectId();
        checkWritePermission(entity, projectId, user, "create");

        String name = widgetCreate.getName();
        if (isExist(name, null, projectId)) {
            alertNameTaken(entity, name);
        }

        checkView(widgetCreate.getViewId());

        BaseLock lock = getLock(entity, name, projectId);

        if (lock != null && !lock.getLock()) {
            alertNameTaken(entity, name);
        }

        try {

            Widget widget = new Widget().createdBy(user.getId());
            BeanUtils.copyProperties(widgetCreate, widget);
            if (widgetMapper.insert(widget) <= 0) {
                throw new ServerException("Create widget fail");
            }

            optLogger.info("Widget({}) is create by user({})", widget.toString());
            return widget;

        } finally {
            releaseLock(lock);
        }
    }

    private void checkView(Long id) {
        if (null == viewMapper.getById(id)) {
            log.info("View({}) is not found", id);
            throw new NotFoundException("View not found");
        }
    }

    /**
     * 修改widget
     *
     * @param widgetUpdate
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean updateWidget(WidgetUpdate widgetUpdate, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        Long id = widgetUpdate.getId();
        Widget widget = getWidget(id);

        Long projectId = widget.getProjectId();
        checkWritePermission(entity, projectId, user, "update");

        String name = widgetUpdate.getName();
        if (isExist(name, id, projectId)) {
            alertNameTaken(entity, name);
        }

        checkView(widget.getViewId());

        BaseLock lock = getLock(entity, name, projectId);

        if (lock != null && !lock.getLock()) {
            alertNameTaken(entity, name);
        }

        try {

            String originStr = widget.toString();
            BeanUtils.copyProperties(widgetUpdate, widget);
            widget.updatedBy(user.getId());
            if (widgetMapper.update(widget) <= 0) {
                throw new ServerException("Update widget fail");
            }

            optLogger.info("Widget({}) is update by user({}), origin:{}", widget.toString(), user.getId(),
                    originStr);
            return true;

        } finally {
            releaseLock(lock);
        }
    }

    private Widget getWidget(Long id) {
        Widget widget = widgetMapper.getById(id);
        if (null == widget) {
            log.info("Widget({}) is not found", id);
            throw new NotFoundException("Widget is not found");
        }
        return widget;
    }

    /**
     * 删除widget
     *
     * @param id
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean deleteWidget(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        Widget widget = getWidget(id);

        checkDeletePermission(entity, widget.getProjectId(), user);

        memDashboardWidgetMapper.deleteByWidget(id);
        memDisplaySlideWidgetMapper.deleteByWidget(id);
        widgetMapper.deleteById(id);

        optLogger.info("Widget({}) is delete by user({})", widget.toString(), user.getId());
        return true;
    }


    /**
     * 共享widget
     *
     * @param id
     * @param user
     * @param shareEntity
     * @return
     */
    @Override
    public ShareResult shareWidget(Long id, User user, ShareEntity shareEntity) throws NotFoundException, UnAuthorizedException, ServerException {

        Widget widget = getWidget(id);
        checkSharePermission(entity, widget.getProjectId(), user);
        shareService.formatShareParam(widget.getProjectId(), shareEntity);
        ShareFactor shareFactor = ShareFactor.Builder
                .shareFactor()
                .withType(ShareType.WIDGET)
                .withShareEntity(shareEntity)
                .withEntityId(id)
                .withSharerId(user.getId())
                .build();

        return shareFactor.toShareResult(TOKEN_SECRET);
    }


    @Override
    public String generationFile(Long id, ViewExecuteParam executeParam, User user, String type) throws NotFoundException, ServerException, UnAuthorizedException {

        Widget widget = getWidget(id);

        ProjectDetail projectDetail = projectService.getProjectDetail(widget.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
        //校验权限
        if (!projectPermission.getDownloadPermission()) {
            log.info("User({}) have not permission to download the widget({})", user.getUsername(), id);
            throw new UnAuthorizedException("You have not permission to download the widget");
        }

        executeParam.setPageNo(-1);
        executeParam.setPageSize(-1);
        executeParam.setLimit(-1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String rootPath = fileUtils.fileBasePath +
                File.separator +
                "download" +
                File.separator +
                sdf.format(new Date()) +
                File.separator +
                type +
                File.separator;

        String filePath = null;
        try {
            if (type.equals(FileTypeEnum.CSV.getType())) {
                ViewWithSource viewWithSource = viewMapper.getViewWithSource(widget.getViewId());
                boolean maintainer = projectService.isMaintainer(projectDetail, user);
                PaginateWithQueryColumns paginate = viewService.getResultDataList(maintainer, viewWithSource, executeParam, user);
                List<QueryColumn> columns = paginate.getColumns();
                if (!CollectionUtils.isEmpty(columns)) {
                    File file = new File(rootPath);
                    if (!file.exists()) {
                        file.mkdirs();
                    }

                    String csvName = widget.getName() + "_" +
                            System.currentTimeMillis() +
                            UUID.randomUUID().toString().replace("-", Consts.EMPTY) +
                            FileTypeEnum.CSV.getFormat();

                    filePath = CsvUtils.formatCsvWithFirstAsHeader(rootPath, csvName, columns, paginate.getResultList());
                }
            } else if (type.equals(FileTypeEnum.XLSX.getType())) {

                String excelName = widget.getName() + "_" +
                        System.currentTimeMillis() +
                        UUID.randomUUID().toString().replace("-", Consts.EMPTY) +
                        FileTypeEnum.XLSX.getFormat();

                HashSet<Widget> widgets = new HashSet<>();
                widgets.add(widget);
                Map<Long, ViewExecuteParam> executeParamMap = new HashMap<>();
                executeParamMap.put(widget.getId(), executeParam);

                filePath = rootPath + excelName;
                writeExcel(widgets, projectDetail, executeParamMap, filePath, user, false);

            } else {
                throw new ServerException("Unknown file type");
            }
        } catch (Exception e) {
            throw new ServerException("Generation " + type + " error!");
        }

        return serverUtils.getHost() + fileUtils.formatFilePath(filePath);
    }


    /**
     * widget列表数据写入指定excel文件
     *
     * @param widgets
     * @param projectDetail
     * @param executeParamMap
     * @param filePath
     * @param user
     * @param containType
     * @return
     * @throws Exception
     */
    public File writeExcel(Set<Widget> widgets,
                           ProjectDetail projectDetail, Map<Long, ViewExecuteParam> executeParamMap,
                           String filePath, User user, boolean containType) throws Exception {

        if (StringUtils.isEmpty(filePath)) {
            throw new ServerException("Excel file path is empty");
        }

        if (!filePath.trim().toLowerCase().endsWith(FileTypeEnum.XLSX.getFormat())) {
            throw new ServerException("Unknown file format");
        }

        SXSSFWorkbook wb = new SXSSFWorkbook(1000);
        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(widgets.size(), 8));
        CountDownLatch countDownLatch = new CountDownLatch(widgets.size());
        int i = 1;
        boolean maintainer = projectService.isMaintainer(projectDetail, user);
        Iterator<Widget> iterator = widgets.iterator();
        while (iterator.hasNext()) {
            Widget widget = iterator.next();
            final String sheetName = widgets.size() == 1 ? "Sheet" : "Sheet" + (widgets.size() - (i - 1));
            executorService.execute(() -> {
                Sheet sheet = null;
                try {

                    ViewWithProjectAndSource viewWithProjectAndSource = viewMapper
                            .getViewWithProjectAndSourceById(widget.getViewId());

                    ViewExecuteParam executeParam = null;
                    if (null != executeParamMap && executeParamMap.containsKey(widget.getId())) {
                        executeParam = executeParamMap.get(widget.getId());
                    } else {

                        Set<SimpleView> simpleViews = new HashSet<>();

                        // widget controller view
                        Map<String, Object> widgetConfigMap = JSON.parseObject(widget.getConfig(), Map.class);
                        if (!CollectionUtils.isEmpty(widgetConfigMap)) {
                            simpleViews.addAll(VizUtils.getControllerViews((List<Map<String, Object>>) widgetConfigMap.get("controls")));
                        }

                        // widget view
                        simpleViews.add(((ViewMapper) SpringContextHolder.getBean(ViewMapper.class)).getSimpleViewById(widget.getViewId()));

                        executeParam = getViewExecuteParam(null, widget.getConfig(), simpleViews, null);
                    }

                    PaginateWithQueryColumns paginate = viewService.getResultDataList(maintainer,
                            viewWithProjectAndSource, executeParam, user);

                    sheet = wb.createSheet(sheetName);
                    ExcelUtils.writeSheet(sheet, paginate.getColumns(), paginate.getResultList(), wb, containType,
                            widget.getConfig(), executeParam.getParams());
                } catch (Exception e) {
                    log.error(e.toString(), e);
                } finally {
                    sheet = null;
                    countDownLatch.countDown();
                }
            });

            i++;
        }

        countDownLatch.await();
        executorService.shutdown();

        File file = new File(filePath);
        File dir = new File(file.getParent());
        if (!dir.exists()) {
            dir.mkdirs();
        }

        FileOutputStream out = new FileOutputStream(filePath);
        try {
            wb.write(out);
            out.flush();
        } catch (Exception e) {
            // ignore
        } finally {
            FileUtils.closeCloseable(out);
        }
        return file;
    }

    @Override
    public String showSql(Long id, ViewExecuteParam executeParam, User user) {
        Widget widget = widgetMapper.getById(id);

        if (null == widget) {
            log.info("Widget({}) not found", id);
            throw new NotFoundException("Widget is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(widget.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
        if (projectPermission.getWidgetPermission() < UserPermissionEnum.WRITE.getPermission()) {
            throw new UnAuthorizedException();
        }

        boolean isMaintainer = projectService.isMaintainer(projectDetail, user);

        ViewWithProjectAndSource viewWithSource = viewMapper.getViewWithProjectAndSourceByWidgetId(id);

        if (null == viewWithSource.getSource()) {
            throw new NotFoundException("Source is not found");
        }

        if (null == executeParam || (CollectionUtils.isEmpty(executeParam.getGroups()) && CollectionUtils.isEmpty(executeParam.getAggregators()))) {
            throw new ServerException("Illegal execute parameters");
        }

        if (StringUtils.isEmpty(viewWithSource.getSql())) {
            return "";
        }

        List<SqlVariable> variables = viewWithSource.getVariables();
        SqlEntity sqlEntity = sqlParseUtils.parseSql(viewWithSource.getSql(), variables, sqlTempDelimiter, user, isMaintainer);
        Set<String> excludeColumns = new HashSet<>();
        viewService.packageParams(isMaintainer, viewWithSource.getId(), sqlEntity, variables, executeParam.getParams(), excludeColumns, user);

        String srcSql = sqlParseUtils.replaceParams(sqlEntity.getSql(), sqlEntity.getQueryParams(), sqlEntity.getAuthParams(), sqlTempDelimiter);

        StringBuilder res = new StringBuilder();
        List<String> executeSqlList = sqlParseUtils.getSqls(srcSql, false);
        executeSqlList.forEach(s -> res.append(s).append(Consts.NEW_LINE_CHAR));
        List<String> querySqlList = sqlParseUtils.getSqls(srcSql, true);
        querySqlList.forEach(s -> res.append(s).append(Consts.NEW_LINE_CHAR));

        return res.toString();
    }
}
