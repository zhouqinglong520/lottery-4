package com.cyq.app.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.cyq.app.constant.PayChannel;
import com.cyq.app.entity.AppUserQrcode;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppUserQrcodeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * <p>
 * 金融账户设置 前端控制器
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/qrcode")
public class AppUserQrcodeController extends ApiController {

    @Autowired
    AppUserQrcodeService appUserQrcodeService;
    @Autowired
    Environment env;

    /**
     * 查询所有收款设置
     *
     * @return
     * @throws Exception
     */
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() throws Exception {
        List<Map<String, Object>> resultList = new ArrayList<>();
        List<AppUserQrcode> qrcodeList = appUserQrcodeService.list(Wrappers.<AppUserQrcode>lambdaQuery().eq(AppUserQrcode::getUserId, RequestContext.getUserId()));
        for (AppUserQrcode appUserQrcode : qrcodeList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", appUserQrcode.getId());
            map.put("type", appUserQrcode.getType());
            map.put("realName", appUserQrcode.getRealName());
            map.put("account", appUserQrcode.getAccount());
            map.put("qrCode", appUserQrcode.getQrCode());
            map.put("isDefault", appUserQrcode.getIsDefault());
            if (PayChannel.WEIXIN.getValue() == appUserQrcode.getType()) {
                map.put("logo", env.getProperty("logo.wxpay"));
            } else if (PayChannel.ALIPAY.getValue() == appUserQrcode.getType()) {
                map.put("logo", env.getProperty("logo.alipay"));
            } else {
                map.put("logo", env.getProperty("logo.bank"));
                map.put("bankName", appUserQrcode.getBankName());
            }
            resultList.add(map);
        }
        return success(resultList);
    }

    /**
     * 删除指定收款设置
     *
     * @param id
     * @return
     * @throws Exception
     */
    @PostMapping("/delete")
    public R<Boolean> delete(Integer id) throws Exception {
        return success(appUserQrcodeService.remove(Wrappers.<AppUserQrcode>lambdaQuery()
                .eq(AppUserQrcode::getId, id)
                .eq(AppUserQrcode::getUserId, RequestContext.getUserId())));
    }

    /**
     * 设置默认的收款配置
     *
     * @param id
     * @return
     * @throws Exception
     */
    @PostMapping("/makeDefault")
    public R<Boolean> makeDefault(@NotNull Integer id) throws Exception {
        return success(appUserQrcodeService.makeDefault(id, RequestContext.getUserId()));
    }

    /**
     * @param type           银行卡：1，微信：2，支付宝：3
     * @param realName       真实姓名
     * @param account        账号信息
     * @param qrCode         二维码地址
     * @param bankName       银行名称
     * @return
     * @throws Exception
     */
    @PostMapping("/add")
    public R<Boolean> assignFinance(
            @NotNull @Range(min = 1, max = 3) Integer type,
            @NotBlank @RequestParam("realName") String realName,
            @NotBlank @RequestParam("account") String account,
            @RequestParam(name = "qrCode", required = false) String qrCode,
            @RequestParam(name = "bankName", required = false) String bankName) throws Exception {
        Integer userId = RequestContext.getUserId();
        AppUserQrcode userQrcode = appUserQrcodeService.getOne(Wrappers.<AppUserQrcode>lambdaQuery()
                .eq(AppUserQrcode::getUserId, userId)
                .eq(AppUserQrcode::getType, type));
        if (Objects.isNull(userQrcode)) {
            userQrcode = new AppUserQrcode();
        }
        if (type == 2 || type == 3) {// 校验微信参数
            if (StringUtils.isEmpty(realName) || StringUtils.isEmpty(account) || StringUtils.isEmpty(qrCode)) {
                throw new ApiException("参数不全，需同时传递 realName, account, qrCode !");
            }
        } else if (type == 1) {
            if (StringUtils.isEmpty(bankName)) {
                throw new ApiException("参数不全，需同时传递 bankName !");
            }
            userQrcode.setBankName(bankName);
        }
        userQrcode.setUserId(userId);
        userQrcode.setAccount(account);
        userQrcode.setType(type);
        userQrcode.setRealName(realName);
        userQrcode.setQrCode(qrCode);
        // 收款方式已存在则覆盖更新，否则新增一条
        return success(appUserQrcodeService.saveOrUpdate(userQrcode));
    }
}
