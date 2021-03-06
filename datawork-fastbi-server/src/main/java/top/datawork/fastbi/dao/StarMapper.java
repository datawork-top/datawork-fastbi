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

import top.datawork.fastbi.dto.projectDto.ProjectWithCreateBy;
import top.datawork.fastbi.dto.starDto.StarUser;
import top.datawork.fastbi.model.Star;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface StarMapper {

    int insert(Star star);

    @Delete({
            "delete from fastbi_star",
            "where id = #{id,jdbcType=BIGINT}"
    })
    int deleteById(Long id);

    @Delete({
            "delete from fastbi_star",
            "where target = #{target} and  target_id = #{targetId} and user_id = #{userId}"
    })
    int delete(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("target") String target);


    @Select({
            "select * from fastbi_star",
            "where target = #{target} and target_id = #{targetId} and user_id = #{userId}"
    })
    Star select(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("target") String target);


    @Select({
            "select p.*, u.id as 'createBy.id', IF(u.`name` is NULL,u.username,u.`name`) as 'createBy.username'," +
                    " u.avatar as 'createBy.avatar'  " +
                    " from fastbi_project p " +
                    "left join user u on u.id = p.user_id ",
            "where p.id in (select target_id from fastbi_star where target = #{target} and user_id = #{userId})"
    })
    List<ProjectWithCreateBy> getStarProjectListByUser(@Param("userId") Long userId, @Param("target") String target);


    @Select({
            "select u.id, IF(u.`name` is NULL,u.username,u.`name`) as username, u.email, u.avatar, " +
                    "s.star_time from fastbi_star s " +
                    "left join user u on u.id = s.user_id",
            "where s.target = #{target} and s.target_id = #{targetId}"
    })
    List<StarUser> getStarUserListByTarget(@Param("targetId") Long targetId, @Param("target") String target);
}