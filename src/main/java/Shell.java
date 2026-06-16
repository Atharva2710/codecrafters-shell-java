import java.io.File;
import java.io.PrintStream;
import java.util.Scanner;

public class Shell {
    private boolean running = true;
    private File currentDirectory;
    private final Scanner scanner;
    private final PrintStream out;
    private final PrintStream err;

    public Shell() {
        this.scanner = new Scanner(System.in);
        this.out = System.out;
        this.err = System.err;
        // Start in user's current directory
        this.currentDirectory = new File(System.getProperty("user.dir"));
    }

    public void start() {
        while (running) {
            out.print("$ ");
            out.flush();

            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine();
            if (input.trim().isEmpty()) {
                continue;
            }

            Command command = Command.parse(input);
            if (command != null) {
                execute(command);
            }
        }
        scanner.close();
    }

    public void exit(int statusCode) {
        this.running = false;
        System.exit(statusCode);
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(File directory) {
        this.currentDirectory = directory;
    }

    private void execute(Command command) {
        if (Builtins.isBuiltin(command.getName())) {
            Builtins.execute(command, this);
        } else {
            // Handle external command or print command not found
            executeExternalOrError(command);
        }
    }

    private void executeExternalOrError(Command command) {
        // For early stages, all non-builtins are treated as invalid
        out.println(command.getName() + ": command not found");
    }

    public PrintStream getOut() {
        return out;
    }

    public PrintStream getErr() {
        return err;
    }
}
