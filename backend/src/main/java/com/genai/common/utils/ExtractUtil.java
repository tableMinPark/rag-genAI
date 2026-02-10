package com.genai.common.utils;

import com.genai.common.vo.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ExtractUtil {

    private final static Set<String> ALLOW_EXT = Set.of("hwp", "hwpx", "pdf");

    /**
     * 텍스트 추출
     *
     * @param fullPath 파일 경로
     * @return 추출 문자열
     */
    public static String extractText(String fullPath, String ext) {

        StringBuilder textBuilder = new StringBuilder();

        extractDocument(fullPath, ext).getDocumentContents().forEach(documentContent -> {
            textBuilder.append(documentContent.getContext()).append("\n");
        });

        return textBuilder.toString().trim();
    }

    public static DocumentVO extractDocument(String fullPath) {

        String ext = FileUtil.extractExt(fullPath);

        if (ext.trim().isBlank()) {
            throw new RuntimeException("not found file extension");
        }

        return extractDocument(fullPath, ext);
    }

    public static DocumentVO extractDocument(String fullPath, String ext) {

        if (!ALLOW_EXT.contains(ext.toLowerCase())) {
            throw new RuntimeException("not support file extension");
        }

        if (ext.equals("hwp") || ext.equals("hwpx")) {
            return extractHwpDocument(fullPath, ext);
        } else {
            return extractPdfDocument(fullPath);
        }
    }

    public static DocumentVO extractHwpDocument(String fullPath, String ext) {

        if (!ext.equals("hwp") && !ext.equals("hwpx")) {
            throw new RuntimeException("not support file extension");
        }

        // 사용 파일 경로
        Path fullFilePath = Paths.get(fullPath);
        String originFileName = StringUtil.removeExtension(fullFilePath.getFileName().toString());

        try {
            Path zipFilePath = Files.createTempFile("input-", ".zip");

            if (!fullFilePath.toFile().exists()) {
                throw new RuntimeException("not found target file");
            }

            if (ext.equals("hwp")) {
                // HWP 파일 변환
                HwpUtil.convertHwpToHwpx(fullFilePath.toString(), zipFilePath.toString());
            } else {
                FileUtil.copyFile(fullFilePath.toString(), zipFilePath.toString());
            }

            // 압축 파일 존재 여부 확인
            if (!zipFilePath.toFile().exists()) {
                throw new RuntimeException("not exists zip file");
            }

            // 압축 해제
            Path unZipDirPath = Paths.get(FileUtil.decompression(zipFilePath.toString()));

            // metadata 추출
            Path metaDataPath = unZipDirPath.resolve("Contents").resolve("content.hpf");
            String metaData = FileUtil.read(metaDataPath.toString());

            // XML DOM 파싱
            Element root = XmlUtil.parseXml(metaData).getDocumentElement();
            NodeList items = root.getElementsByTagName("opf:item");

            // 데이터 저장
            List<HwpSectionVO> sections = new ArrayList<>();
            Map<String, HwpImageVO> images = new HashMap<>();

            for (int itemIndex = 0; itemIndex < items.getLength(); itemIndex++) {
                Node item = items.item(itemIndex);

                String resourceId = item.getAttributes().getNamedItem("id").getTextContent();
                String resourceFilePath = item.getAttributes().getNamedItem("href").getTextContent();
                String mediaType = item.getAttributes().getNamedItem("media-type").getTextContent();

                if (mediaType.endsWith("xml") && resourceId.startsWith("section")) {
                    File xmlFile = unZipDirPath.resolve(resourceFilePath).toFile();

                    if (xmlFile.exists()) {
                        String content = FileUtil.read(xmlFile.toPath().toString())
                                .replaceAll("<hp:lineBreak/>", "\n")                        // 개행 태그 개행 문자로 치환
                                .replaceAll("\\s[a-zA-Z_-]+=\"[^\"]*[<>][^\"]*\"", "");     // XML 속성 내에 "<", ">" 가 있는 경우 속성 제거

                        sections.add(HwpSectionVO.builder()
                                .id(resourceId)
                                .content(content)
                                .build());
                    }
                } else if (mediaType.startsWith("image/")) {
                    File imageFile = unZipDirPath.resolve(resourceFilePath).toFile();

                    // TODO: Image -> Text 추출 (OCR)
                    String content = "";

                    if (imageFile.exists()) {
                        images.put(resourceId, HwpImageVO.builder()
                                .id(resourceId)
                                .content(content)
                                .path(imageFile.toPath())
                                .ext(mediaType)
                                .build());
                    }
                }
            }

            // 압축 파일 삭제
            FileUtil.deleteFile(zipFilePath.toString());

            // 압축 해제 디렉토리 삭제
            FileUtil.deleteDirectory(unZipDirPath.toString());

            return new DocumentVO(originFileName, ext, extractHwpxContents(sections, images));

        } catch (IOException e) {
            throw new RuntimeException("hwp parsing error", e);
        }
    }

    private static List<DocumentContentVO> extractHwpxContents(List<HwpSectionVO> sections, Map<String, HwpImageVO> images) {

        List<DocumentContentVO> documentContents = new ArrayList<>();

        sections.forEach(section -> {
            Document document = XmlUtil.parseXml(section.getContent());
            Element root = document.getDocumentElement();

            XmlUtil.findChildElementsByTagName(root, "hp:p").forEach(p -> {
                StringBuilder contentBuilder = new StringBuilder();

                for (Element run : XmlUtil.findChildElementsByTagName(p, "hp:run")) {
                    for (Element node : XmlUtil.findChildElements(run)) {
                        switch (node.getNodeName()) {
                            // 텍스트
                            case "hp:t" -> contentBuilder.append(node.getTextContent());
                            // 표
                            case "hp:tbl" -> {
                                Arrays.stream(contentBuilder.toString().split("\n")).forEach(content -> {
                                    documentContents.add(DocumentContentVO.text(content));
                                });
                                String tableContent = HwpUtil.convertTableXmlToHtml(node, 0);
                                tableContent = HtmlUtil.removeHtmlExceptTable(tableContent);
                                documentContents.add(DocumentContentVO.table(tableContent));

                                contentBuilder = new StringBuilder();
                            }
                            // 이미지
                            case "hp:pic" -> {
                                Arrays.stream(contentBuilder.toString().split("\n")).forEach(content -> {
                                    documentContents.add(DocumentContentVO.text(content));
                                });

                                Element img = XmlUtil.findChildElementByTagName(node, "hc:img");

                                if (img != null) {
                                    String id = img.getAttribute("binaryItemIDRef");
                                    String content = images.containsKey(id) ? images.get(id).getContent() : "";
                                    documentContents.add(DocumentContentVO.image(content));
                                }

                                contentBuilder = new StringBuilder();
                            }
                        }
                    }
                }

                if (!contentBuilder.isEmpty()) {
                    Arrays.stream(contentBuilder.toString().split("\n")).forEach(content -> {
                        documentContents.add(DocumentContentVO.text(content));
                    });
                }
            });
        });

        return documentContents;
    }

    public static DocumentVO extractPdfDocument(String fullPath) {

        Path fullFilePath = Paths.get(fullPath);
        String originFileName = StringUtil.removeExtension(fullFilePath.getFileName().toString());
        String ext = FileUtil.extractExt(fullPath);

        if (!fullFilePath.toFile().exists()) {
            throw new RuntimeException("not found target file");
        }

        List<PdfSectionVO> sections = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(fullFilePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            SpreadsheetExtractionAlgorithm tableExtractor = new SpreadsheetExtractionAlgorithm();
            PageIterator pi = new ObjectExtractor(document).extract();

            while (pi.hasNext()) {
                Page page = pi.next();
                int pageNumber = page.getPageNumber();

                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String text = stripper.getText(document);
                text = normalize(text);

                List<Table> tables = tableExtractor.extract(page);
                List<List<List<String>>> tableRows = new ArrayList<>();
//                for (Table table : tables) {
//                    List<List<String>> rows = new ArrayList<>();
//                    table.getRows().forEach(row -> {
//                        rows.add(row.stream().map(cell ->
//                            cell.getText().replaceAll("\\s+", " ").trim()).toList());
//                    });
//                    tableRows.add(rows);
//                }

                sections.add(new PdfSectionVO(pageNumber, text, tableRows));
            }

        } catch (Exception e) {
            throw new RuntimeException("pdf parsing error", e);
        }

        return new DocumentVO(originFileName, ext, extractPdfContents(sections));
    }

    private static List<DocumentContentVO> extractPdfContents(List<PdfSectionVO> sections) {

        List<DocumentContentVO> documentContents = new ArrayList<>();

        sections.forEach(section -> {
            Arrays.stream(section.getText().split("\n")).forEach(content -> {
                documentContents.add(DocumentContentVO.text(content));
            });

            for (List<List<String>> table : section.getTables()) {
                StringBuilder tableHtmlBuilder = new StringBuilder();

                tableHtmlBuilder.append("<table>");
                tableHtmlBuilder.append("<tbody>");
                for (List<String> row : table) {

                    tableHtmlBuilder.append("<tr>");
                    for (String cell : row) {
                        tableHtmlBuilder.append("<td>");
                        tableHtmlBuilder.append(cell);
                        tableHtmlBuilder.append("</td>");
                    }
                    tableHtmlBuilder.append("</tr>");
                }
                tableHtmlBuilder.append("</tbody>");
                tableHtmlBuilder.append("</table>");

                String tableContent = HtmlUtil.removeHtmlExceptTable(tableHtmlBuilder.toString());
                documentContents.add(DocumentContentVO.table(tableContent));
            }
        });

        return documentContents;
    }

    /**
     * 텍스트 정규화 (누락 방지용 최소 처리)
     */
    private static String normalize(String text) {
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
