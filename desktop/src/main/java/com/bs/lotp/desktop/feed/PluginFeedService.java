package com.bs.lotp.desktop.feed;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Downloads and parses the lotrointerface plugin compendium XML feed. Call off the FX thread. */
@Service
public class PluginFeedService {

    public static final String FEED_URL = "https://api.lotrointerface.com/fav/plugincompendium.xml";

    private final RestClient client;
    private final String feedUrl;

    @Autowired
    public PluginFeedService(RestClient.Builder builder) {
        this(builder, FEED_URL);
    }

    public PluginFeedService(RestClient.Builder builder, String feedUrl) {
        this.client = builder.build();
        this.feedUrl = feedUrl;
    }

    public List<PluginInfo> fetch() throws IOException {
        byte[] body;
        try {
            body = client.get().uri(feedUrl).retrieve().body(byte[].class);
        } catch (RestClientException e) {
            throw new IOException("Feed download failed: " + e.getMessage(), e);
        }
        if (body == null || body.length == 0) {
            throw new IOException("Feed returned an empty body");
        }
        try {
            return parse(body);
        } catch (Exception e) {
            throw new IOException("Failed to parse plugin feed: " + e.getMessage(), e);
        }
    }

    /** Package-visible for tests. */
    static List<PluginInfo> parse(byte[] xml) throws Exception {
        List<PluginInfo> result = new ArrayList<>();
        XMLInputFactory factory = XMLInputFactory.newFactory();
        // Hardening: no DTDs / external entities in this feed.
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        XMLStreamReader reader =
                factory.createXMLStreamReader(new ByteArrayInputStream(xml), StandardCharsets.UTF_8.name());

        String name = null, author = null, version = null, category = null,
                description = "", file = null, md5 = "", fileUrl = null;
        long uid = 0, updated = 0, downloads = 0, size = 0;
        boolean inUi = false;
        StringBuilder text = new StringBuilder();
        String element = null;

        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    element = reader.getLocalName();
                    if ("Ui".equals(element)) {
                        inUi = true;
                        name = author = version = category = file = fileUrl = null;
                        description = "";
                        md5 = "";
                        uid = updated = downloads = size = 0;
                    }
                    text.setLength(0);
                }
                case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA ->
                        text.append(reader.getText());
                case XMLStreamConstants.END_ELEMENT -> {
                    String end = reader.getLocalName();
                    if ("Ui".equals(end) && inUi) {
                        inUi = false;
                        result.add(new PluginInfo(
                                uid,
                                orEmpty(name), orEmpty(author), orEmpty(version),
                                updated == 0 ? null : Instant.ofEpochSecond(updated),
                                downloads, orEmpty(category), description,
                                orEmpty(file), md5, size, orEmpty(fileUrl)));
                    } else if (inUi && end.equals(element)) {
                        String value = text.toString().trim();
                        switch (end) {
                            case "UID" -> uid = parseLong(value);
                            case "UIName" -> name = value;
                            case "UIAuthorName" -> author = value;
                            case "UIVersion" -> version = value;
                            case "UIUpdated" -> updated = parseLong(value);
                            case "UIDownloads" -> downloads = parseLong(value);
                            case "UICategory" -> category = value;
                            case "UIDescription" -> description = value;
                            case "UIFile" -> file = value;
                            case "UIMD5" -> md5 = value;
                            case "UISize" -> size = parseLong(value);
                            case "UIFileURL" -> fileUrl = value;
                            default -> {
                            }
                        }
                    }
                    element = null;
                    text.setLength(0);
                }
                default -> {
                }
            }
        }
        reader.close();
        return result;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
