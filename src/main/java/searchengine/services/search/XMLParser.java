package searchengine.services.search;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

@Service
public class XMLParser {

    public String getTitle(String content) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        String title = "";
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(content);
            doc.getDocumentElement().normalize();
            NodeList list = doc.getElementsByTagName("title");
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                title = node.getTextContent();
            }

        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
        return title;
    }

}
