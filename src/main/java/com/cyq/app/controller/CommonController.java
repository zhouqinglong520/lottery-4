package com.cyq.app.controller;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.cyq.app.constant.AppConsts;
import com.cyq.app.constant.AppLogType;
import com.cyq.app.entity.*;
import com.cyq.app.helper.RequestContext;
import com.cyq.app.service.*;
import com.cyq.app.utils.AMapUtil;
import com.cyq.app.utils.FileProcessorHolder;
import com.cyq.common.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/common")
public class CommonController extends ApiController {
    @Autowired
    AppLoginLogService appLoginLogService;
    @Autowired
    AppUserService appUserService;
    @Autowired
    AppVersionService appVersionService;
    @Autowired
    AppHelpCenterService appHelpCenterService;
    @Autowired
    CommonService commonService;
    @Autowired
    FileProcessorHolder fileProcessorHolder;
    @Autowired
    Environment env;
    @Autowired
    AppSmsLogService appSmsLogService;
    @Autowired
    AppHomePageLogService appHomePageLogService;
    @Autowired
    AppLotteryTypeLogService appLotteryTypeLogService;
    @Autowired
    AppDownloadLogService appDownloadLogService;
    @Autowired
    AppBannerService appBannerService;
    @Autowired
    AppDataDictionaryService appDataDictionaryService;
    @Autowired
    AMapUtil aMapUtil;

    /**
     * 应用每次启动时，获取密钥
     *
     * @param appType         类型 例如 android ios
     * @param appVersion      应用版本号 1.0.0
     * @param deviceModel     设备型号 如：htc m8
     * @param mobileOs        操作系统
     * @param mobileOsVersion 操作系统版本
     * @param deviceId        设备唯一标识
     * @param carrier         运营商
     * @param resolution      手机分辨率：800*800
     * @return
     */
    @PostMapping("/key")
    public R<Map<String, Object>> key(
            @NotNull String appType,     // 固定值
            @NotNull String appVersion,  // 固定值
            @NotNull String deviceModel, // 自动获取
            @NotNull String mobileOs,
            @NotNull String mobileOsVersion,
            @NotNull String deviceId,
            @NotNull String carrier,     // 运营商
            @NotNull String resolution) throws Exception {
        int appVersionValue = CommonUtils.formatVersion(appVersion);
        if ("prod".equals(env.getProperty("spring.profiles.active"))) {
            if (appVersionValue < 110) {
                throw new ApiException("服务已关闭");
            }
        }

        String clientId = UUID.randomUUID().toString().replaceAll("-", "");
        // 判断用户是否登陆过
        boolean isLogin = RequestContext.isLogin();
        Integer userId = RequestContext.getUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("clientId", clientId);
        if (!isLogin) {
            result.put("login", false);
            // 生成secret
            result.put("secret", commonService.createSecret(clientId));
        } else if (StringUtils.isEmpty(appUserService.getById(userId).getMobile())) {// 第三方登录时还没手机号
            result.put("login", false);
            // 生成secret
            result.put("secret", commonService.createSecret(clientId));
        } else {
            // 登陆过
            result.put("login", true);
            result.put("userId", userId);
        }
//        // 准备日志数据
//        AppLoginLog log = new AppLoginLog();
//        log.setAppType(appType);
//        log.setAppVersion(appVersion);
//        log.setDeviceModel(deviceModel);
//        log.setOs(mobileOs);
//        log.setOsVersion(mobileOsVersion);
//        log.setDeviceId(deviceId);
//        log.setCarrier(carrier);
//        log.setResolution(resolution);
//        log.setClientId(clientId);
//        log.setUserId(userId);
//        // 记录登陆日志
//        appLoginLogService.save(log);
        return success(result);
    }


