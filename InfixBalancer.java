import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

public class InfixBalancer {

    private ArrayList<ArrayList<String>> tokens;

    public InfixBalancer() {
        this.tokens = new ArrayList<>();
    }

    public void tokenize(File file) throws IOException {
        tokens.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                ArrayList<String> lineTokens = new ArrayList<>();
                for (char c : line.toCharArray()) {
                    if (!Character.isWhitespace(c)) {
                        lineTokens.add(Character.toString(c));
                    }
                }
                tokens.add(lineTokens);
            }
        }
    }

    public ArrayList<ArrayList<String>> getTokens() {
        return tokens;
    }

    public boolean isBalanced(ArrayList<String> lineTokens) {
        Deque<Character> stack = new ArrayDeque<>();
        String openings = "([{", closings = ")]}";

        for (String token : lineTokens) {
            char symbol = token.charAt(0);
            int openingIndex = openings.indexOf(symbol);
            int closingIndex = closings.indexOf(symbol);

            if (openingIndex >= 0) {
                stack.addLast(symbol);
                System.out.printf("  %-3s -> push       %s%n", symbol, stack);
            } else if (closingIndex >= 0) {
                if (stack.isEmpty() || stack.peekLast() != openings.charAt(closingIndex)) {
                    System.out.printf("  %-3s -> mismatch  %s%n", symbol, stack);
                    return false;
                }
                stack.removeLast();
                System.out.printf("  %-3s -> pop    %s%n", symbol, stack);
            }
        }

        if (!stack.isEmpty()) {
            System.out.println("  end -> stack is not empty: " + stack);
        }
        return stack.isEmpty();
    }

}
