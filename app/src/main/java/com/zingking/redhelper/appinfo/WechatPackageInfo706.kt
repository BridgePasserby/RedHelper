package com.zingking.redhelper.appinfo

/**
 * Copyright © 2018 www.zingking.cn All Rights Reserved.
 * @author Z.kai
 * @date 2019/7/18
 * @description 适配 微信7.0.5 版本
 */

class WechatPackageInfo706 : WechatPackageInfo703() {
    override var OPEN_BUTTON_ID = "com.tencent.mm:id/d5a" // "开"按钮
    override val MESSAGE_GROUP_ID = "com.tencent.mm:id/anm" // 消息树(ListView)的父控件名

}