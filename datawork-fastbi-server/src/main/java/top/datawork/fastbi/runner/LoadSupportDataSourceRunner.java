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

package top.datawork.fastbi.runner;

import top.datawork.core.consts.Consts;
import top.datawork.core.enums.DataTypeEnum;
import top.datawork.core.utils.CustomDataSourceUtils;
import top.datawork.fastbi.dto.sourceDto.DatasourceType;
import lombok.Getter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Order(3)
@Component
public class LoadSupportDataSourceRunner implements ApplicationRunner {

    @Getter
    private static final List<DatasourceType> supportDatasourceList = new ArrayList<>();

    @Getter
    private static final Map<String, String> supportDatasourceMap = new HashMap<>();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String, List<String>> dataSourceVersions = CustomDataSourceUtils.getDataSourceVersion();

        for (DataTypeEnum dataTypeEnum : DataTypeEnum.values()) {
            if (dataSourceVersions.containsKey(dataTypeEnum.getFeature())) {
                List<String> versions = dataSourceVersions.get(dataTypeEnum.getFeature());
                if (!versions.isEmpty() && !versions.contains(Consts.JDBC_DATASOURCE_DEFAULT_VERSION)) {
                    versions.add(0, Consts.JDBC_DATASOURCE_DEFAULT_VERSION);
                }
            } else {
                dataSourceVersions.put(dataTypeEnum.getFeature(), null);
            }
        }

        dataSourceVersions.forEach((name, versions) -> supportDatasourceList.add(new DatasourceType(name, versions)));

        supportDatasourceList.forEach(s -> supportDatasourceMap.put(
                s.getName(),
                s.getName().equalsIgnoreCase(DataTypeEnum.ORACLE.getFeature()) ? Consts.ORACLE_JDBC_PREFIX : String.format(Consts.JDBC_PREFIX_FORMATTER, s.getName())
        ));

        supportDatasourceList.sort(Comparator.comparing(DatasourceType::getName));
    }
}
