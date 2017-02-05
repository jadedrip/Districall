package org.caffy.districall.pool;

import org.caffy.districall.impl.Services;
import org.caffy.districall.interf.IServicePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Chen Wang on 2016/7/1 0001.
 */
@SuppressWarnings("unused")
public class StaticServicePool implements IServicePool {
    private static final Logger logger = LoggerFactory.getLogger(StaticServicePool.class);

    private ConcurrentHashMap<String, Services> nameServices = new ConcurrentHashMap<String, Services>();

    public StaticServicePool(String filename) throws SAXException, IOException {
        XMLReader parser = XMLReaderFactory.createXMLReader();
        parser.setContentHandler(new DefaultHandler() {
            class Group {
                List<String> names = new ArrayList<String>();
                List<String> servers = new ArrayList<String>();

                void store() {
                    for (String name : names) {
                        Services x = new Services();
                        x.onReset(servers);
                        nameServices.put(name, x);
                    }
                }
            }

            Group group = null;
            int type = 0;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                if ("group".equals(qName))
                    group = new Group();
                if ("server".equals(qName))
                    type = 1;
                else if ("name".equals(qName))
                    type = 2;
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                type = 0;
                if ("group".equals(qName)) {
                    group.store();
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                String n;
                switch (type) {
                    case 1:
                        n = new String(ch, start, length);
                        group.servers.add(n);
                        break;
                    case 2:
                        n = new String(ch, start, length);
                        group.names.add(n);
                        break;
                }
            }
        });

        if (filename.startsWith("classpath:")) {
            String f = filename.substring(10);
            InputStream stream = StaticServicePool.class.getResourceAsStream(f);
            if (stream == null)
                stream = StaticServicePool.class.getResourceAsStream("/" + f);
            if (stream == null) throw new FileNotFoundException();
            InputSource source = new InputSource(stream);
            parser.parse(source);
        } else {
            parser.parse(filename);
        }
    }

    @Override
    public Services queryService(String name) throws Exception {
        return nameServices.get(name);
    }
}
