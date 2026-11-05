package org.study.jibeo.domain.equipmentmodel.service;

import org.study.jibeo.domain.equipmentmodel.model.Manual;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 매뉴얼 전문(마크다운)을 AI 가 검색하는 항목 목록으로 나눈다.
 * <ul>
 *   <li>제목(# ~ ######) 하나가 항목 하나. 첫 제목 앞의 글은 "개요" 항목</li>
 *   <li>제목에 안전 수칙·주의사항·경고가 들어가면 SAFETY (AI 가 모든 해결책의 주의사항에 덧붙임), 그 밖은 TROUBLESHOOTING (상담 검색 대상)</li>
 *   <li>본문이 없는 제목은 건너뛰고, 본문이 길면 문단 단위로 나눈다 (AI 는 항목당 12,000자까지)</li>
 *   <li>제목이 하나도 없으면 문단을 묶어 덩어리로 나눈다</li>
 * </ul>
 */
public final class ManualSplitter {

    public static final int MAX_SECTIONS = 200;
    static final int MAX_CHUNK_CHARS = 6000;
    private static final int MAX_TITLE_CHARS = 200;

    private static final Pattern HEADING = Pattern.compile("^ {0,3}#{1,6}\\s+(.+?)\\s*#*\\s*$");
    private static final Pattern FENCE = Pattern.compile("^ {0,3}(```|~~~)");
    private static final Pattern SAFETY_TITLE = Pattern.compile("안전\\s*(수칙|주의|경고|사항|지침|정보)|주의\\s*사항|경고");

    private ManualSplitter() {
    }

    /** 항목이 200개를 넘으면 IllegalArgumentException */
    public static Manual split(String text, int version, String fallbackTitle) {
        List<Block> blocks = blocks(text.replace("\r\n", "\n").replace('\r', '\n'), fallbackTitle);
        List<Manual.Section> sections = new ArrayList<>();
        for (Block block : blocks) {
            List<String> chunks = chunks(block.body());
            for (int i = 0; i < chunks.size(); i++) {
                String title = chunks.size() == 1 ? block.title() : block.title() + " (" + (i + 1) + ")";
                sections.add(new Manual.Section("s-%03d".formatted(sections.size() + 1), truncate(title),
                        type(block.title()), List.of(), List.of(), chunks.get(i), List.of(), List.of()));
            }
        }
        if (sections.size() > MAX_SECTIONS) {
            throw new IllegalArgumentException("매뉴얼 항목이 %d개로 너무 많습니다. 제목을 %d개 이하로 줄여 주세요."
                    .formatted(sections.size(), MAX_SECTIONS));
        }
        return new Manual(version, sections);
    }

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

    private static Manual.SectionType type(String title) {
        return SAFETY_TITLE.matcher(title).find() ? Manual.SectionType.SAFETY : Manual.SectionType.TROUBLESHOOTING;
    }

    private static String truncate(String title) {
        return title.length() <= MAX_TITLE_CHARS ? title : title.substring(0, MAX_TITLE_CHARS);
    }
}
