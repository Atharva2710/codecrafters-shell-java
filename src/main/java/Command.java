import java.util.ArrayList;
import java.util.List;

public class Command {
    private final String name;
    private final List<String> args;

    public Command(String name, List<String> args) {
        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public List<String> getArgs() {
        return args;
    }

    /**
     * Parse raw line input into Command.
     * Simple split by spaces for now, keeping it open to advanced parsing (quotes, backslashes) later.
     */
    public static Command parse(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return null;
        }

        // Split by whitespace, ignoring empty elements
        String[] parts = rawInput.trim().split("\\s+");
        if (parts.length == 0) {
            return null;
        }

        String name = parts[0];
        List<String> args = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            args.add(parts[i]);
        }

        return new Command(name, args);
    }
}
