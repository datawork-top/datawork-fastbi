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

package top.datawork.fastbi.core.enums;

public enum CheckEntityEnum {
    USER("user", "userService", "top.datawork.fastbi.model.User"),
    PROJECT("project", "projectService", "top.datawork.fastbi.model.Project"),
    ORGANIZATION("organization", "organizationService", "top.datawork.fastbi.model.Organization"),
    SOURCE("source", "sourceService", "top.datawork.fastbi.model.Source"),
    VIEW("view", "viewService", "top.datawork.fastbi.model.View"),
    WIDGET("widget", "widgetService", "top.datawork.fastbi.model.Widget"),
    DISPLAY("display", "displayService", "top.datawork.fastbi.model.Display"),
    DISPLAYSLIDE("displaySlide", "displaySlideService", "top.datawork.fastbi.model.DisplaySlide"),
    DASHBOARD("dashboard", "dashboardService", "top.datawork.fastbi.model.Dashboard"),
    DASHBOARDPORTAL("dashboardPortal", "dashboardPortalService", "top.datawork.fastbi.model.DashboardPortal"),
    CRONJOB("cronJob", "cronJobService", "top.datawork.fastbi.model.CronJob");

    private String source;
    private String service;
    private String clazz;


    CheckEntityEnum(String source, String service, String clazz) {
        this.source = source;
        this.service = service;
        this.clazz = clazz;
    }

    public static CheckEntityEnum sourceOf(String source) {
        for (CheckEntityEnum sourceEnum : values()) {
            if (sourceEnum.source.equals(source)) {
                return sourceEnum;
            }
        }
        return null;
    }

    public String getService() {
        return service;
    }

    public String getClazz() {
        return clazz;
    }

    public String getSource() {
        return source;
    }
}
