package com.bs.lotp.desktop;

import com.bs.lotp.desktop.feed.PluginFeedService;
import com.bs.lotp.desktop.local.InstalledStore;
import com.bs.lotp.desktop.local.PluginInstaller;
import com.bs.lotp.desktop.settings.PluginPaths;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the real Spring context headlessly and checks the service wiring.
 */
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
