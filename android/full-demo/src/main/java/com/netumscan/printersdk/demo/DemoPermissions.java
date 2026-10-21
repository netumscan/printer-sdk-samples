// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.Manifest;

/** 按实际 SDK 操作声明运行时权限；SPP 连接先取消系统发现，因此也需要 SCAN。 */
final class DemoPermissions {
    enum Operation { SPP_CONNECT, BLE_CONNECT, SPP_DISCOVERY, BLE_DISCOVERY }
    private DemoPermissions() { }

    // 调用方传入实际系统 API；权限名是编译期字符串，旧系统分支不会使用 API 31 的权限。
    @android.annotation.SuppressLint("InlinedApi")
    static String[] required(int api, Operation operation) {
        if (api >= 31) {
            return operation == Operation.SPP_CONNECT || operation == Operation.BLE_DISCOVERY ?
                new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN} :
                new String[]{Manifest.permission.BLUETOOTH_CONNECT};
        }
        return api >= 23 && operation == Operation.BLE_DISCOVERY ?
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION} : new String[0];
    }
}
