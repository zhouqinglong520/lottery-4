package com.cyq.app.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.app.entity.AppUserWalletLog;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppUserWalletLogService;
import com.cyq.common.dto.PageDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户账户 前端控制器
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/wallet")
public class AppUserWalletController extends ApiController {

    @Autowired
    AppUserWalletLogService appUserWalletLogService;

    /**
     * 查询帐户明细
     *
     * @param type 0全部，1收入，2支出
     * @return
     */
    @GetMapping("/detailList")
    public R<List<Map<String, Object>>> list(
            @RequestParam("type") int type,
            PageDTO pageDTO) {
        Integer userId = RequestContext.getUserId();
        List<Map<String, Object>> list=new ArrayList<>();
        if(!userId.equals(2916)){
            list=appUserWalletLogService.detailList(userId, type, pageDTO);
        }
        return success(list);
    }

    @GetMapping("/detailUrl")
    public R<String> detailUrl(
            @RequestParam("orderId") String orderId) {
        Integer userId = RequestContext.getUserId();
        AppUserWalletLog userWalletLog = appUserWalletLogService.getOne(Wrappers.<AppUserWalletLog>lambdaQuery()
                .eq(AppUserWalletLog::getOrderId, orderId)
                .eq(AppUserWalletLog::getUserId, userId));
        if (StringUtils.isEmpty(userWalletLog.getUrl())) {
            return success("");
        }
        return success(userWalletLog.getUrl());
    }

}
