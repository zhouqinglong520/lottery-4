package com.cyq.app.controller;


import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppThirdLoginService;
import com.cyq.app.service.CommonService;
import com.cyq.common.utils.ValidatorUtils;
import com.cyq.common.web.exception.errorcode.ApiErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Map;

/**
 * 第三方登录控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user")
public class ThirdLoginController extends ApiController {
    @Value("${SMS.enable}")
    boolean smsEnable;
    @Autowired
    CommonService commonService;
    @Autowired
    AppThirdLoginService appThirdLoginService;


    /**
     * 第三方登陆接口
     *
     * @param thirdType @link RegisterType
     * @param thirdKey
     * @param nickName
     * @param avatar    头像地址
     * @return
     * @throws Exception
     */
    @PostMapping("/thirdLogin")
    @Transactional
    public R<Map<String, Object>> thirdLogin(
            @NotNull @Range(min = 1, max = 3) Integer thirdType,
            @NotBlank String thirdKey,
            String nickName,
            String avatar) throws Exception {
        return success(appThirdLoginService.thirdLogin(thirdKey, thirdType, nickName, avatar));
    }

    /**
     * 第三方登陆绑定手机号码
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/bindMobile")
    @Transactional
    public R<Map<String, Object>> bindMobile(
            @NotNull @Range(min = 1, max = 3) Integer thirdType,
            @NotBlank String thirdKey,
            @NotBlank @Pattern(regexp = ValidatorUtils.mobileValidExp, message = "手机号码格式错误") String mobile,
            @NotBlank @RequestParam("randCode") String randCode) throws Exception {
        if (smsEnable) {// 验证随机码
            if (!commonService.verifySecurityCode(mobile, randCode)) {
                throw new ApiException(ApiErrorCode.VERIFY_CODE_NOT_MATCH);
            }
        }
        return success(appThirdLoginService.bindMobile(RequestContext.getClientId(), thirdKey, thirdType, mobile, RequestContext.getUserId()));
    }

}