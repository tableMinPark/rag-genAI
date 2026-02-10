package com.genai.common.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HtmlUtil {

    private static final Set<String> SINGLE_TAGS = Set.of("br");
    private static final Set<String> TABLE_CHILD_TAGS = Set.of("tr", "td", "thead", "tbody", "th", "br");
    private static final Set<String> NEW_LINE_TAGS = Set.of("div", "p", "section", "article", "header", "footer", "aside", "nav", "main", "li", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "hr");
    private static final String NEW_LINE_PREFIX = "__NEW_LINE__";
    private static final String MARKDOWN_NEW_LINE_PREFIX = "__MARKDOWN_NEW_LINE__";

    /**
     * 테이블 태그 존재 여부 확인
     *
     * @param html HTML 문자열
     * @return 존재 여부
     */
    public static boolean isContainsTableHtml(String html) {
        Document doc = Jsoup.parse(html);
        return !doc.body().getElementsByTag("table").isEmpty();
    }

    /**
     * 마크 다운 표 데이터 HTML 변환
     *
     * @param markdown 마크 다운 문자열
     * @return 표 HTML 변환 된, 마크 다운 문자열
     */
    public static String convertTableMarkdownToHtml(String markdown) {
        if (markdown == null) return "";

        String[] lines = markdown.split("\\r?\\n");
        StringBuilder result = new StringBuilder();

        List<String> tableBlock = new ArrayList<>();
        boolean inTable = false;

        for (String line : lines) {
            if (line.trim().startsWith("|")) {
                // 표 시작 또는 이어지는 줄
                tableBlock.add(line);
                inTable = true;
            } else {
                // 표 종료
                if (inTable && !tableBlock.isEmpty()) {
                    String tableMarkdown = String.join("\n", tableBlock);
                    String tableHtml = convertTableMarkdownToHtml(tableMarkdown, false);
                    result.append(tableHtml).append("\n");
                    tableBlock.clear();
                }
                inTable = false;
                // 표가 아닌 일반 텍스트는 그대로
                result.append(line).append("\n");
            }
        }

        // 마지막 표 블록 처리
        if (!tableBlock.isEmpty()) {
            String tableMarkdown = String.join("\n", tableBlock);
            String tableHtml = convertTableMarkdownToHtml(tableMarkdown, false);
            result.append(tableHtml).append("\n");
        }

        return result.toString().trim();
    }

    /**
     * 마크 다운 표 데이터 HTML 변환
     *
     * @param markdown     마크 다운 문자열
     * @param addAttribute 텍스트 정렬 속성 포함 여부
     * @return 표 HTML 변환 된, 마크 다운 문자열
     */
    private static String convertTableMarkdownToHtml(String markdown, boolean addAttribute) {
        if (markdown == null) return "";

        // 1) 이스케이프된 파이프(\|)를 임시 토큰으로 치환
        String token = "__ESCAPED_PIPE__";
        String tmp = markdown.replaceAll("\\\\\\|", token); // regex "\\\\|" matches a backslash + pipe

        // 2) 줄 단위로 나누고 공백 줄 제거
        List<String> lines = Arrays.stream(tmp.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (lines.isEmpty()) return "";

        // 3) 파이프(|)로 분할하는 헬퍼
        Pattern splitPattern = Pattern.compile("\\|");
        java.util.function.Function<String, List<String>> splitRow = row -> {
            // 이스케이프된 파이프는 이미 처리했다고 가정
            String[] parts = row.split("\\|", -1); // -1: 빈 문자열 유지
            List<String> cols = Arrays.stream(parts)
                    .map(String::trim)
                    .filter(s -> true) // 빈 문자열 제거 여부 선택 가능
                    .collect(Collectors.toList());

            // 양 끝 빈 문자열 제거
            if (!cols.isEmpty() && cols.getFirst().isEmpty()) {
                cols.removeFirst();
            }
            if (!cols.isEmpty() && cols.getLast().isEmpty()) {
                cols.removeLast();
            }

            return cols;
        };

        // 4) 헤더 구분선(두 번째 행이 sep인지) 판별
        boolean hasSeparatorLine = false;
        int sepIndex = -1;
        if (lines.size() >= 2) {
            String second = lines.get(1);
            // 각 셀이 --- 또는 :---: 등으로만 이루어졌는지 확인
            List<String> secondCols = splitRow.apply(second);
            boolean allSepLike = true;
            for (String c : secondCols) {
                String t = c.trim();
                if (t.isEmpty()) {
                    // 빈 셀은 무시(허용)
                    continue;
                }
                // 허용 문자: :, -, 공백
                if (!t.matches("^:?-+:?$")) {
                    allSepLike = false;
                    break;
                }
            }
            if (allSepLike) {
                hasSeparatorLine = true;
                sepIndex = 1;
            }
        }

        List<List<String>> rows = new ArrayList<>();
        for (String line : lines) {
            rows.add(splitRow.apply(line));
        }

        // 5) 열 수 정하기 (가장 길게 분할된 열 수에 맞춤)
        int colCount = rows.stream().mapToInt(List::size).max().orElse(0);

        // Normalize each row to same column count (빈 문자열로 채움)
        for (List<String> row : rows) {
            while (row.size() < colCount) row.add("");
        }

        // 6) 정렬 정보 추출 (sep line이 있으면)
        String[] alignments = new String[colCount]; // null이면 기본(없음)
        if (hasSeparatorLine) {
            List<String> sepCols = rows.get(sepIndex);
            for (int i = 0; i < colCount; i++) {
                String cell = i < sepCols.size() ? sepCols.get(i).trim() : "";
                if (cell.startsWith(":") && cell.endsWith(":")) {
                    alignments[i] = "center";
                } else if (cell.startsWith(":")) {
                    alignments[i] = "left";
                } else if (cell.endsWith(":")) {
                    alignments[i] = "right";
                } else {
                    alignments[i] = null;
                }
            }
        }

        // 7) HTML 조립
        StringBuilder html = new StringBuilder();
        html.append("<table>\n");

        int startBodyRow = 0;
        if (hasSeparatorLine) {
            // 헤더는 첫 줄 (rows[0]), sep 라인은 제외
            List<String> headerCols = rows.getFirst();
            html.append("  <thead>\n");
            html.append("    <tr>\n");
            for (int i = 0; i < colCount; i++) {
                String h = i < headerCols.size() ? headerCols.get(i) : "";
                String th = escapeHtml(h);
                if (alignments[i] != null) {
                    if (addAttribute) {
                        html.append(String.format("      <th style=\"text-align:%s\">%s</th>\n", alignments[i], th));
                    } else {
                        html.append(String.format("      <th>%s</th>\n", th));
                    }
                } else {
                    html.append(String.format("      <th>%s</th>\n", th));
                }
            }
            html.append("    </tr>\n");
            html.append("  </thead>\n");
            startBodyRow = sepIndex + 1;
        }

        html.append("  <tbody>\n");
        for (int r = startBodyRow; r < rows.size(); r++) {
            // 만약 sepIndex가 1이면 이미 sep line을 건너뛰었음
            List<String> cols = rows.get(r);
            html.append("    <tr>\n");
            for (int i = 0; i < colCount; i++) {
                String c = i < cols.size() ? cols.get(i) : "";
                String td = escapeHtml(c);
                if (alignments[i] != null) {
                    if (addAttribute) {
                        html.append(String.format("      <td style=\"text-align:%s\">%s</td>\n", alignments[i], td));
                    } else {
                        html.append(String.format("      <td>%s</td>\n", td));
                    }
                } else {
                    html.append(String.format("      <td>%s</td>\n", td));
                }
            }
            html.append("    </tr>\n");
        }
        html.append("  </tbody>\n");
        html.append("</table>\n");

        return html.toString();
    }

    /**
     * HTML 표 데이터 마크 다운 변환
     *
     * @param html HTML 문자열
     * @return 표 Markdown 변환 된, HTML 문자열
     */
    public static String convertTableHtmlToMarkdown(String html) {
        // 개형 변환
        String convertNewLineHtml = html
                .replace("\n", NEW_LINE_PREFIX);

        Document doc = Jsoup.parse(convertNewLineHtml);
        StringBuilder tableMarkdownBuilder = new StringBuilder();

        convertTableHtmlToMarkdown(doc.body(), tableMarkdownBuilder, -1);

        String convertMarkdown = doc.body().html()
                .replace("&lt;", "<")
                .replace("&gt;", ">");

        if (!tableMarkdownBuilder.toString().trim().isBlank()) {
            convertMarkdown += "\n---\n" + tableMarkdownBuilder.toString().trim();
        }

        convertMarkdown = convertMarkdown
                .replace(NEW_LINE_PREFIX, "\n")
                .replace(MARKDOWN_NEW_LINE_PREFIX, " ");

        return normalize(convertMarkdown);
    }

    /**
     * HTML 표 데이터 마크 다운 변환 재귀 함수
     */
    private static void convertTableHtmlToMarkdown(Node node, StringBuilder tableMarkdownBuilder, int depth) {
        // 표안의 표 처리
        for (Node child : node.childNodes()) {
            if (child instanceof Element el) {
                String tag = el.tagName();

                if ("table".equals(tag)) {
                    convertTableHtmlToMarkdown(child, tableMarkdownBuilder, depth + 1);
                } else {
                    convertTableHtmlToMarkdown(child, tableMarkdownBuilder, depth);
                }
            }
        }

        if (node instanceof Element el) {
            String tag = el.tagName();

            if ("table".equals(tag)) {
                String html = node.toString().replace(NEW_LINE_PREFIX, "\n");

                if (depth == 0) {
                    node.replaceWith(new TextNode(convertTableHtmlToMarkdownOneDepth(html)));
                } else if (depth > 0) {
                    String tableId = StringUtil.generateRandomId();
                    // 표 마크 다운 내부 개행 기호 => MARKDOWN__NEW_LINE_PREFIX
                    node.replaceWith(new TextNode(String.format("[#%d](#%s)%s", depth, tableId, MARKDOWN_NEW_LINE_PREFIX)));
                    tableMarkdownBuilder
                            .insert(0, NEW_LINE_PREFIX)
                            .insert(0, convertTableHtmlToMarkdownOneDepth(html))
                            .insert(0, String.format("# %s%s", tableId, NEW_LINE_PREFIX));
                }
            }
        }
    }

    /**
     * 표 데이터 HTML 마크 다운 1Depth 변환 재귀 함수
     *
     * @param html HTML 문자열
     * @return 마크 다운 문자열
     */
    private static String convertTableHtmlToMarkdownOneDepth(String html) {
        Document doc = Jsoup.parse(html);
        Element table = doc.selectFirst("table");
        if (table == null) return "";

        List<List<String>> grid = new ArrayList<>();
        Elements rows = table.select("tr");

        for (int r = 0; r < rows.size(); r++) {
            Element tr = rows.get(r);
            // 현재 row 준비
            while (grid.size() <= r) {
                grid.add(new ArrayList<>());
            }

            Elements cells = tr.select("th, td");
            int cIdx = 0;

            for (Element cell : cells) {
                // 이미 채워진 칸 건너뛰기
                while (cIdx < grid.get(r).size() && grid.get(r).get(cIdx) != null) {
                    cIdx++;
                }

                String rowspanStr = cell.attr("rowspan");
                String colspanStr = cell.attr("colspan");

                int rowspan = cell.hasAttr("rowspan") ? Integer.parseInt(rowspanStr.isBlank() ? "1" : rowspanStr) : 1;
                int colspan = cell.hasAttr("colspan") ? Integer.parseInt(colspanStr.isBlank() ? "1" : colspanStr) : 1;

                String text = cell.text().trim().replaceAll("\\s+", " ");

                // 필요한 만큼 row 확보
                while (grid.size() < r + rowspan) {
                    grid.add(new ArrayList<>());
                }

                // 각 row에 충분한 열 확보
                for (int rr = r; rr < r + rowspan; rr++) {
                    List<String> row = grid.get(rr);
                    while (row.size() < cIdx + colspan) {
                        row.add(null);
                    }
                }

                // 병합된 범위 모두 같은 값으로 채우기
                for (int rr = r; rr < r + rowspan; rr++) {
                    for (int cc = cIdx; cc < cIdx + colspan; cc++) {
                        grid.get(rr).set(cc, text);
                    }
                }

                cIdx += colspan;
            }
        }

        // 최대 열 수 맞추기
        int maxCols = grid.stream().mapToInt(List::size).max().orElse(0);
        for (List<String> row : grid) {
            while (row.size() < maxCols) {
                row.add("");
            }
        }

        // Markdown 문자열 만들기
        StringBuilder sb = new StringBuilder();
        if (!grid.isEmpty()) {
            // 헤더
            sb.append("|");
            sb.append(String.join("|", grid.getFirst()));
            sb.append("|");
            sb.append("\n");

            // 구분선
            sb.append("|");
            for (int i = 0; i < maxCols; i++) {
                sb.append("---");
                if (i < maxCols - 1) sb.append("|");
            }
            sb.append("|");
            sb.append("\n");

            // 나머지 행
            for (int i = 1; i < grid.size(); i++) {
                sb.append("|");
                sb.append(String.join("|", grid.get(i)));
                sb.append("|");
                sb.append("\n");
            }
        }

        return sb.toString().trim().replace("\n", NEW_LINE_PREFIX);
    }

    /**
     * HTML 태그 삭제
     *
     * @param html HTML 문자열
     * @return HTML 태그 삭제 문자열
     */
    public static String removeHtml(String html) {
        // 개형 변환
        String convertNewLineHtml = html.replace("\n", NEW_LINE_PREFIX);

        // table 태그 표 마크 다운 문자열 변환
        Document doc = Jsoup.parse(convertNewLineHtml);

        StringBuilder stringBuilder = new StringBuilder();
        // HTML 태그 삭제
        removeHtml(doc.body(), stringBuilder);

        // 표 마크 다운 셀 내부 개행 처리
        return normalize(stringBuilder.toString().replace(NEW_LINE_PREFIX, "\n"));
    }

    /**
     * HTML 태그 삭제 재귀 함수
     */
    private static void removeHtml(Node node, StringBuilder stringBuilder) {
        for (Node child : node.childNodes()) {
            // 텍스트 노드의 경우
            if (child instanceof TextNode textNode) {
                // 개행만 있는 경우
                if (NEW_LINE_PREFIX.equals(textNode.text().trim())) continue;

                stringBuilder.append(" ").append(textNode.text().trim());
            }
            // 텍스트 노드 외
            else if (child instanceof Element el) {
                String tag = el.tagName().toLowerCase();
                removeHtml(el, stringBuilder);
                if (NEW_LINE_TAGS.contains(tag)) {
                    stringBuilder.append(NEW_LINE_PREFIX);
                }
            }
        }
    }

    /**
     * HTML 태그 삭제 (표 HTML 보존)
     *
     * @param html HTML 문자열
     * @return HTML 태그 삭제 문자열
     */
    public static String removeHtmlExceptTable(String html) {
        return removeHtmlExceptTable(html, 0);
    }

    /**
     * HTML 태그 삭제 (표 HTML 보존)
     *
     * @param html            HTML 문자열
     * @param startTableDepth 표 보존 깊이
     * @return HTML 태그 삭제 문자열
     */
    public static String removeHtmlExceptTable(String html, int startTableDepth) {
        // 개형 변환
        String convertNewLineHtml = html.replace("\n", NEW_LINE_PREFIX);

        // table 태그 표 마크 다운 문자열 변환
        Document doc = Jsoup.parse(convertNewLineHtml);

        StringBuilder stringBuilder = new StringBuilder();
        // HTML 태그 삭제
        removeHtmlExceptTable(doc.body(), stringBuilder, startTableDepth, 0);

        // 표 마크 다운 셀 내부 개행 처리
        return normalize(stringBuilder.toString().replace(NEW_LINE_PREFIX, "\n"));
    }

    /**
     * HTML 태그 삭제 (표 HTML 보존) 재귀 함수
     */
    private static void removeHtmlExceptTable(Node node, StringBuilder stringBuilder, int startTableDepth, int depth) {
        for (Node child : node.childNodes()) {
            // 텍스트 노드의 경우
            if (child instanceof TextNode textNode) {
                // 개행만 있는 경우
                if (NEW_LINE_PREFIX.equals(textNode.text().trim())) continue;

                stringBuilder.append(textNode.text().trim());

                // 테이블 태그 하위 텍스트 노드의 아닌 경우
                if (child.parent() instanceof Element el) {
                    String tag = el.tagName().toLowerCase();
                    if (!TABLE_CHILD_TAGS.contains(tag)) {
                        stringBuilder.append(NEW_LINE_PREFIX);
                    }
                }
            }
            // 텍스트 노드 외
            else if (child instanceof Element el) {
                String tag = el.tagName().toLowerCase();

                // 테이블 태그인 경우
                if ("table".equals(tag)) {
                    int nextDepth = depth + 1;

                    // startTableDepth 이상 table은 HTML 유지
                    if (nextDepth > startTableDepth) {
                        stringBuilder.append("<table>");
                        removeHtmlExceptTable(el, stringBuilder, startTableDepth, nextDepth);
                        stringBuilder.append("</table>");
                    }
                    // startTableDepth 이전 table 은 삭제
                    else {
                        removeHtmlExceptTable(el, stringBuilder, startTableDepth, nextDepth);
                    }
                }
                // 테이블 하위 태그인 경우
                else if (TABLE_CHILD_TAGS.contains(tag) && depth > startTableDepth) {
                    // table 구조 태그 유지 + 속성 포함
                    stringBuilder.append("<").append(tag);
                    // 속성 보존 (colspan, rowspan)
                    for (Attribute attr : el.attributes()) {
                        if (Set.of("colspan", "rowspan").contains(attr.getKey())) {
                            stringBuilder.append(" ")
                                    .append(attr.getKey())
                                    .append("=\"").append(attr.getValue()).append("\"");
                        }
                    }
                    stringBuilder.append(">");
                    removeHtmlExceptTable(el, stringBuilder, startTableDepth, depth);
                    if (!SINGLE_TAGS.contains(tag)) stringBuilder.append("</").append(tag).append(">");
                }
                // 그 외 태그인 경우
                else {
                    removeHtmlExceptTable(el, stringBuilder, startTableDepth, depth);
                    if (NEW_LINE_TAGS.contains(tag)) {
                        stringBuilder.append(NEW_LINE_PREFIX);
                    }
                }
            }
        }
    }

    /**
     * HTML 태그 치환
     *
     * @param s 원본 문자열
     * @return HTML 태그 치환 문자열
     */
    private static String escapeHtml(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * 공백/개행 정리
     *
     * @param str 원본 문자열
     * @return 공백 정리 문자열
     */
    private static String normalize(String str) {
        str = str.replaceAll("[ \\t\\f\\r]+", " ");   // 연속 공백 → 하나
        str = str.replaceAll(" *\\n+ *", "\n");       // 개행 여러 개 → 하나
        return str.trim();
    }
}
