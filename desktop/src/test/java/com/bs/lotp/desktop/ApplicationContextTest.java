package com.bs.lotp.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import com.bs.lotp.desktop.plugins.InstalledStore;
import com.bs.lotp.desktop.plugins.PluginFeedService;
import com.bs.lotp.desktop.plugins.PluginInstaller;
import com.bs.lotp.desktop.plugins.PluginPaths;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Boots the real Spring context headlessly and checks the service wiring. */
class ApplicationContextTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(DesktopApp.class);

    @Test
    void servicesAreWired() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(PluginFeedService.class)
                .hasSingleBean(PluginInstaller.class)
                .hasSingleBean(PluginPaths.class)
                .hasSingleBean(InstalledStore.class));
    }
}
