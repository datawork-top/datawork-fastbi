/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2020 EDP
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
 */

package top.datawork.fastbi.core.enums;

import lombok.Getter;

public enum SystemVariableEnum {

    USER_ID("$FASTBI.USER.ID$", "\\$FASTBI.USER.ID\\$"),
    USER_NAME("$FASTBI.USER.NAME$", "\\$FASTBI.USER.NAME\\$"),
    USER_USERNAME("$FASTBI.USER.USERNAME$", "\\$FASTBI.USER.USERNAME\\$"),
    USER_EMAIL("$FASTBI.USER.EMAIL$", "\\$FASTBI.USER.EMAIL\\$"),
    USER_DEPARTMENT("$FASTBI.USER.DEPARTMENT$", "\\$FASTBI.USER.DEPARTMENT\\$");

    @Getter
    private String key;

    @Getter
    private String regex;

    SystemVariableEnum(String variable, String regex) {
        this.key = variable;
        this.regex = regex;
    }

    public static boolean isContains(String str) {
        str = str.toUpperCase();
        return str.contains(USER_ID.key) ||
                str.contains(USER_NAME.key) ||
                str.contains(USER_USERNAME.key) ||
                str.contains(USER_EMAIL.key) ||
                str.contains(USER_DEPARTMENT.key);
    }
}
