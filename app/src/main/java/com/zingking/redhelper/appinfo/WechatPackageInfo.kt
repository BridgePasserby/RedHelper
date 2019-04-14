package com.zingking.redhelper.appinfo

import android.app.Notification
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Copyright © 2018 www.zingking.cn All Rights Reserved.
 * @author Z.kai
 * @date 2019/4/12
 * @description
 */

class WechatPackageInfo : IPackageInfo {
    val SCREEN_LOCK_CLICK_INTERVAL = 3000L // 锁屏状态下弹出带“开”字的框时，不会触发自动点“开”的逻辑，使用延迟处理
    val SCREEN_LOCK_CLICK_TAG = 0x22 // 锁屏状态下弹出带“开”字的框时，不会触发自动点“开”的逻辑，使用延迟处理
    val CHAT_UI_CLASS = "com.tencent.mm.ui.LauncherUI" // 微信聊天界面
    val MONEY_UI_CLASS = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI" // 抢红包界面
    val OPEN_BUTTON_ID = "com.tencent.mm:id/cyf" // "开"按钮
    val MESSAGE_GROUP_ID = "com.tencent.mm:id/alc" // 消息树(ListView)的父控件名
    val handler: Handler = Handler {
        if (it.what == SCREEN_LOCK_CLICK_TAG) {
            clickViewById(OPEN_BUTTON_ID)
        }
        false
    }

    override var event: AccessibilityEvent? = null
    override var iNodeInfoListener: INodeInfoListener? = null
    val TAG = "WechatPackageInfo"

    init {
        println("Init block")
    }

    override fun grabPacket() {
        val rootInActiveWindow: AccessibilityNodeInfo = iNodeInfoListener!!.getNodeInfo() ?: return
        val className = event!!.className.toString()
        when (className) {
            CHAT_UI_CLASS -> { // 微信聊天页面
                val redPacket = filterRedPacket(rootInActiveWindow)
                redPacket?.let {
                    clickPacket(redPacket)
                }
            }
            MONEY_UI_CLASS -> { // 开红包页面
                clickViewById(OPEN_BUTTON_ID)
            }

            else -> {
            }
        }
    }

    private fun clickPacket(redPacket: AccessibilityNodeInfo) {
        redPacket.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        val message = Message.obtain()
        message.what = SCREEN_LOCK_CLICK_TAG
        handler.sendMessageDelayed(message, SCREEN_LOCK_CLICK_INTERVAL)
    }

    private fun filterRedPacket(nodeInfo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var result: AccessibilityNodeInfo? = null
        if (nodeInfo.childCount == 0) {
            nodeInfo.text?.let {
                if ("微信红包" == (nodeInfo.text.toString())) {
                    //注意，需要找到一个可以点击的View
                    Log.i("demo", "Click" + ",isClick:" + nodeInfo.isClickable())
                    var parent: AccessibilityNodeInfo? = nodeInfo.parent
                    while (parent != null) {
                        Log.i("demo", "parent isClick:" + parent.isClickable())
                        if (parent.isClickable) {
                            if (!hasFinish(parent)) {
                                result = parent
                                break
                            }
                        }
                        parent = parent.parent
                    }
                }
            }
        } else {
            for (i in 0 until nodeInfo.childCount) {
                val child = nodeInfo.getChild(i)
                if (child != null) {
                    result = filterRedPacket(child)
                    if (result != null) {
                        break
                    }
                }
            }
        }
        return result
    }

    override fun openApp() {
        val texts: List<CharSequence> = event!!.text
        for (text in texts) {
            if (text.contains("微信红包")) {
                val parcelableData = event!!.parcelableData
                if (parcelableData is Notification) {
                    val pendingIntent = parcelableData.contentIntent
                    pendingIntent.send()
                    Log.i(TAG, "打开微信")
                }
                break
            }
        }
    }

    override fun dealLastMsg() {
        val className = event!!.className.toString()
//        when (className) {
//            CHAT_UI_CLASS -> { // 微信聊天页面
        val rootInActiveWindow = iNodeInfoListener!!.getNodeInfo() ?: return
        val messageGroup: List<AccessibilityNodeInfo>? = rootInActiveWindow.findAccessibilityNodeInfosByViewId(MESSAGE_GROUP_ID)
        if (messageGroup == null || messageGroup.isEmpty()) {
            return
        }
        val frameLayout: AccessibilityNodeInfo = messageGroup.get(0) // 聊天树中的listView
        if (frameLayout.childCount == 0) {
            return
        }
        val listView: AccessibilityNodeInfo = frameLayout.getChild(0) // 聊天树中的listView
        if (listView.childCount == 0) {
            return
        }
        val lastChild = listView.getChild(listView.childCount - 1)
        val redPacket = filterRedPacket(lastChild)
        redPacket?.let {
            clickPacket(redPacket)
        }
//            }
//        }
    }

    private fun clickViewById(s: String) {
        handler.removeMessages(SCREEN_LOCK_CLICK_TAG)
        val rootInActiveWindow = iNodeInfoListener!!.getNodeInfo() ?: return
        val infosByViewId = rootInActiveWindow.findAccessibilityNodeInfosByViewId(s)
        for (node: AccessibilityNodeInfo in infosByViewId) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    private fun hasFinish(parent: AccessibilityNodeInfo): Boolean {
        if (parent.childCount > 0) {
            for (i in 0..parent.childCount - 1) {
                if (hasFinish(parent.getChild(i))) {
                    return true
                }
            }
        }
        parent.text?.let {
            if ("已领取" == (parent.text.toString())
                    || "已被领完" == (parent.text.toString())
                    || "以过期" == (parent.text.toString())) {
                Log.i(TAG, "已领取")
                return true
            }
        }
        return false
    }

}