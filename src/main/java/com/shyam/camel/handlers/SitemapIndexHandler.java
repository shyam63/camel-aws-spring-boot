package com.shyam.camel.handlers;

import org.springframework.stereotype.Component;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SitemapIndexHandler extends DefaultHandler {

    private boolean loc = false;

    private List<String> urls = new ArrayList<>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("loc")) {
            loc = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (loc) {
            String str = new String(ch, start, length);
            str = str.replace("\n", "");
            str = str.replace(" ", "");
            urls.add(str);
            loc = false;
        }
    }

    public List<String> getUrls() {
        return urls;
    }
}
