package com.cyq.app.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.app.entity.AppUserWallet;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppUserService;
import com.cyq.app.service.AppUserWalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@Validated
@RestController
@RequestMapping("/user")
public class AppUserAccountController extends ApiController {

    @Autowired
    AppUserService appUserService;
    @Autowired
    AppUserWalletService appUserWalletService;

    /**
     * 获取用户余额
     *
     * @return
     */
    @GetMapping("/getBalance")
    public R<BigDecimal> getBalance() {
        Integer userId = RequestContext.getUserId();
        AppUserWallet userWallet = appUserWalletService.getOne(Wrappers.<AppUserWallet>lambdaQuery().eq(AppUserWallet::getUserId, userId));
        return success(userWallet.getBalance());
    }


}
