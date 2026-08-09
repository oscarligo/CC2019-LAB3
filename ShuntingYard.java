import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Stack;

public class ShuntingYard {
    private ArrayList<ArrayList<String>> tokens;
    private ArrayList<ArrayList<String>> postfixExpressions;

    public ShuntingYard() {
        tokens = new ArrayList<>();
        postfixExpressions = new ArrayList<>();
    }

    public void tokenize(File file) throws IOException {
        tokens.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ArrayList<String> lineTokens = new ArrayList<>();
                for (char symbol : line.toCharArray()) {
                    if (!Character.isWhitespace(symbol)) {
                        lineTokens.add(String.valueOf(symbol));
                    }
                }
                if (!lineTokens.isEmpty()) {
                    tokens.add(lineTokens);
                }
            }
        }
    }

    public void processAllTokens(boolean trace) {
        postfixExpressions.clear();
        for (ArrayList<String> regex : tokens) {
            if (isBalanced(regex)) {
                try {
                    ArrayList<String> postfix = infixToPostfix(regex, trace);
                    postfixExpressions.add(postfix);
                } catch (IllegalArgumentException e) {
                    System.err.println("Error processing expression: " + e.getMessage());
                }
            } else {
                System.err.println("Skipped unbalanced expression: " + String.join("", regex));
            }
        }
    }

    public boolean isBalanced(ArrayList<String> lineTokens) {
        Stack<Character> stack = new Stack<>();
        String openings = "([{", closings = ")]}";

        for (String token : lineTokens) {
            char symbol = token.charAt(0);
            int openingIndex = openings.indexOf(symbol);
            int closingIndex = closings.indexOf(symbol);

            if (openingIndex >= 0) {
                stack.push(symbol);
                System.out.printf("  %-3s -> push       %s%n", symbol, stack);
            } else if (closingIndex >= 0) {
                if (stack.isEmpty() || stack.peek() != openings.charAt(closingIndex)) {
                    System.out.printf("  %-3s -> mismatch  %s%n", symbol, stack);
                    return false;
                }
                stack.pop();
                System.out.printf("  %-3s -> pop    %s%n", symbol, stack);
            }
        }

        if (!stack.isEmpty()) {
            System.out.println("  end -> stack is not empty: " + stack);
            return false;
        }
        return true;
    }

    public ArrayList<ArrayList<String>> getTokens() {
        return tokens;
    }

    public ArrayList<ArrayList<String>> getPostfixExpressions() {
        return postfixExpressions;
    }

    public int getPrecedence(String token) {
        if (token.equals("(")) {
            return 1;
        }
        if (token.equals("|")) {
            return 2;
        }
        if (token.equals("·")) {
            return 3;
        }
        if (token.equals("*") || token.equals("+") || token.equals("?")) {
            return 4;
        }
        if (token.equals("^")) {
            return 5;
        }
        return 0;
    }

    public ArrayList<String> formatRegEx(ArrayList<String> regex) {
        ArrayList<String> grouped = groupEscapedCharacters(regex);
        ArrayList<String> formatted = new ArrayList<>();

        for (int i = 0; i < grouped.size(); i++) {
            String current = grouped.get(i);
            formatted.add(current);
            if (i + 1 < grouped.size()
                    && canEndOperand(current)
                    && canStartOperand(grouped.get(i + 1))) {
                formatted.add("·");
            }
        }
        return formatted;
    }

    public ArrayList<String> infixToPostfix(ArrayList<String> regex) {
        return infixToPostfix(regex, true);
    }

    private ArrayList<String> infixToPostfix(ArrayList<String> regex, boolean trace) {
        ArrayList<String> postfix = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();
        ArrayList<String> formattedRegEx = formatRegEx(regex);
        boolean expectingOperand = true;

        if (formattedRegEx.isEmpty()) {
            throw new IllegalArgumentException("Empty expression");
        }
        if (trace) {
            System.out.println("Tokens: " + String.join(" ", formattedRegEx));
            System.out.printf("%-10s %-22s %-35s %s%n",
                    "Token", "Action", "Output", "Stack");
        }

        for (String token : formattedRegEx) {
            String action;
            if (isOperand(token)) {
                postfix.add(token);
                expectingOperand = false;
                action = "Send to output";
            } else if (token.equals("(")) {
                stack.addLast(token);
                expectingOperand = true;
                action = "Push to stack";
            } else if (token.equals(")")) {
                if (expectingOperand) {
                    throw new IllegalArgumentException("Empty parentheses or unexpected closing parenthesis");
                }
                int moved = 0;
                while (!stack.isEmpty() && !stack.peekLast().equals("(")) {
                    postfix.add(stack.removeLast());
                    moved++;
                }
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Closing parenthesis without opening parenthesis");
                }
                stack.removeLast();
                expectingOperand = false;
                action = "Close group (" + moved + " moved)";
            } else {
                if (expectingOperand) {
                    throw new IllegalArgumentException(
                            "Operator " + token + " without preceding expression");
                }
                int moved = 0;
                while (!stack.isEmpty()
                        && !stack.peekLast().equals("(")
                        && getPrecedence(stack.peekLast()) >= getPrecedence(token)) {
                    postfix.add(stack.removeLast());
                    moved++;
                }
                stack.addLast(token);
                expectingOperand = isBinaryOperator(token);
                action = moved == 0 ? "Push to stack" : "Move " + moved + " and push";
            }
            if (trace) {
                printStep(token, action, postfix, stack);
            }
        }

        if (expectingOperand) {
            throw new IllegalArgumentException("Expression ends with an operator");
        }
        while (!stack.isEmpty()) {
            if (stack.peekLast().equals("(")) {
                throw new IllegalArgumentException("Opening parenthesis without closing parenthesis");
            }
            postfix.add(stack.removeLast());
            if (trace) {
                printStep("End", "Empty stack", postfix, stack);
            }
        }

        return postfix;
    }

    private ArrayList<String> groupEscapedCharacters(ArrayList<String> regex) {
        ArrayList<String> grouped = new ArrayList<>();

        for (int i = 0; i < regex.size(); i++) {
            String token = regex.get(i);
            if (token.equals("\\")) {
                if (++i == regex.size()) {
                    throw new IllegalArgumentException("Escape character without symbol");
                }
                grouped.add("\\" + regex.get(i));
            } else if (token.equals("[")) {
                ArrayList<String> members = new ArrayList<>();
                boolean closed = false;
                while (++i < regex.size()) {
                    token = regex.get(i);
                    if (token.equals("\\")) {
                        if (++i == regex.size()) {
                            throw new IllegalArgumentException(
                                    "Incomplete escape character inside []");
                        }
                        members.add("\\" + regex.get(i));
                    } else if (token.equals("]")) {
                        closed = true;
                        break;
                    } else {
                        members.add(token);
                    }
                }
                if (!closed) {
                    throw new IllegalArgumentException("Unclosed character class");
                }
                if (members.isEmpty()) {
                    throw new IllegalArgumentException("Empty character class");
                }
                grouped.add("(");
                for (int member = 0; member < members.size(); member++) {
                    if (member > 0) {
                        grouped.add("|");
                    }
                    grouped.add(members.get(member));
                }
                grouped.add(")");
            } else {
                grouped.add(token);
            }
        }
        return grouped;
    }

    private boolean isOperand(String token) {
        return !token.equals("(")
                && !token.equals(")")
                && !isBinaryOperator(token)
                && !isPostfixOperator(token);
    }

    private boolean isBinaryOperator(String token) {
        return token.length() == 1 && (token.equals("|") || token.equals("·") || token.equals("^"));
    }

    private boolean isPostfixOperator(String token) {
        return token.length() == 1 && (token.equals("*") || token.equals("+") || token.equals("?"));
    }

    private boolean canEndOperand(String token) {
        return isOperand(token) || token.equals(")") || isPostfixOperator(token);
    }

    private boolean canStartOperand(String token) {
        return isOperand(token) || token.equals("(");
    }

    private void printStep(String token, String action, List<String> output,
            Deque<String> stack) {
        System.out.printf("%-10s %-22s %-35s %s%n",
                token, action, String.join(" ", output), stack);
    }

    public static void selfTest() {
        ArrayList<String> expression = new ArrayList<>();
        for (char symbol : "[ae]+".toCharArray()) {
            expression.add(String.valueOf(symbol));
        }
        String postfix = String.join("",
                new ShuntingYard().infixToPostfix(expression, false));
        if (!postfix.equals("ae|+")) {
            throw new AssertionError("Expected ae|+, got " + postfix);
        }
        System.out.println("Test passed: [ae]+ -> ae|+");
    }
}