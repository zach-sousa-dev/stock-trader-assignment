/**
 * I like to add colors to my console output to make it easier to read.
 * Concatenate these into a string to use the colors. Make sure to use 
 * RESET at the end so that your output does not remain that color until
 * the end of the program.
 * @author Zachary Sousa
 */
public class IOColors {
    //  colors
    public static final String BOLD_RED = "\u001B[1;31m";
    public static final String BOLD_GREEN = "\u001B[1;32m";
    public static final String BLUE = "\u001B[34m";

    // reset code
    public static final String RESET = "\u001B[0m";
}
