package com.genai.common.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XmlUtil {

    /**
     * XML 파싱
     *
     * @param content XML 본문
     * @return DOM 객체
     */
    public static Document parseXml(String content) {
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder Builder = factory.newDocumentBuilder();

            return Builder.parse(input);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("xml parse error");
        }
    }

    /**
     * 직계 자식 노드 리스트 반환
     *
     * @param element 노드
     * @return 노드 리스트
     */
    public static List<Element> findChildElements(Element element) {

        List<Element> elements = new ArrayList<>();

        NodeList nodes = element.getChildNodes();
        for (int nodeIndex = 0; nodeIndex < nodes.getLength(); nodeIndex++) {
            Node node = nodes.item(nodeIndex);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }

        return elements;
    }

    /**
     * 직계 자식 노드 반환
     *
     * @param element 노드
     * @param tagName 태그명
     * @return 노드
     */
    public static Element findChildElementByTagName(Element element, String tagName) {

        NodeList nodes = element.getChildNodes();
        for (int nodeIndex = 0; nodeIndex < nodes.getLength(); nodeIndex++) {
            Node node = nodes.item(nodeIndex);

            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(tagName)) {
                return (Element) node;
            }
        }

        return null;
    }

    /**
     * 직계 자식 노드 리스트 반환
     *
     * @param element 노드
     * @param tagName 태그명
     * @return 노드 리스트
     */
    public static List<Element> findChildElementsByTagName(Element element, String tagName) {

        List<Element> elements = new ArrayList<>();

        NodeList nodes = element.getChildNodes();
        for (int nodeIndex = 0; nodeIndex < nodes.getLength(); nodeIndex++) {
            Node node = nodes.item(nodeIndex);

            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(tagName)) {
                elements.add((Element) node);
            }
        }

        return elements;
    }

    /**
     * 자식 노드 리스트 반환
     *
     * @param element 노드
     * @param tagName 태그명
     * @return 노드 리스트
     */
    public static List<Element> findElementsByTagName(Element element, String tagName) {
        return findElementsByTagName(element, tagName, Collections.emptyList());
    }

    /**
     * 자식 노드 리스트 반환
     *
     * @param element         노드
     * @param tagName         태그명
     * @param excludeTagNames 제외할 부모 태그명
     * @return 노드 리스트
     */
    public static List<Element> findElementsByTagName(Element element, String tagName, List<String> excludeTagNames) {

        List<Element> elements = new ArrayList<>();

        NodeList nodes = element.getElementsByTagName(tagName);
        for (int nodeIndex = 0; nodeIndex < nodes.getLength(); nodeIndex++) {
            Node node = nodes.item(nodeIndex);
            if (!excludeTagNames.isEmpty() && isIncludeTagNames((Element) node, excludeTagNames)) continue;
            elements.add((Element) node);
        }

        return elements;
    }

    /**
     * 부모 노드 중 태그명 포함 여부 확인
     *
     * @param element  노드
     * @param tagNames 태그명 목록
     * @return 포함 여부
     */
    private static boolean isIncludeTagNames(Element element, List<String> tagNames) {
        Node parent = element.getParentNode();
        while (parent != null) {
            if (parent.getNodeType() == Node.ELEMENT_NODE) {
                Element parentElement = (Element) parent;

                for (String tagName : tagNames) {
                    if (tagName.equals(parentElement.getTagName())) {
                        return true;
                    }
                }
            }
            parent = parent.getParentNode();
        }
        return false;
    }
}