    /**
     * 前端埋点
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/buryPoint")
    public R<String> userOpLog(@RequestParam int type,
                               @RequestParam(required = false) String mobile,
                               @RequestParam(required = false) String msg,
                               @RequestParam(required = false) int lotteryType,
                               @RequestParam(required = false) String downloadType,
                               @RequestParam(required = false) String version) {
        Integer userId = RequestContext.getUserId();
        if (AppLogType.SMS_LOG.getValue() == type) {
            AppSmsLog appSmsLog = new AppSmsLog();
            appSmsLog.setMobile(mobile);
            appSmsLog.setMsg(msg);
            appSmsLog.setUserId(userId);
            appSmsLogService.save(appSmsLog);
        } else if (AppLogType.HOMEPAGE_LOG.getValue() == type) {
            AppHomePageLog appHomePageLog = new AppHomePageLog();
            appHomePageLog.setUserId(userId);
            appHomePageLogService.save(appHomePageLog);
        } else if (AppLogType.LOTTERY_TYPE_LOG.getValue() == type) {
            AppLotteryTypeLog appLotteryTypeLog = new AppLotteryTypeLog();
            appLotteryTypeLog.setUserId(userId);
            appLotteryTypeLog.setLotterType(lotteryType);
            appLotteryTypeLogService.save(appLotteryTypeLog);
        } else if (AppLogType.DOWNLOAD_LOG.getValue() == type) {
            AppDownloadLog appDownloadLog = new AppDownloadLog();
            appDownloadLog.setType(downloadType);
            appDownloadLog.setVersion(version);
            appDownloadLogService.save(appDownloadLog);
        }

        return success("OK");
    }


    /**
     * 用户行为日志
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/userOpLog")
    public R<String> userOpLog(
            @NotNull String appType,     // 固定值
            @NotNull String appVersion,  // 固定值
            @NotNull String deviceModel, // 自动获取
            @NotNull String mobileOs,
            @NotNull String mobileOsVersion,
            @NotNull String deviceId,
            @NotNull String carrier,     // 运营商
            @NotNull String resolution,
            @NotNull Integer type
    ) throws Exception {
        String clientId = UUID.randomUUID().toString().replaceAll("-", "");
        // 准备日志数据
        AppLoginLog log = new AppLoginLog();
        log.setAppType(appType);
        log.setAppVersion(appVersion);
        log.setDeviceModel(deviceModel);
        log.setOs(mobileOs);
        log.setOsVersion(mobileOsVersion);
        log.setDeviceId(deviceId);
        log.setCarrier(carrier);
        log.setResolution(resolution);
        log.setClientId(clientId);
        log.setType(type);
        log.setUserId(RequestContext.getUserId());
        // 记录登陆日志
        appLoginLogService.save(log);
        return success("OK");
    }

    /**
     * 获取最新版本信息
     *
     * @param appType    类型 例如 android ios
     * @param appVersion 应用版本号 1.0.0
     * @return
     * @throws Exception
     */
    @GetMapping("/getLatestVersion")
    public R<Map<String, Object>> getLatestVersion(@NotBlank String appType, @NotBlank String appVersion) throws Exception {
        return success(appVersionService.getLatestVersion(appType, CommonUtils.formatVersion(appVersion)));
    }


    /**
     * 获取banner
     *
     * @return
     */
    @GetMapping("/getBannerContent")
    public R<String> getBannerContent(@RequestParam Integer id) {
        AppBanner appBanner = appBannerService.getById(id);
        if (appBanner == null) {
            throw new ApiException("banner不存在");
        }
        return success(appBanner.getHref());
    }

    /**
     * 获取banner
     *
     * @return
     */
    @GetMapping("/getBanners")
    public R<List<AppBanner>> getBanners(@RequestParam(required = false) Integer type) {
        if (type != null) {
            List<AppBanner> bannerList = appBannerService.list(Wrappers.<AppBanner>lambdaQuery()
                    .eq(AppBanner::getStatus, 1)
                    .eq(AppBanner::getState, 0)
                    .eq(AppBanner::getType, type)
                    .orderByDesc(AppBanner::getWeight));
            for (AppBanner appBanner : bannerList) {
                if (appBanner.getJumpType().equals(1)) {
                    appBanner.setHref(null);
                }
            }
            return success(bannerList);
        } else {
            List<AppBanner> bannerList = appBannerService.list(Wrappers.<AppBanner>lambdaQuery()
                    .eq(AppBanner::getStatus, 1)
                    .eq(AppBanner::getState, 0)
                    .orderByDesc(AppBanner::getWeight));
            for (AppBanner appBanner : bannerList) {
                if (appBanner.getJumpType().equals(1)) {
                    appBanner.setHref(null);
                }
            }
            return success(bannerList);
        }


    }


