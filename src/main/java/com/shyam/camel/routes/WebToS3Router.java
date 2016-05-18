package com.shyam.camel.routes;

import com.shyam.camel.handlers.SitemapIndexHandler;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.common.HttpMethods;
import org.apache.camel.model.ExpressionNode;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.commons.collections4.list.AbstractLinkedList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Component
public class WebToS3Router extends RouteBuilder {

    @Autowired
    private SitemapIndexHandler sitemapIndexHandler;

    @Override
    public void configure() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        from("timer:trigger?period=30s&repeatCount=1")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.GET))
                .setHeader(Exchange.HTTP_URI, simple("http://sitemap.target.com/wcsstore/SiteMap/sitemap_index.xml.gz"))
                .to("http4:dummyUrl")
                .process(exchange -> {
                    InputStream is = exchange.getIn().getBody(InputStream.class);
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser saxParser = factory.newSAXParser();
                    saxParser.parse(is, sitemapIndexHandler);
                })
                .split(body(), new AggregationStrategy() {
                        @Override
                        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                            if (oldExchange == null) {
                                return newExchange;
                            }
                            List<String> urls = newExchange.getIn().getBody(List.class);
                            List<String> prevUrls = oldExchange.getIn().getBody(List.class);
                            prevUrls.addAll(urls);
                            oldExchange.getIn().setBody(prevUrls);
                            return oldExchange;
                        }
                    }).executorService(executorService)
                    .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.GET))
                    .setHeader(Exchange.HTTP_URI, simple("${body}"))
                    .to("http4:dummyUrl")
                    .process(exchange -> {
                        Object obj = exchange.getIn().getBody();
                        // create a new DocumentBuilderFactory
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        List<String> urls = new ArrayList<>();
                        try {
                            // use the factory to create a documentbuilder
                            DocumentBuilder builder = factory.newDocumentBuilder();
                            // create a new document from input stream
                            Document doc = builder.parse((InputStream) obj);
                            // get the first element
                            Element element = doc.getDocumentElement();
                            // get all child nodes
                            NodeList nodes = element.getChildNodes();
                            // print the text content of each child
                            for (int i = 0; i < nodes.getLength(); i++) {
                                if (!StringUtils.equals(nodes.item(i).getTextContent(), "\n  ")) {
                                    String str = nodes.item(i).getTextContent().replace("\n", "");
                                    str = str.replace(" ", "");
                                    urls.add(str);
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        exchange.getIn().setBody(urls);
                    })
                    .end()
                .to("log:out");
    }
}
