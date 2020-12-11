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

package top.datawork.fastbi.service.excel;

import top.datawork.fastbi.dto.viewDto.ViewExecuteParam;
import top.datawork.fastbi.model.Dashboard;
import top.datawork.fastbi.model.MemDashboardWidget;
import top.datawork.fastbi.model.Widget;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author daemon
 * @Date 19/5/29 11:46
 * To change this template use File | Settings | File Templates.
 */
@Data
public class WidgetContext implements Serializable {

    private Widget widget;

    private Dashboard dashboard;

    private MemDashboardWidget memDashboardWidget;

    private Boolean isMaintainer;

    private ViewExecuteParam executeParam;

    public WidgetContext(Widget widget, Dashboard dashboard, MemDashboardWidget memDashboardWidget, ViewExecuteParam executeParam) {
        this.widget = widget;
        this.dashboard = dashboard;
        this.memDashboardWidget = memDashboardWidget;
        this.executeParam = executeParam;
    }

    public WidgetContext(Widget widget, boolean isMaintainer, ViewExecuteParam executeParam) {
        this.widget = widget;
        this.isMaintainer = isMaintainer;
        this.executeParam = executeParam;
    }

    public WidgetContext() {

    }

    public boolean hasExecuteParam() {
        return this.executeParam != null;
    }
}
