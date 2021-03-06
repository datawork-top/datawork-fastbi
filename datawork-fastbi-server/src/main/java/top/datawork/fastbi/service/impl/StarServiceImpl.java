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
import top.datawork.core.utils.TokenUtils;
import top.datawork.fastbi.core.common.Constants;
import top.datawork.fastbi.core.common.ResultMap;
import top.datawork.fastbi.dao.ProjectMapper;
import top.datawork.fastbi.dao.StarMapper;
import top.datawork.fastbi.dto.projectDto.ProjectWithCreateBy;
import top.datawork.fastbi.dto.starDto.StarUser;
import top.datawork.fastbi.model.Project;
import top.datawork.fastbi.model.Star;
import top.datawork.fastbi.model.User;
import top.datawork.fastbi.service.StarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Service
public class StarServiceImpl implements StarService {

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private StarMapper starMapper;

    @Override
    @Transactional
    public ResultMap starAndUnstar(String target, Long targetId, User user, HttpServletRequest request) {
        ResultMap resultMap = new ResultMap(tokenUtils);

        if (!StringUtils.isEmpty(target)) {
            if (Constants.STAR_TARGET_PROJECT.equals(target)) {
                Project project = projectMapper.getById(targetId);
                String id = project.getId().toString().intern();
                if (null == project) {
                    return resultMap.failAndRefreshToken(request).message("project not found");
                }

                Star star = starMapper.select(user.getId(), targetId, target);
                if (null == star) {
                    //star
                    star = new Star(target, targetId, user.getId());
                    int i = starMapper.insert(star);
                    if (i > 0) {
                        synchronized (id) {
                            projectMapper.starNumAdd(project.getId());
                        }
                        return resultMap.successAndRefreshToken(request);
                    }
                } else {
                    //unstar
                    int i = starMapper.deleteById(star.getId());
                    if (i > 0) {
                        synchronized (id) {
                            projectMapper.starNumReduce(project.getId());
                        }
                    }
                    return resultMap.successAndRefreshToken(request);
                }
            }
        }

        return resultMap.failAndRefreshToken(request);
    }

    @Override
    public ResultMap getStarListByUser(String target, User user, HttpServletRequest request) {
        ResultMap resultMap = new ResultMap(tokenUtils);

        if (!StringUtils.isEmpty(target)) {
            if (Constants.STAR_TARGET_PROJECT.equals(target)) {
                List<ProjectWithCreateBy> starProjectList = starMapper.getStarProjectListByUser(user.getId(), target);
                return resultMap.successAndRefreshToken(request).payloads(starProjectList);
            }
        }
        return resultMap.failAndRefreshToken(request);
    }

    @Override
    public ResultMap getStarUserListByTarget(String target, Long targetId, HttpServletRequest request) {
        ResultMap resultMap = new ResultMap(tokenUtils);
        if (!StringUtils.isEmpty(target)) {
            if (Constants.STAR_TARGET_PROJECT.equals(target)) {
                Project project = projectMapper.getById(targetId);
                if (null == project) {
                    return resultMap.failAndRefreshToken(request).message("project not found");
                }

                List<StarUser> starUserList = starMapper.getStarUserListByTarget(targetId, target);
                return resultMap.successAndRefreshToken(request).payloads(starUserList);
            }
        }

        return resultMap.failAndRefreshToken(request);
    }
}
