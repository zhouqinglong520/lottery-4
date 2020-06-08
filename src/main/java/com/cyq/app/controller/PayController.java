package com.cyq.app.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.cyq.app.constant.AppConsts;
import com.cyq.app.entity.AppDataDictionary;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppDataDictionaryService;
import com.cyq.app.service.PayService;
import com.cyq.common.web.exception.errorcode.ApiErrorCode;
import com.cyq.lottery.utils.OKHttpUtil;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/pay")
public class PayController extends ApiController {

    @Autowired
    PayService payService;
    @Autowired
    Environment env;
    @Autowired
    AppDataDictionaryService appDataDictionaryService;

    /**
     * 订单支付
     *
     * @param orderId
     * @return
     * @throws Exception
     */
    @PostMapping("/doPay")
    public R<String> doPay(
            @NotBlank @RequestParam("orderId") String orderId) throws Exception {
        Integer userId = RequestContext.getUserId();
        return success(payService.doPay(userId, orderId));
    }

    /**
     * 福袋订单支付
     *
     * @param orderId
     * @return
     * @throws Exception
     */
    @PostMapping("/doLuckyPay")
    public R<String> doLuckyPay(
            @NotBlank @RequestParam("orderId") String orderId) throws Exception {
        Integer userId = RequestContext.getUserId();
        return success(payService.doLuckyPay(userId, orderId));
    }

    /**
     * 用户充值限制
     * @return
     */
    @GetMapping("/recharge/limit")
    public R<Map<String,Object>> rechargeLimit(){
        List<Integer> ids=new ArrayList<>();
        //id固定，admin项目设置的
        ids.add(12);
        ids.add(13);
        ids.add(14);
        ids.add(15);
        ids.add(16);
        ids.add(17);
        Map<String,Object> map=new HashMap<>();
        List<AppDataDictionary> list=appDataDictionaryService.list(Wrappers.<AppDataDictionary>lambdaQuery().in(AppDataDictionary::getId,ids));
        list.stream().forEach(item->{
            String[] value=item.getDataValue().split(",");
            if(item.getId().equals(12)){
                map.put("wechatMin",Integer.valueOf(value[0]));
                map.put("wechatMax",Integer.valueOf(value[1]));
            }
            if(item.getId().equals(13)){
                map.put("alipayMin",Integer.valueOf(value[0]));
                map.put("alipayMax",Integer.valueOf(value[1]));
            }
            if(item.getId().equals(14)){
                map.put("unionPayMin",Integer.valueOf(value[0]));
                map.put("unionPayMax",Integer.valueOf(value[1]));
            }
            if(item.getId().equals(15)){
                map.put("cloudPayMin",Integer.valueOf(value[0]));
                map.put("cloudPayMax",Integer.valueOf(value[1]));
            }
            if(item.getId().equals(16)){
                map.put("alipayToUnionPayMin",Integer.valueOf(value[0]));
                map.put("alipayToUnionPayMax",Integer.valueOf(value[1]));
            }
            if(item.getId().equals(17)){
                map.put("alipayToAlipayMin",Integer.valueOf(value[0]));
                map.put("alipayToAlipayMax",Integer.valueOf(value[1]));
            }
        });
        boolean recharge=payService.customerServiceRole(RequestContext.getUserId());
        map.put("recharge",recharge);
        return success(map);
    }

    /**
     * 用户充值
     *
     * @param payChannel 1 银行卡  2是微信  3是支付宝 4是云闪付 5是支付宝转银行卡 6是支付宝转支付宝
     * @param fee        交费金额  单位 元
     */
    @RequestMapping("/recharge")
    public R<String> recharge(
            @NotNull @Range(min = 1, max = 6) Integer payChannel,
            @NotNull @RequestParam("fee") Integer fee) throws Exception {
        if (!Boolean.valueOf(env.getProperty("supportRecharge"))) {
            throw new ApiException("不支持在线充值");
        }
        List<Integer> ids=new ArrayList<>();
        //id固定，admin项目设置的
        ids.add(12);
        ids.add(13);
        ids.add(14);
        ids.add(15);
        ids.add(16);
        ids.add(17);
        List<AppDataDictionary> list=appDataDictionaryService.list(Wrappers.<AppDataDictionary>lambdaQuery().in(AppDataDictionary::getId,ids));
        list.stream().forEach(item->{
            String[] value=item.getDataValue().split(",");
            if(payChannel.equals(1)&&item.getId().equals(14)){
                if(fee<Integer.valueOf(value[0])||fee>Integer.valueOf(value[1])){
                    throw new ApiException("银行卡充值范围为："+value[0]+"到"+value[1]);
                }
            }
            if(payChannel.equals(2)&&item.getId().equals(12)){
                if(fee<Integer.valueOf(value[0])||fee>Integer.valueOf(value[1])){
                    throw new ApiException("微信充值范围为："+value[0]+"到"+value[1]);
                }
            }
            if(payChannel.equals(3)&&item.getId().equals(13)){
                if(fee<Integer.valueOf(value[0])||fee>Integer.valueOf(value[1])){
                    throw new ApiException("支付宝充值范围为："+value[0]+"到"+value[1]);
                }
            }
            if(payChannel.equals(4)&&item.getId().equals(15)){
                if(fee<Integer.valueOf(value[0])||fee>Integer.valueOf(value[1])){
                    throw new ApiException("云闪付充值范围为："+value[0]+"到"+value[1]);
                }
            }
            if(payChannel.equals(5)&&item.getId().equals(16)){
                if(fee<Integer.valueOf(value[0])||fee>Integer.valueOf(value[1])){
                    throw new ApiException("支付宝转银行卡充值范围为："+value[0]+"到"+value[1]);
                }
            }
            if(payChannel.equals(6)&&item.getId().equals(17)){
                if(fee<Integer.valueOf(value[0])||fee>Integer.valueOf(value[1])){
                    throw new ApiException("支付宝转支付宝充值范围为："+value[0]+"到"+value[1]);
                }
            }
        });

//        if (!LocalDateTimeUtils.isAllowedRecharge(LocalDateTime.now())) {
//            throw new ApiException("周二至周六00:15-09:00以及周天与周一的01:15-09:00时间段不能充值");
//        }
        Integer userId = RequestContext.getUserId();
        return success(payService.recharge(payChannel, fee, userId));
    }

    /**
     * 用户提现
     *
     * @param fee 交费金额  单位 元
     * @return
     * @throws Exception
     */
    @RequestMapping("/withdraw")
    public R<String> withdraw(@NotNull Integer fee) throws Exception {
        AppDataDictionary appDataDictionary= appDataDictionaryService.getById(2);
        String[] value=appDataDictionary.getDataValue().split(",");
        if (fee < Integer.valueOf(value[0])) {// 提现金额必须大于等于value[0]
            throw new ApiException(String.format("提现金额必须大于等于%d",Integer.valueOf(value[0])));
        }
        if (fee > Integer.valueOf(value[1])) {// 提现金额必须小于等于value[1]
            throw new ApiException(String.format("提现金额必须小于等于%d",Integer.valueOf(value[1])));
        }
        Integer userId = RequestContext.getUserId();
        return success(payService.withdraw(fee, userId));
    }

    /**
     * TTM 回调
     *
     * @return
     */
    @RequestMapping("/TTM/callback")
    public String ttmCallback(@RequestBody String body) throws Exception {
        return payService.ttmCallback(body);
    }

}
