/*
 * <<
 *  Davinci
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

package top.datawork.fastbi.core.inteceptor;

import top.datawork.core.annotation.CurrentPlatform;
import top.datawork.core.consts.Consts;
import top.datawork.core.inteceptor.CurrentPlatformMethodArgumentResolverInterface;
import top.datawork.fastbi.model.Platform;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @CurrentPlatform 注解 解析器
 */
public class CurrentPlatformMethodArgumentResolver implements CurrentPlatformMethodArgumentResolverInterface {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().isAssignableFrom(Platform.class)
                && parameter.hasParameterAnnotation(CurrentPlatform.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return  (Platform) webRequest.getAttribute(Consts.CURRENT_PLATFORM, RequestAttributes.SCOPE_REQUEST);
    }
}