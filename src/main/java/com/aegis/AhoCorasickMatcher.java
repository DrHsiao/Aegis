package com.aegis;

import java.util.*;

public class AhoCorasickMatcher {

    private static class Node {
        Map<Character, Node> children = new HashMap<>();
        Node fail;
        List<String> outputs = new ArrayList<>();
    }

    private final Node root;

    public AhoCorasickMatcher(Collection<String> patterns) {
        this.root = new Node();
        buildTrie(patterns);
        buildFailureLinks();
    }

    private void buildTrie(Collection<String> patterns) {
        for (String pattern : patterns) {
            Node curr = root;
            for (char c : pattern.toCharArray()) {
                curr = curr.children.computeIfAbsent(c, k -> new Node());
            }
            curr.outputs.add(pattern);
        }
    }

    private void buildFailureLinks() {
        Queue<Node> queue = new LinkedList<>();
        for (Node child : root.children.values()) {
            child.fail = root;
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            Node u = queue.poll();
            for (Map.Entry<Character, Node> entry : u.children.entrySet()) {
                char c = entry.getKey();
                Node v = entry.getValue();
                Node f = u.fail;
                while (f != null && !f.children.containsKey(c)) {
                    f = f.fail;
                }
                v.fail = (f == null) ? root : f.children.get(c);
                v.outputs.addAll(v.fail.outputs);
                queue.add(v);
            }
        }
    }

    public List<MatchResult> match(String text) {
        List<MatchResult> results = new ArrayList<>();
        Node curr = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (curr != root && !curr.children.containsKey(c)) {
                curr = curr.fail;
            }
            curr = curr.children.getOrDefault(c, root);
            for (String pattern : curr.outputs) {
                results.add(new MatchResult(i - pattern.length() + 1, pattern));
            }
        }
        return results;
    }

    public static class MatchResult {
        public final int index;
        public final String pattern;

        public MatchResult(int index, String pattern) {
            this.index = index;
            this.pattern = pattern;
        }

        @Override
        public String toString() {
            return "MatchResult{" + "index=" + index + ", pattern='" + pattern + '\'' + '}';
        }
    }
}
