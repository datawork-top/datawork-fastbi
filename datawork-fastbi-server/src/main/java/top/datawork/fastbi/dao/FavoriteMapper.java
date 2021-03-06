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

package top.datawork.fastbi.dao;

import top.datawork.fastbi.model.Favorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface FavoriteMapper {

    int insert(Favorite favorite);

    @Delete({"delete from fastbi_favorite where id = #{id,jdbcType=BIGINT}"})
    int deleteById(Long id);

    @Delete({"delete from fastbi_favorite where id = #{id,jdbcType=BIGINT}"})
    int delete(@Param("userId") Long userId, @Param("projectId") Long projectId);

    @Select({
            "select",
            "id, user_id, project_id, create_time",
            "from fastbi_favorite",
            "where id = #{id,jdbcType=BIGINT}"
    })
    Favorite selectById(Long id);


    int deleteBatch(@Param("list") List<Long> list, @Param("userId") Long userId);
}