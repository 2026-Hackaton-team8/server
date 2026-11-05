package org.study.jibeo.domain.equipmentmodel.service;

import org.study.jibeo.domain.equipmentmodel.model.Manual;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 매뉴얼 전문(마크다운)을 항목으로 나누고, 항목마다 줄 머리표를 칸으로 읽는다.
 *
 * <pre>
 * ## 고장: 냉각수가 약하게 나와요      ← 제목 하나가 항목 하나
 * 유형: 고장                          ← 개요·운전·점검·정비·고장·안전 (없으면 제목으로 판단)
 * 오류 코드: AL-151, AL-152           ← 쉼표로 나눔
 * 증상:                               ← 한 줄로 쓰면 1개, 아래에 - 또는 1. 목록이면 여러 개
 * - 노즐에서 냉각수가 줄줄 흐름
 * 가능한 원인: …   조치: …   주의: …  ← 목록 칸
 * 담당자 호출: …   근거: …             ← 글 칸 (빈 줄 전까지 이어 씀)
 * 그 밖의 줄                           ← 설명(description)
 * </pre>
 *
 * 줄 머리표가 하나도 없는 긴 항목은 문단 단위로 나눈다 (AI 는 항목당 12,000자까지).
 */
public final class ManualParser {

    public static final int MAX_SECTIONS = 200;
    static final int MAX_CHUNK_CHARS = 6000;
    static final int MAX_CONTENT_CHARS = 12_000;
    static final int MAX_ITEMS = 30;
    static final int MAX_ITEM_CHARS = 1500;
    private static final int MAX_TITLE_CHARS = 200;

