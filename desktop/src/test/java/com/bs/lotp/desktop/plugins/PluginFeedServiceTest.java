package com.bs.lotp.desktop.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PluginFeedServiceTest {

    private static final String SAMPLE = """
            <Favorites>
              <Ui>
                <UID>1308</UID>
                <UIName>Broke Legs</UIName>
                <UIAuthorName>Nolemir</UIAuthorName>
                <UIVersion>1.0.2</UIVersion>
                <UIUpdated>1786644762</UIUpdated>
                <UIDownloads>182</UIDownloads>
                <UICategory>Other</UICategory>
                <UIDescription><![CDATA[Tracker for "Falling Injuries" & deaths]]></UIDescription>
                <UIFile>BrokeLegsV1.0.2.zip</UIFile>
                <UIMD5></UIMD5>
                <UISize>2166</UISize>
                <UIFileURL>http://www.lotrointerface.com/downloads/download1308</UIFileURL>
              </Ui>
              <Ui>
                <UID>1310</UID>
                <UIName>Shire Quest &amp; Deed Tracker SQDT</UIName>
                <UIAuthorName>Tanorth</UIAuthorName>
                <UIVersion>v0.4.7.50</UIVersion>
                <UIUpdated>not-a-number</UIUpdated>
                <UIDownloads>437</UIDownloads>
                <UICategory>Other</UICategory>
                <UIDescription><![CDATA[x]]></UIDescription>
                <UIFile>f.zip</UIFile>
                <UISize>10</UISize>
                <UIFileURL>http://example.com/f</UIFileURL>
              </Ui>
            </Favorites>
            """;

    @Test
    void parsesEntries() throws Exception {
        List<PluginInfo> plugins =
                PluginFeedService.parse(SAMPLE.getBytes(StandardCharsets.UTF_8));

        assertEquals(2, plugins.size());

        PluginInfo first = plugins.get(0);
        assertEquals(1308, first.uid());
        assertEquals("Broke Legs", first.name());
        assertEquals("Nolemir", first.author());
        assertEquals("1.0.2", first.version());
        assertEquals(1786644762L, first.updated().getEpochSecond());
        assertEquals(182, first.downloads());
        assertEquals("Other", first.category());
        assertTrue(first.description().contains("Falling Injuries"));
        assertEquals("BrokeLegsV1.0.2.zip", first.fileName());
        assertEquals("", first.md5());
        assertEquals(2166, first.size());
        assertEquals("http://www.lotrointerface.com/downloads/download1308", first.fileUrl());

        PluginInfo second = plugins.get(1);
        assertEquals("Shire Quest & Deed Tracker SQDT", second.name());
        assertNull(second.updated());
        assertEquals("?", second.updatedText());
        assertEquals(10, first.updatedText().length()); // yyyy-MM-dd
        assertEquals("2 KB", new PluginInfo(0, "", "", "", null, 0, "", "", "", "", 2166, "").sizeText());
    }
}
