package com.cyq.app.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.cyq.app.TIM.IMRemoteApi;
import com.cyq.app.TIM.IMResponseData;
import com.cyq.app.TIM.service.IMService;
import com.cyq.app.aliyun.AliyunOssService;
import com.cyq.app.entity.AppLocatedApply;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.AppLocatedApplyService;
import com.cyq.app.utils.FileProcessorHolder;
import com.cyq.common.dto.PageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * IM 消息服务接口
 *
 * @Created by Lee lu on 2018/8/13
 */
@Slf4j
@Validated
@RequestMapping("/im")
@RestController
public class IMController extends ApiController {

    @Autowired
    IMService imService;

    @Autowired
    IMRemoteApi imRemoteApi;

    @Autowired
    ExecutorService executorService;

    @Autowired
    AliyunOssService aliyunOssService;

    @Autowired
    Environment env;

    @Autowired
    AppLocatedApplyService appLocatedApplyService;

    /**
     * 获取销售推荐列表（带查询和分页）
     *
     * @return
     * @throws Exception
     */
    @GetMapping("/getRecommendList")
    public R<List<Map<String, Object>>> getRecommendList() throws Exception {
        return success(imService.getRecommendList());
    }

    /**
     * 获取销售员信息
     *
     * @param sellerId
     * @return
     */
    @GetMapping("/getSellerInfo")
    public R<Map<String, Object>> getSellerInfo(
            @NotNull @RequestParam("sellerId") Long sellerId) throws Exception {
        return success(imService.getSellerInfo(sellerId));
    }

    /**
     * 添加好友接口
     *
     * @param fromAccount   需要为该 Identifier 添加好友
     * @param toAccount     好友的 Identifier
     * @param forceAddFlags 管理员强制加好友标记：1 表示强制加好友；0 表示常规加好友方式。
     * @return
     */
    @PostMapping("friendAdd")
    public R<String> friendAdd(
            @RequestParam("fromAccount") String fromAccount,
            @RequestParam("toAccount") String toAccount,
            @RequestParam(name = "forceAddFlags", required = false, defaultValue = "1") Integer forceAddFlags) {
        IMResponseData responseData = imRemoteApi.friendAdd(fromAccount, toAccount, forceAddFlags);
        if (!responseData.isSuccess()) {
            throw new ApiException("加好友失败");
        }
        return success("ok");
    }




    /**
     * 根据条件查询销售列表
     * @param nickName
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getSellerList")
    public R<List<Map<String, Object>>> getSellerList(
            PageDTO pageDTO,
            @RequestParam(required = false) String nickName) throws Exception {
        return success(imService.getSellerList(pageDTO,nickName));
    }

    /**
     * 评价销售
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/evaluateSeller")
    public R<String> evaluateSeller(@RequestParam Long sellerId,
                                    @RequestParam Integer star) throws Exception {
        Integer userId = RequestContext.getUserId();
        imService.evaluateSeller(sellerId,star,userId);
        return success("ok");
    }


    /**
     * 评价列表
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getSellerEvaluateList")
    public R<List<Map<String, Object>>> getSellerEvaluateList(@RequestParam Integer pageNum,
                                                      @RequestParam Integer pageSize,
                                                      @RequestParam Long sellerId) throws Exception {
        return success(imService.getSellerEvaluateList(pageNum,pageSize,sellerId));
    }

    /**
     * 是否已评价过销售
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/ifEvaluateSeller")
    public R<String> ifEvaluateSeller(@RequestParam Long sellerId) throws Exception {
        Integer userId = RequestContext.getUserId();
        return success(imService.ifEvaluateSeller(sellerId,userId));
    }

    /**
     * 销售投诉
     *
     * @param sellerId 销售id
     * @param content 投诉内容
     * @param files   图片证据
     * @return
     */
    @PostMapping("/addSellerComplain")
    public R<String> addSellerComplain(
            @RequestParam("sellerId") Long sellerId,
            @RequestParam(value = "content",required = false) String content,
            @RequestParam("type") String type,
            MultipartFile[] files) throws Exception {
        StringJoiner joiner = new StringJoiner(",");
        log.info("订单投诉:{}，附件图片个数: {}", sellerId, files.length);
        CountDownLatch countDownLatch = new CountDownLatch(files.length);
        for (MultipartFile file : files) {
            log.info("文件名:{}，文件大小: {} KB", file.getOriginalFilename(), file.getSize() / 1024);
            executorService.submit(() -> {
                try {
                    String url = aliyunOssService.storeToAliyun(FileProcessorHolder.ORDER_COMPLAIN_UPLOAD_PATH + "/" + file.getOriginalFilename(), file.getInputStream());
                    joiner.add(url);
                    countDownLatch.countDown();
                } catch (IOException e) {
                    countDownLatch.countDown();
                    log.error(e.getMessage(), e);
                }
            });
        }
        countDownLatch.await(30, TimeUnit.SECONDS);
        Integer userId = RequestContext.getUserId();
        imService.saveSellerComplain(sellerId, content, joiner.toString(),userId,type);
        return success("ok");
    }

    /**
     * 判断客服是否在线
     *
     * @return
     */
    @GetMapping("/isOnline")
    public R<String> isOnline(){
        return success(imService.isOnline(env.getProperty("tencent.im.kefu"),String.valueOf(RequestContext.getUserId())));
    }


    /**
     * 店铺入驻
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/locatedIn")
    public R<String> locatedIn(@RequestParam String licenceImage,
                                @RequestParam String idcardImage,
                               @RequestParam String storeImage) throws Exception {
        Integer userId = RequestContext.getUserId();
        List<AppLocatedApply> appLocatedApplies = appLocatedApplyService.list(Wrappers.<AppLocatedApply>lambdaQuery()
        .eq(AppLocatedApply::getUserId,userId)
        .eq(AppLocatedApply::getStatus,1));
        if (appLocatedApplies==null || appLocatedApplies.size()==0){
            AppLocatedApply appLocatedApply = new AppLocatedApply();
            appLocatedApply.setUserId(userId);
            appLocatedApply.setLicenceImage(licenceImage);
            appLocatedApply.setIdcardImage(idcardImage);
            appLocatedApply.setStoreImage(storeImage);
            appLocatedApplyService.save(appLocatedApply);
            return success("ok");
        }else {
            throw new ApiException("您有一个申请正在审核,请勿重复提交");
        }


    }

}