    private static final Pattern HEADING = Pattern.compile("^ {0,3}#{1,6}\\s+(.+?)\\s*#*\\s*$");
    private static final Pattern FENCE = Pattern.compile("^ {0,3}(```|~~~)");
    private static final Pattern LABEL = Pattern.compile(
            "^\\s*(유형|오류\\s*코드|알람\\s*코드|증상|가능한\\s*원인|원인|조치|담당자\\s*호출|호출\\s*기준|주의|근거)\\s*:\\s*(.*)$");
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*(?:[-*•]|\\d+[.)])\\s+(.+)$");
    private static final Pattern SAFETY_TITLE = Pattern.compile(
            "안전\\s*(수칙|주의|경고|사항|지침|정보|안내|유의\\s*사항)|유의\\s*사항|주의\\s*사항|경고");

    private static final Map<String, Manual.SectionType> TYPE_WORDS = Map.of(
            "개요", Manual.SectionType.OVERVIEW,
            "운전", Manual.SectionType.OPERATION,
            "점검", Manual.SectionType.OPERATION,
            "정비", Manual.SectionType.MAINTENANCE,
            "고장", Manual.SectionType.TROUBLESHOOTING,
            "안전", Manual.SectionType.SAFETY);

    private ManualParser() {
    }

    /** 항목 하나. content 는 제목 아래 원문 그대로 (AI 가 인용·검색에 쓴다) */
    public record Section(String key, String title, Manual.SectionType type, List<String> errorCodes,
                          List<String> symptoms, List<String> causes, List<String> steps, String escalation,
                          List<String> cautions, String source, String description, String content) {

        /** AI 가 읽는 jsonb 항목. 담당자 호출 기준은 작업자에게 보이도록 주의사항 끝에 붙인다 */
        public Manual.Section toAi() {
            List<String> aiCautions = new ArrayList<>(cautions);
            if (escalation != null) {
                aiCautions.add("담당자 호출: " + escalation);
            }
            List<String> aiSymptoms = new ArrayList<>(errorCodes);
            aiSymptoms.addAll(symptoms);
            return new Manual.Section(key, title, type, aiSymptoms, List.of(), content, steps, aiCautions);
        }
    }

    /** 규칙에 맞지 않으면 IllegalArgumentException (메시지는 그대로 사용자에게 보여 준다) */
    public static List<Section> parse(String text, String fallbackTitle) {
        List<Section> sections = new ArrayList<>();
        for (Block block : blocks(text.replace("\r\n", "\n").replace('\r', '\n'), fallbackTitle)) {
            if (hasLabel(block.body())) {
                sections.add(structured(sections.size() + 1, block));
                continue;
            }
            List<String> chunks = chunks(block.body());
            for (int i = 0; i < chunks.size(); i++) {
                String title = chunks.size() == 1 ? block.title() : block.title() + " (" + (i + 1) + ")";
                sections.add(new Section(key(sections.size() + 1), truncate(title), typeByTitle(block.title()),
                        List.of(), List.of(), List.of(), List.of(), null, List.of(), null, chunks.get(i),
                        chunks.get(i)));
            }
        }
        if (sections.size() > MAX_SECTIONS) {
            throw new IllegalArgumentException("매뉴얼 항목이 %d개로 너무 많습니다. 제목을 %d개 이하로 줄여 주세요."
                    .formatted(sections.size(), MAX_SECTIONS));
        }
        return sections;
    }

    // ---------- 항목 하나 읽기 ----------

    private enum Field {
        TYPE, ERROR_CODES, SYMPTOMS, CAUSES, STEPS, ESCALATION, CAUTIONS, SOURCE;

        static Field of(String label) {
            return switch (label.replaceAll("\\s+", "")) {
                case "유형" -> TYPE;
                case "오류코드", "알람코드" -> ERROR_CODES;
                case "증상" -> SYMPTOMS;
                case "가능한원인", "원인" -> CAUSES;
                case "조치" -> STEPS;
                case "담당자호출", "호출기준" -> ESCALATION;
                case "주의" -> CAUTIONS;
                default -> SOURCE;
            };
        }

        boolean isText() {
            return this == ESCALATION || this == SOURCE || this == TYPE;
        }
    }

    private static Section structured(int index, Block block) {
        Map<Field, List<String>> values = new java.util.EnumMap<>(Field.class);
        StringBuilder description = new StringBuilder();
        Field current = null;
        for (String line : block.body().split("\n")) {
            Matcher label = LABEL.matcher(line);
            Matcher item = LIST_ITEM.matcher(line);
            if (label.matches()) {
                current = Field.of(label.group(1));
                String inline = label.group(2).strip();
                List<String> list = values.computeIfAbsent(current, f -> new ArrayList<>());
                if (current == Field.ERROR_CODES) {
                    Arrays.stream(inline.split("[,，]")).map(String::strip).filter(s -> !s.isEmpty()).forEach(list::add);
                } else if (!inline.isEmpty()) {
                    list.add(inline);
                }
                if (current == Field.TYPE) {
                    current = null;   // 유형은 그 줄만
                }
            } else if (line.isBlank()) {
                if (current != null && current.isText()) {
                    current = null;
                }
            } else if (current != null && item.matches() && !current.isText()) {
                values.get(current).add(item.group(1).strip());
            } else if (current != null && current.isText()) {
                values.get(current).add(line.strip());
            } else {
                current = null;
                description.append(line).append('\n');
            }
        }

        String content = block.body();
        if (content.length() > MAX_CONTENT_CHARS) {
            throw new IllegalArgumentException("항목 '%s'가 %d자로 너무 깁니다. %d자 이하로 나눠 주세요."
                    .formatted(block.title(), content.length(), MAX_CONTENT_CHARS));
        }
        Manual.SectionType type = values.containsKey(Field.TYPE) ? typeByWord(block.title(), text(values, Field.TYPE))
                : typeByTitle(block.title());
        return new Section(key(index), truncate(block.title()), type,
                list(block, values, Field.ERROR_CODES), list(block, values, Field.SYMPTOMS),
                list(block, values, Field.CAUSES), list(block, values, Field.STEPS), text(values, Field.ESCALATION),
                list(block, values, Field.CAUTIONS), text(values, Field.SOURCE),
                description.isEmpty() ? null : description.toString().strip(), content);
    }

    private static List<String> list(Block block, Map<Field, List<String>> values, Field field) {
        List<String> list = values.getOrDefault(field, List.of());
        if (list.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("항목 '%s'의 목록이 %d개로 너무 많습니다. %d개 이하로 줄여 주세요."
                    .formatted(block.title(), list.size(), MAX_ITEMS));
        }
        for (String item : list) {
            if (item.length() > MAX_ITEM_CHARS) {
                throw new IllegalArgumentException("항목 '%s'에 %d자를 넘는 줄이 있습니다."
                        .formatted(block.title(), MAX_ITEM_CHARS));
            }
        }
        return List.copyOf(list);
    }

    private static String text(Map<Field, List<String>> values, Field field) {
        List<String> lines = values.get(field);
        return lines == null || lines.isEmpty() ? null : String.join(" ", lines);
    }

    private static Manual.SectionType typeByWord(String title, String word) {
        Manual.SectionType type = TYPE_WORDS.get(word.strip());
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("항목 '%s'의 유형 '%s'를 알 수 없습니다. 개요·운전·점검·정비·고장·안전 중 하나로 써 주세요."
                .formatted(title, word));
    }

    private static Manual.SectionType typeByTitle(String title) {
        return SAFETY_TITLE.matcher(title).find() ? Manual.SectionType.SAFETY : Manual.SectionType.TROUBLESHOOTING;
    }

    private static boolean hasLabel(String body) {
        for (String line : body.split("\n")) {
            if (LABEL.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }

    // ---------- 제목으로 나누기 ----------

    private record Block(String title, String body) {
    }

    private static List<Block> blocks(String text, String fallbackTitle) {
        List<Block> blocks = new ArrayList<>();
        String title = null;
        StringBuilder body = new StringBuilder();
        boolean inFence = false;
        boolean anyHeading = false;
        for (String line : text.split("\n", -1)) {
            if (FENCE.matcher(line).find()) {
                inFence = !inFence;
            }
            Matcher heading = inFence ? null : HEADING.matcher(line);
            if (heading != null && heading.matches()) {
                add(blocks, title == null ? "개요" : title, body);
                title = heading.group(1).trim();
                body.setLength(0);
                anyHeading = true;
            } else {
                body.append(line).append('\n');
            }
        }
        add(blocks, title == null ? (anyHeading ? "개요" : fallbackTitle) : title, body);
        return blocks;
    }

    private static void add(List<Block> blocks, String title, StringBuilder body) {
        String content = body.toString().strip();
        if (!content.isEmpty()) {
            blocks.add(new Block(title, content));
        }
    }

    /** 문단(빈 줄) 경계에서 MAX_CHUNK_CHARS 이하로 묶는다. 한 문단이 너무 길면 줄, 그래도 길면 글자 수로 자른다 */
    static List<String> chunks(String body) {
        if (body.length() <= MAX_CHUNK_CHARS) {
            return List.of(body);
        }
        List<String> pieces = new ArrayList<>();
        for (String paragraph : body.split("\n\\s*\n")) {
            if (paragraph.length() <= MAX_CHUNK_CHARS) {
                pieces.add(paragraph.strip());
                continue;
            }
            for (String line : paragraph.split("\n")) {
                for (int i = 0; i < line.length(); i += MAX_CHUNK_CHARS) {
                    pieces.add(line.substring(i, Math.min(line.length(), i + MAX_CHUNK_CHARS)).strip());
                }
            }
        }
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String piece : pieces) {
            if (piece.isEmpty()) {
                continue;
            }
            if (!current.isEmpty() && current.length() + 2 + piece.length() > MAX_CHUNK_CHARS) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(piece);
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private static String key(int index) {
        return "s-%03d".formatted(index);
    }

    private static String truncate(String title) {
        return title.length() <= MAX_TITLE_CHARS ? title : title.substring(0, MAX_TITLE_CHARS);
    }
}
