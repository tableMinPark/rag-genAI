package com.genai.global.utils;

import java.util.*;

public class AhoCorasick {

    public static class AhoNode {
        Map<Character, AhoNode> children = new HashMap<>();
        AhoNode fail;
        List<String> outputs = new ArrayList<>();
    }

    private final AhoNode root = new AhoNode();

    public void insert(String word) {
        AhoNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new AhoNode());
        }
        node.outputs.add(word);
    }
    public void build() {
        Queue<AhoNode> queue = new LinkedList<>();

        root.fail = root;

        for (AhoNode child : root.children.values()) {
            child.fail = root;
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            AhoNode current = queue.poll();

            for (var entry : current.children.entrySet()) {
                char ch = entry.getKey();
                AhoNode child = entry.getValue();

                AhoNode failNode = current.fail;
                while (failNode != root && !failNode.children.containsKey(ch)) {
                    failNode = failNode.fail;
                }

                child.fail = failNode.children.getOrDefault(ch, root);

                child.outputs.addAll(child.fail.outputs);
                queue.add(child);
            }
        }
    }

    public Set<String> search(String text) {
        Set<String> result = new HashSet<>();
        AhoNode node = root;

        for (char c : text.toCharArray()) {
            while (node != root && !node.children.containsKey(c)) {
                node = node.fail;
            }
            node = node.children.getOrDefault(c, root);
            result.addAll(node.outputs);
        }
        return result;
    }
}
