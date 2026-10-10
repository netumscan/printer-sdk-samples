package com.netumscan.printersdk.demo;

import android.content.Context;
import com.netumscan.printersdk.BluetoothDiscovery;
import java.util.ArrayList;
import java.util.List;

/** Demo seam for discovery UI checks without Bluetooth permissions or platform IO. */
interface DemoDiscovery {
    final class Device {
        final String address, name;
        Device(String address, String name) { this.address = address; this.name = name; }
    }
    interface Completion { void completed(List<Device> devices, int errorCode); }
    List<Device> bondedDevices();
    void scanBle(Completion completion);
    void stop();

    static DemoDiscovery create(Context context) {
        BluetoothDiscovery scanner = new BluetoothDiscovery(context);
        return new DemoDiscovery() {
            public List<Device> bondedDevices() { return candidates(scanner.bondedDevices()); }
            public void scanBle(Completion completion) {
                scanner.scanBle(null, 10000, (devices, code) -> completion.completed(candidates(devices), code));
            }
            public void stop() { scanner.stop(); }
        };
    }
    static List<Device> candidates(List<BluetoothDiscovery.Device> devices) {
        List<Device> found = new ArrayList<>();
        for (BluetoothDiscovery.Device device : devices) found.add(new Device(device.address, device.name));
        return found;
    }
}