    /**
     * 获取服务器时间
     *
     * @return
     * @throws Exception
     */
    @RequestMapping("/getServerTime")
    public R<Long> getDateTime() throws Exception {
        return success(System.currentTimeMillis());
    }

    /**
     * 获取频繁被问到的问题
     *
     * @return
     */
    @GetMapping("/getFAQ")
    public R<List<AppHelpCenter>> getFAQ() throws Exception {
        List<AppHelpCenter> list = appHelpCenterService.list(Wrappers.<AppHelpCenter>lambdaQuery()
                .orderByDesc(AppHelpCenter::getWeight));
        return success(list);
    }

    /**
     * 目录层级为一级的单文件上传
     * 若目录层级比较深，则可以在FileProcessor的实现类手动指定目录层级
     *
     * @param file
     * @param uploadpath
     * @return
     * @throws Exception
     */
    @PostMapping("/{uploadpath}/upload")
    public R<JSONObject> uploadOne(
            @NotNull @RequestParam("file") MultipartFile file,
            @PathVariable("uploadpath") String uploadpath) throws Exception {
        JSONObject param = new JSONObject();
        param.put("file", file);
        FileProcessorHolder.FileProcessor processor = fileProcessorHolder.getProcessor(uploadpath);
        if (processor == null) {
            throw new ApiException("未找到[" + uploadpath + "]相关的文件上传 processor");
        }
        JSONObject process = processor.process(param);
        return success(process);
    }

    /**
     * 获取 App 配置参数
     *
     * @return
     * type 1-提现配置，2-下单配置
     */
    @GetMapping("/appconf")
    public R<Map<String, Object>> getAppconf(@RequestParam("type") Integer type) {
        Map<String, Object> config = new HashMap<>();
        if(type.equals(1)){
            config.put("serviceRate", AppConsts.SERVICE_RATE);
            config.put("maxServiceFee", AppConsts.MAX_SERVICE_FEE);
            config.put("maxWithdrawNum", AppConsts.MAX_WITHDRAW_NUM);
            AppDataDictionary appDataDictionary= appDataDictionaryService.getById(2);
            String[] value=appDataDictionary.getDataValue().split(",");
            config.put("minWithdrawFee", Integer.valueOf(value[0]));
            config.put("maxWithdrawFee", Integer.valueOf(value[1]));
        }else if(type.equals(2)){
            //投注金额区间
            AppDataDictionary appDataDictionary= appDataDictionaryService.getById(3);
            String[] value=appDataDictionary.getDataValue().split(",");
            config.put("minFee", Integer.valueOf(value[0]));
            config.put("maxFee", Integer.valueOf(value[1]));

            //投注倍数区间
            appDataDictionary= appDataDictionaryService.getById(18);
            value=appDataDictionary.getDataValue().split(",");
            config.put("minBetTime", Integer.valueOf(value[0]));
            config.put("maxBetTime", Integer.valueOf(value[1]));
        }
        return success(config);
    }


    /**
     * 获取配置信息
     *
     * @return type 1:打榜最低金额 2:推荐最低金额
     */
    @GetMapping("/getOrderConfig")
    public R<String> getOrderConfig(@RequestParam int type) {
        AppDataDictionary appDataDictionary;
        if (type == 1) {
            appDataDictionary = appDataDictionaryService.getById(10);
            return success(appDataDictionary.getDataValue());
        } else if (type == 2) {
            appDataDictionary = appDataDictionaryService.getById(9);
            return success(appDataDictionary.getDataValue());
        }
        return success(null);
    }

    /**
     * 暂时不用
     * @param lon 经度
     * @param lat 纬度
     * @return
     */
    @GetMapping("/address")
    public R<String> getBannerContent(@RequestParam Double lon, @RequestParam Double lat) {
        String address = aMapUtil.getAddress(lon, lat);
        return success(address);
    }

}
