import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Builtins {
    private static final Map<String, BiConsumer<Command, Shell>> REGISTRY = new HashMap<>();

    static {
        // Register exit command
        REGISTRY.put("exit", (command, shell) -> {
            int exitCode = 0;
            if (!command.getArgs().isEmpty()) {
                try {
                    exitCode = Integer.parseInt(command.getArgs().get(0));
                } catch (NumberFormatException e) {
                    // Ignore or default to 0
                }
            }
            shell.exit(exitCode);
        });

        // Register echo command
        REGISTRY.put("echo", (command, shell) -> {
            shell.getOut().println(String.join(" ", command.getArgs()));
        });
    }

    public static boolean isBuiltin(String name) {
        return REGISTRY.containsKey(name);
    }

    public static void execute(Command command, Shell shell) {
        BiConsumer<Command, Shell> action = REGISTRY.get(command.getName());
        if (action != null) {
            action.accept(command, shell);
        }
    }
}
