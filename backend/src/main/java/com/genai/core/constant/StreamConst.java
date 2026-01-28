package com.genai.core.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class StreamConst {

    public static final String CONNECT         = "connect";
    public static final String DISCONNECT      = "disconnect";
    public static final String EXCEPTION       = "exception";

    public static final List<Event> EVENT_STEP = Arrays.stream(Event.values())
            .sorted(Comparator.comparingInt(Event::getSort))
            .toList();

    @Getter
    @AllArgsConstructor
    public enum Event {
        INITIALIZE("initialize", 0),
        PREPARE("prepare", 1),
        INFERENCE("inference", 2),
        ANSWER("answer", 3),
        REFERENCE("reference", 4),
        ;

        public final String name;
        public final String start;
        public final String process;
        public final String done;
        public final int sort;

        Event(String process, int sort) {
            this.name = process;
            this.start = process + "-start";
            this.process = process;
            this.done = process + "-done";
            this.sort = sort;
        }
    }
}