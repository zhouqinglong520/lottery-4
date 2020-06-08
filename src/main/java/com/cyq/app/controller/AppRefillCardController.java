package com.cyq.app.controller;

import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppRefillCardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 充值卡
 */
@Slf4j
@Validated
@RequestMapping("/refillCard")
@RestController
public class AppRefillCardController extends ApiController {

    @Autowired
    private AppRefillCardService appRefillCardService;

    /**
     * 充值卡使用
     *
     * @return
     */
    @GetMapping("/use")
    public R<String> useRefillCard(@RequestParam("cardNumber") String cardNumber) {
        return success(appRefillCardService.useRefillCard(cardNumber, RequestContext.getUserId()));
    }


}
