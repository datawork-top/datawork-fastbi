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

package top.datawork.fastbi.controller;

import top.datawork.core.annotation.AuthShare;
import top.datawork.core.annotation.CurrentUser;
import top.datawork.core.model.Paginate;
import top.datawork.fastbi.common.controller.BaseController;
import top.datawork.fastbi.core.common.Constants;
import top.datawork.fastbi.core.common.ResultMap;
import top.datawork.fastbi.dto.shareDto.ShareDashboard;
import top.datawork.fastbi.dto.shareDto.ShareDisplay;
import top.datawork.fastbi.dto.shareDto.ShareWidget;
import top.datawork.fastbi.dto.userDto.UserLogin;
import top.datawork.fastbi.dto.userDto.UserLoginResult;
import top.datawork.fastbi.dto.viewDto.DistinctParam;
import top.datawork.fastbi.dto.viewDto.ViewExecuteParam;
import top.datawork.fastbi.model.User;
import top.datawork.fastbi.service.ShareService;
import top.datawork.fastbi.service.share.ShareOperation;
import top.datawork.fastbi.service.share.ShareType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;


@Api(value = "/share", tags = "share", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "resource not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/share", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ShareController extends BaseController {


    @Autowired
    private ShareService shareService;

    @ApiOperation(value = "share preflight")
    @AuthShare(type = ShareType.LOGIN)
    @GetMapping(value = "/preflight/{token}")
    public ResponseEntity preFlight(@PathVariable String token) {
        Map<String, Object> result = shareService.checkShareToken();
        return ResponseEntity.ok(new ResultMap().success().payload(result));
    }

    @ApiOperation(value = "get share permission")
    @AuthShare(type = ShareType.PERMISSION, operation = ShareOperation.PERMISSION)
    @GetMapping(value = "/permissions/{token}")
    public ResponseEntity permission(@PathVariable(name = "token") String token,
                                     @RequestParam(required = false) String password,
                                     @ApiIgnore @CurrentUser User user,
                                     HttpServletRequest request) {

        Map<String, Object> result = shareService.getSharePermissions();
        if (null == user || user.getId() == null) {
            return ResponseEntity.ok(new ResultMap().success().payload(result));
        } else {
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(result));
        }
    }

    /**
     * share页登录
     *
     * @param token
     * @param userLogin
     * @param bindingResult
     * @return
     */
    @ApiOperation(value = "share login")
    @AuthShare(type = ShareType.LOGIN)
    @PostMapping("/login/{token}")
    public ResponseEntity shareLogin(@PathVariable String token,
                                     @Valid @RequestBody UserLogin userLogin,
                                     @ApiIgnore BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap().fail().message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        User user = shareService.shareLogin(userLogin);
        return ResponseEntity.ok(new ResultMap().success(tokenUtils.generateToken(user)).payload(new UserLoginResult(user)));
    }

    /**
     * share页获取dashboard信息
     *
     * @param token
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get share dashboard")
    @AuthShare(type = ShareType.DASHBOARD, operation = ShareOperation.READ)
    @GetMapping("/dashboard/{token}")
    public ResponseEntity getShareDashboard(@PathVariable String token,
                                            @RequestParam(required = false) String password,
                                            @ApiIgnore @CurrentUser User user,
                                            HttpServletRequest request) {

        ShareDashboard shareDashboard = shareService.getShareDashboard(user);
        if (null == user || user.getId() == null) {
            return ResponseEntity.ok(new ResultMap().success().payload(shareDashboard));
        } else {
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(shareDashboard));
        }
    }

    /**
     * share页获取display信息
     *
     * @param token
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get share display")
    @AuthShare(type = ShareType.DISPLAY, operation = ShareOperation.READ)
    @GetMapping("/display/{token}")
    public ResponseEntity getShareDisplay(@PathVariable String token,
                                          @RequestParam(required = false) String password,
                                          @ApiIgnore @CurrentUser User user,
                                          HttpServletRequest request) {

        ShareDisplay shareDisplay = shareService.getShareDisplay(user);
        if (null == user || user.getId() == null) {
            return ResponseEntity.ok(new ResultMap().success().payload(shareDisplay));
        } else {
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(shareDisplay));
        }
    }

    /**
     * share页获取widget信息
     *
     * @param token
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get share widget")
    @AuthShare(type = ShareType.WIDGET, operation = ShareOperation.READ)
    @GetMapping("/widget/{token}")
    public ResponseEntity getShareWidget(@PathVariable String token,
                                         @RequestParam(required = false) String password,
                                         @ApiIgnore @CurrentUser User user,
                                         HttpServletRequest request) {

        ShareWidget shareWidget = shareService.getShareWidget(user);
        if (null == user || user.getId() == null) {
            return ResponseEntity.ok(new ResultMap().success().payload(shareWidget));
        } else {
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(shareWidget));
        }
    }

    /**
     * share页获取源数据
     *
     * @param token
     * @param executeParam
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get share data")
    @AuthShare(type = ShareType.DATA, operation = ShareOperation.LOAD_DATA)
    @PostMapping(value = "/data/{token}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getShareData(@PathVariable String token,
                                       @RequestParam(required = false) String password,
                                       @RequestBody(required = false) ViewExecuteParam executeParam,
                                       @ApiIgnore @CurrentUser User user,
                                       HttpServletRequest request) throws SQLException {

        Paginate<Map<String, Object>> shareData = shareService.getShareData(executeParam, user);
        if (null == user || user.getId() == null) {
            return ResponseEntity.ok(new ResultMap().success().payload(shareData));
        } else {
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(shareData));
        }
    }


    /**
     * share获取控制器的值
     *
     * @param token
     * @param param
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get share data")
    @AuthShare(type = ShareType.DATA, operation = ShareOperation.LOAD_DISTINCT_DATA)
    @PostMapping(value = "/data/{token}/distinctvalue", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getDistinctValue(@PathVariable("token") String token,
                                           @Valid @RequestBody DistinctParam param,
                                           @ApiIgnore BindingResult bindingResult,
                                           @ApiIgnore @CurrentUser User user,
                                           HttpServletRequest request) {

        List<Map<String, Object>> resultList = shareService.getDistinctValue(param, user);
        if (null == user || user.getId() == null) {
            return ResponseEntity.ok(new ResultMap().success().payloads(resultList));
        } else {
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(resultList));
        }
    }
}
