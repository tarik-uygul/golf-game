
import java.util.Map;

public class ExpressionParser {

    public double evaluate(String equation, String[] names, 
                          double[] state, Map<String, Double> constants) {
        
        // step 1: replace constants
        for (Map.Entry<String, Double> entry : constants.entrySet()) {
            equation = equation.replace(entry.getKey(), 
                                       String.valueOf(entry.getValue()));
        }
        
        // step 2: replace variables
        for (int i = 0; i < names.length; i++) {
            equation = equation.replace(names[i], 
                                       String.valueOf(state[i]));
        }
        
        // step 3: remove all spaces
        equation = equation.replace(" ", "");
        
        // step 4: calculate the result
        return calculate(equation);
    }

    private double calculate(String equation) {
        // handle + and - last (lowest priority)
        // scan from right to left to handle left-to-right evaluation
        int depth = 0;
        for (int i = equation.length() - 1; i >= 0; i--) {
            char c = equation.charAt(i);
            if (c == '(') depth++;
            if (c == ')') depth--;
            if (depth == 0 && (c == '+' || c == '-') && i > 0) {
                double left = calculate(equation.substring(0, i));
                double right = calculate(equation.substring(i + 1));
                return c == '+' ? left + right : left - right;
            }
        }

        // handle * and / first (higher priority)
        depth = 0;
        for (int i = equation.length() - 1; i >= 0; i--) {
            char c = equation.charAt(i);
            if (c == '(') depth++;
            if (c == ')') depth--;
            if (depth == 0 && (c == '*' || c == '/') && i > 0) {
                double left = calculate(equation.substring(0, i));
                double right = calculate(equation.substring(i + 1));
                return c == '*' ? left * right : left / right;
            }
        }

        // no operators left - just a number
        return Double.parseDouble(equation);
    }
}