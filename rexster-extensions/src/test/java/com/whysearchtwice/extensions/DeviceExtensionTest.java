package com.whysearchtwice.extensions;

import org.junit.BeforeClass;

import com.whysearchtwice.rexster.extension.DeviceExtension;
import com.whysearchtwice.rexster.extension.UserExtension;

public class DeviceExtensionTest extends ExtensionTest {
    private static UserExtension userExtension;
    private static DeviceExtension deviceExtension;

    @BeforeClass
    public static void setupClass() {
        userExtension = new UserExtension();
        deviceExtension = new DeviceExtension();
    }
}
