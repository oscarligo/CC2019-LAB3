import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class SyntaxTree {
    public static Node fromPostfix(List<String> postfix) {
        Deque<Node> nodes = new ArrayDeque<>();

        for (String token : postfix) {
            if (token.equals("*")) {
                nodes.push(new Node(token, pop(nodes, token), null));
            } else if (token.equals("+")) {
                Node operand = pop(nodes, token);
                nodes.push(new Node("·", operand, new Node("*", copy(operand), null)));
            } else if (token.equals("?")) {
                nodes.push(new Node("|", pop(nodes, token), new Node("ε")));
            } else if (token.equals("|") || token.equals("·") || token.equals("^")) {
                Node right = pop(nodes, token);
                Node left = pop(nodes, token);
                nodes.push(new Node(token, left, right));
            } else {
                nodes.push(new Node(token));
            }
        }

        if (nodes.size() != 1) {
            throw new IllegalArgumentException("Invalid postfix expression");
        }
        return nodes.pop();
    }

    public static Path writeGraphviz(Node root, Path file) throws IOException {
        Path output = file.toAbsolutePath();
        Files.createDirectories(output.getParent());

        StringBuilder dot = new StringBuilder(
                "digraph SyntaxTree {\n"
                + "  rankdir=TB;\n"
                + "  node [shape=circle, fontname=Helvetica];\n");
        appendNode(root, dot, new int[] { 0 });
        dot.append("}\n");

        Files.write(output, dot.toString().getBytes(StandardCharsets.UTF_8));
        return output;
    }

    public static void renderPng(Path dot, Path png) throws IOException {
        Process process = new ProcessBuilder(
                "dot", "-Tpng", dot.toString(), "-o", png.toString())
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        try {
            if (process.waitFor() != 0) {
                throw new IOException("Graphviz could not render " + dot);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Graphviz rendering was interrupted", e);
        }
    }

    private static Node pop(Deque<Node> nodes, String operator) {
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Operator " + operator + " does not have enough operands");
        }
        return nodes.pop();
    }

    private static Node copy(Node node) {
        if (node == null) {
            return null;
        }
        return new Node(node.getValue(), copy(node.getLeft()), copy(node.getRight()));
    }

    private static int appendNode(Node node, StringBuilder dot, int[] nextId) {
        int id = nextId[0]++;
        dot.append("  n").append(id).append(" [label=\"")
                .append(escape(node.getValue())).append("\"];\n");

        if (node.getLeft() != null) {
            int child = appendNode(node.getLeft(), dot, nextId);
            dot.append("  n").append(id).append(" -> n").append(child).append(";\n");
        }
        if (node.getRight() != null) {
            int child = appendNode(node.getRight(), dot, nextId);
            dot.append("  n").append(id).append(" -> n").append(child).append(";\n");
        }
        return id;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static void selfTest() {
        Node plus = fromPostfix(Arrays.asList("a", "+"));
        Node optional = fromPostfix(Arrays.asList("b", "?"));

        if (!plus.getValue().equals("·")
                || !plus.getRight().getValue().equals("*")
                || plus.getLeft() == plus.getRight().getLeft()
                || !optional.getValue().equals("|")
                || !optional.getRight().getValue().equals("ε")) {
            throw new AssertionError("Syntax tree simplification failed");
        }
        System.out.println("Test passed: postfix tree and +/? simplifications");
    }
}
