package com.cyq.app.controller;


import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.cyq.app.service.AppUserService;
import com.cyq.app.service.CommonService;
import com.cyq.common.utils.ValidatorUtils;
import com.cyq.common.web.exception.errorcode.ApiErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/h5")
public class H5Controller extends ApiController {
    @Value("${SMS.enable}")
    boolean smsEnable;
    @Autowired
    CommonService commonService;
    @Autowired
    AppUserService appUserService;

    @PostMapping("/quick/regist")
    public R<Map<String, Object>> quickRegist(
            @NotBlank @Pattern(regexp = ValidatorUtils.mobileValidExp, message = "手机号码格式错误") String mobile,
            @NotBlank @RequestParam("randCode") String randCode,
            @RequestParam(name = "inviteCode", required = false) String inviteCode) throws Exception {
        if (smsEnable) {// 验证随机码
            if (!commonService.verifySecurityCode(mobile, randCode)) {
                throw new ApiException(ApiErrorCode.VERIFY_CODE_NOT_MATCH);
            }
        }
        return success(appUserService.findOrCreate(mobile, inviteCode));
    }

    @PostMapping("/quick/randCode")
    public R<Boolean> randCode(
            @NotBlank @Pattern(regexp = ValidatorUtils.mobileValidExp, message = "手机号码格式错误") String mobile) throws Exception {
        if (smsEnable) {
            return success(commonService.sendSecurityCode(mobile));
        }
        return success(true);
    }

    @PostMapping("createPlanDetailQueue")
    public void createPlanDetail() {
        String s = "CREATE_PLAN_DETAIL_QUEUE";
    }

}