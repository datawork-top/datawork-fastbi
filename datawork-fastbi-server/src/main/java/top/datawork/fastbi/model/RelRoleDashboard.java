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

package top.datawork.fastbi.model;

import top.datawork.core.model.RecordInfo;
import lombok.Data;

@Data
public class RelRoleDashboard extends RecordInfo<RelRoleDashboard> {
    private Long id;

    private Long roleId;

    private Long dashboardId;

    private Boolean visible = false; // 可见/不可见  true/false

    public RelRoleDashboard(Long dashboardId, Long roleId) {
        this.dashboardId = dashboardId;
        this.roleId = roleId;
    }

    public RelRoleDashboard() {
    }
}