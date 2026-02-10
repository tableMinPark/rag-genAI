package com.genai.common.utils;

import com.genai.common.vo.HwpImageVO;
import kr.dogfoot.hwp2hwpx.Hwp2Hwpx;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HwpUtil {

    public static void convertHwpToHwpx(String fromFilePath, String toFilePath) {
        try {
            HWPFile fromFile = HWPReader.fromFile(fromFilePath);
            HWPXFile toFile = Hwp2Hwpx.toHWPX(fromFile);
            HWPXWriter.toFilepath(toFile, toFilePath);
        } catch (Exception e) {
            // 변환 실패
            throw new RuntimeException("not support hwp file");
        }
    }

    /**
     * XML 표 데이터 HTML 변환 (재귀)
     *
     * @param element 표 태그 노드
     * @return 표 데이터 HTML 문자열
     */
    public static String convertTableXmlToHtml(Element element, int depth) {
        return convertTableXmlToHtml(element, Collections.emptyMap(), depth);
    }

    /**
     * XML 표 데이터 HTML 변환 (재귀)
     *
     * @param element 표 태그 노드
     * @return 표 데이터 HTML 문자열
     */
    public static String convertTableXmlToHtml(Element element, Map<String, HwpImageVO> hwpxImages, int depth) {

        AtomicInteger rowMax = new AtomicInteger();
        AtomicInteger colMax = new AtomicInteger();
        StringBuilder tableHtmlBodyBuilder = new StringBuilder();

        tableHtmlBodyBuilder.append("<tbody>");
        XmlUtil.findChildElementsByTagName(element, "hp:tr").forEach(tr -> {
            rowMax.incrementAndGet();
            tableHtmlBodyBuilder.append("<tr>");

            AtomicInteger colCount = new AtomicInteger();
            XmlUtil.findChildElementsByTagName(tr, "hp:tc").forEach(td -> {
                Element cellSpan = XmlUtil.findChildElementByTagName(td, "hp:cellSpan");

                // 표 병합 체크
                if (cellSpan != null) {
                    String colSpan = cellSpan.getAttribute("colSpan");
                    String rowSpan = cellSpan.getAttribute("rowSpan");
                    colCount.set(colCount.get() + Integer.parseInt(colSpan));

                    if (!"1".equals(colSpan) && !"1".equals(rowSpan)) {
                        tableHtmlBodyBuilder
                                .append("<td")
                                .append(" ").append("colspan=\"").append(colSpan).append("\"")
                                .append(" ").append("rowspan=\"").append(rowSpan).append("\"")
                                .append(">");
                    } else if ("1".equals(colSpan) && !"1".equals(rowSpan)) {
                        tableHtmlBodyBuilder
                                .append("<td")
                                .append(" ").append("rowspan=\"").append(rowSpan).append("\"")
                                .append(">");
                    } else if (!"1".equals(colSpan)) {
                        tableHtmlBodyBuilder
                                .append("<td")
                                .append(" ").append("colspan=\"").append(colSpan).append("\"")
                                .append(">");
                    } else tableHtmlBodyBuilder.append("<td>");
                } else tableHtmlBodyBuilder.append("<td>");

                // 표 셀 데이터 추출
                XmlUtil.findChildElementsByTagName(td, "hp:subList").forEach(subList -> {
                    List<Element> ps = XmlUtil.findChildElementsByTagName(subList, "hp:p");
                    for (int pIndex = 0; pIndex < ps.size(); pIndex++) {
                        XmlUtil.findChildElementsByTagName(ps.get(pIndex), "hp:run").forEach(run ->
                                XmlUtil.findChildElements(run).forEach(node -> {
                                    switch (node.getNodeName()) {
                                        // 텍스트
                                        case "hp:t" -> tableHtmlBodyBuilder.append(node.getTextContent());
                                        // 표
                                        case "hp:tbl" ->
                                                tableHtmlBodyBuilder.append(convertTableXmlToHtml(node, depth + 1));
                                        // 이미지
                                        case "hp:pic" -> {
                                            Element img = XmlUtil.findChildElementByTagName(node, "hc:img");
                                            if (img != null) {
                                                String id = img.getAttribute("binaryItemIDRef");
                                                tableHtmlBodyBuilder.append(hwpxImages.containsKey(id) ? hwpxImages.get(id).getContent() : "");
                                            }
                                        }
                                    }
                                }));

                        if (pIndex < ps.size() - 1) {
                            tableHtmlBodyBuilder.append("<br>");
                        }
                    }
                });

                tableHtmlBodyBuilder.append("</td>");
            });

            colMax.set(Math.max(colMax.get(), colCount.get()));
            tableHtmlBodyBuilder.append("</tr>");
        });

        tableHtmlBodyBuilder.append("</tbody>");


        StringBuilder tableHtmlBuilder = new StringBuilder();

        tableHtmlBuilder.append("<table>");

        if (rowMax.get() <= 1) {
            tableHtmlBuilder.append("<thead>");
            tableHtmlBuilder.append("<tr>");
            tableHtmlBuilder.append("<td></td>".repeat(Math.max(0, colMax.get())));
            tableHtmlBuilder.append("</tr>");
            tableHtmlBuilder.append("</thead>");
        }

        tableHtmlBuilder.append(tableHtmlBodyBuilder);
        tableHtmlBuilder.append("</table>");

        return tableHtmlBuilder.toString();
    }
}
