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

public enum FieldFormatTypeEnum {
    Default("default"),
    Numeric("numeric"),
    Currency("currency"),
    Percentage("percentage"),
    ScientificNotation("scientificNotation"),
    Date("date"),
    Custom("custom");


    private String type;


    public static FieldFormatTypeEnum typeOf(String type) {
        for (FieldFormatTypeEnum formatTypeEnum : FieldFormatTypeEnum.values()) {
            if (type.equals(formatTypeEnum.type)) {
                return formatTypeEnum;
            }
        }
        return null;
    }

    public String getType() {
        return type;
    }


    FieldFormatTypeEnum(String type) {
        this.type = type;
    }
}
