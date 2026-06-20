import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static int nextJobNumber = 1;

    private static class Job {
        int jobNum;
        long pid;
        String command;
        String status;
        Process process;

        public Job(int jobNum, long pid, String command, String status, Process process) {
            this.jobNum = jobNum;
            this.pid = pid;
            this.command = command;
            this.status = status;
            this.process = process;
        }
    }

    private static final List<Job> backgroundJobs = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // Track the current working directory for the Navigation module
        String currentDirectory = System.getProperty("user.dir");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            reapCompletedJobs();
            System.out.print("$ ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }

            // ==========================================
            // MODULE: Quoting & Parsing
            // ==========================================
            // Parse the command line string, respecting single/double quotes and backslashes
            List<String> parsedArgs = parseCommandLine(input);
            if (parsedArgs.isEmpty()) {
                continue;
            }

            // Check if the command should run in the background (ends with &)
            boolean runInBackground = false;
            if (parsedArgs.get(parsedArgs.size() - 1).equals("&")) {
                runInBackground = true;
                parsedArgs.remove(parsedArgs.size() - 1);
            }
            if (parsedArgs.isEmpty()) {
                continue;
            }

            // ==========================================
            // MODULE: Redirection Parsing
            // ==========================================
            String stdoutRedirectFile = null;
            boolean stdoutAppend = false;
            String stderrRedirectFile = null;
            boolean stderrAppend = false;

            // Search for redirection operators in the arguments
            for (int i = 0; i < parsedArgs.size(); i++) {
                String arg = parsedArgs.get(i);
                
                // --- Redirection: Standard Output (stdout) ---
                if (arg.equals(">") || arg.equals("1>")) {
                    if (i + 1 < parsedArgs.size()) {
                        stdoutRedirectFile = parsedArgs.get(i + 1);
                        stdoutAppend = false;
                        parsedArgs.remove(i + 1);
                        parsedArgs.remove(i);
                        i--; // Adjust index after removal
                    }
                } 
                
                // --- Redirection: Append Standard Output (stdout >>) ---
                else if (arg.equals(">>") || arg.equals("1>>")) {
                    if (i + 1 < parsedArgs.size()) {
                        stdoutRedirectFile = parsedArgs.get(i + 1);
                        stdoutAppend = true;
                        parsedArgs.remove(i + 1);
                        parsedArgs.remove(i);
                        i--;
                    }
                } 
                
                // --- Redirection: Standard Error (stderr 2>) ---
                else if (arg.equals("2>")) {
                    if (i + 1 < parsedArgs.size()) {
                        stderrRedirectFile = parsedArgs.get(i + 1);
                        stderrAppend = false;
                        parsedArgs.remove(i + 1);
                        parsedArgs.remove(i);
                        i--;
                    }
                } 
                
                // --- Redirection: Append Standard Error (stderr 2>>) ---
                else if (arg.equals("2>>")) {
                    if (i + 1 < parsedArgs.size()) {
                        stderrRedirectFile = parsedArgs.get(i + 1);
                        stderrAppend = true;
                        parsedArgs.remove(i + 1);
                        parsedArgs.remove(i);
                        i--;
                    }
                }
            }

            if (parsedArgs.isEmpty()) {
                continue;
            }
            String command = parsedArgs.get(0);

            // ==========================================
            // MODULE: Redirection Implementation (Builtins)
            // ==========================================
            // Save original standard streams so we can restore them afterwards
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            PrintStream redirectedOut = null;
            PrintStream redirectedErr = null;

            try {
                // Apply stdout redirection if requested
                if (stdoutRedirectFile != null) {
                    File file = new File(stdoutRedirectFile);
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs(); // Ensure parent directories exist
                    }
                    redirectedOut = new PrintStream(new FileOutputStream(file, stdoutAppend));
                    System.setOut(redirectedOut);
                }

                // Apply stderr redirection if requested
                if (stderrRedirectFile != null) {
                    File file = new File(stderrRedirectFile);
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs(); // Ensure parent directories exist
                    }
                    redirectedErr = new PrintStream(new FileOutputStream(file, stderrAppend));
                    System.setErr(redirectedErr);
                }

                // ==========================================
                // MODULE: Base Shell Stages
                // ==========================================

                if (command.equals("exit")) {
                    int exitCode = 0;
                    if (parsedArgs.size() > 1) {
                        try {
                            exitCode = Integer.parseInt(parsedArgs.get(1));
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                    System.exit(exitCode);
                } 
                
                else if (command.equals("echo")) {
                    // Echo all parsed arguments separated by a single space
                    List<String> echoArgs = parsedArgs.subList(1, parsedArgs.size());
                    System.out.println(String.join(" ", echoArgs));
                } 
                
                else if (command.equals("type")) {
                    if (parsedArgs.size() > 1) {
                        String target = parsedArgs.get(1);
                        if (isBuiltin(target)) {
                            System.out.println(target + " is a shell builtin");
                        } else {
                            // Check if command is a valid executable in the system PATH
                            String path = getPathOfExecutable(target);
                            if (path != null) {
                                System.out.println(target + " is " + path);
                            } else {
                                System.err.println(target + ": not found");
                            }
                        }
                    }
                } 

                // ==========================================
                // MODULE: Navigation Extension
                // ==========================================

                else if (command.equals("pwd")) {
                    System.out.println(currentDirectory);
                } 
                
                else if (command.equals("cd")) {
                    String targetDir = "~";
                    if (parsedArgs.size() > 1) {
                        targetDir = parsedArgs.get(1);
                    }

                    File dir;
                    if (targetDir.startsWith("/")) {
                        dir = new File(targetDir);
                    } else if (targetDir.startsWith("~")) {
                        String home = System.getenv("HOME");
                        if (home == null) {
                            home = System.getProperty("user.home");
                        }
                        String path = targetDir.replaceFirst("^~", home);
                        dir = new File(path);
                    } else {
                        dir = new File(currentDirectory, targetDir);
                    }

                    if (dir.exists() && dir.isDirectory()) {
                        try {
                            currentDirectory = dir.getCanonicalPath();
                        } catch (Exception e) {
                            currentDirectory = dir.getAbsolutePath();
                        }
                    } else {
                        System.err.println("cd: " + targetDir + ": No such file or directory");
                    }
                } 
                
                else if (command.equals("jobs")) {
                    List<Job> toRemove = new ArrayList<>();
                    for (int i = 0; i < backgroundJobs.size(); i++) {
                        Job job = backgroundJobs.get(i);
                        
                        // Check if the process exited since the last check
                        if (job.status.equals("Running") && !job.process.isAlive()) {
                            job.status = "Done";
                            toRemove.add(job);
                        }

                        String marker = " ";
                        if (i == backgroundJobs.size() - 1) {
                            marker = "+";
                        } else if (i == backgroundJobs.size() - 2) {
                            marker = "-";
                        }

                        String printCmd = job.command;
                        if (job.status.equals("Done")) {
                            if (printCmd.endsWith("&")) {
                                printCmd = printCmd.substring(0, printCmd.length() - 1).trim();
                            }
                        }

                        String formattedStatus = String.format("%-24s", job.status);
                        System.out.println("[" + job.jobNum + "]" + marker + "  " + formattedStatus + printCmd);
                    }
                    backgroundJobs.removeAll(toRemove);
                } 

                // ==========================================
                // MODULE: Base Shell Stages (External Commands Execution)
                // ==========================================

                else {
                    // Determine if the command is an executable in PATH or direct file path
                    String path = null;
                    if (command.contains("/") || command.contains(File.separator)) {
                        File file = new File(command);
                        if (file.exists() && file.isFile() && file.canExecute()) {
                            path = file.getAbsolutePath();
                        }
                    } else {
                        path = getPathOfExecutable(command);
                    }

                    if (path != null) {
                        try {
                            // Spawn external process with arguments
                            ProcessBuilder pb = new ProcessBuilder(parsedArgs);
                            pb.directory(new File(currentDirectory));
                            
                            // Apply output redirection to the child process.
                            // If no redirection file is specified, the child (including background jobs)
                            // inherits the parent shell's standard output to print output directly to the terminal.
                            if (stdoutRedirectFile != null) {
                                File outFile = new File(stdoutRedirectFile);
                                if (stdoutAppend) {
                                    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outFile));
                                } else {
                                    pb.redirectOutput(ProcessBuilder.Redirect.to(outFile));
                                }
                            } else {
                                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                            }

                            // Apply error redirection to the child process.
                            // If no redirection file is specified, the child inherits the parent shell's standard error.
                            if (stderrRedirectFile != null) {
                                File errFile = new File(stderrRedirectFile);
                                if (stderrAppend) {
                                    pb.redirectError(ProcessBuilder.Redirect.appendTo(errFile));
                                } else {
                                    pb.redirectError(ProcessBuilder.Redirect.to(errFile));
                                }
                            } else {
                                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                            }

                            // Inherit input stream by default
                            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);

                            Process process = pb.start();
                            if (runInBackground) {
                                backgroundJobs.add(new Job(nextJobNumber, process.pid(), input, "Running", process));
                                System.out.println("[" + nextJobNumber + "] " + process.pid());
                                nextJobNumber++;
                            } else {
                                process.waitFor();
                            }
                        } catch (Exception e) {
                            System.err.println(command + ": command not found");
                        }
                    } else {
                        System.err.println(command + ": command not found");
                    }
                }
            } catch (Exception e) {
                System.err.println("shell: " + e.getMessage());
            } finally {
                // Restore original standard output and error streams
                if (redirectedOut != null) {
                    redirectedOut.close();
                    System.setOut(originalOut);
                }
                if (redirectedErr != null) {
                    redirectedErr.close();
                    System.setErr(originalErr);
                }
            }
        }
    }

    private static void reapCompletedJobs() {
        List<Job> toRemove = new ArrayList<>();
        for (int i = 0; i < backgroundJobs.size(); i++) {
            Job job = backgroundJobs.get(i);
            
            // Check if the process exited since the last check
            if (job.status.equals("Running") && !job.process.isAlive()) {
                job.status = "Done";
                toRemove.add(job);

                String marker = " ";
                if (i == backgroundJobs.size() - 1) {
                    marker = "+";
                } else if (i == backgroundJobs.size() - 2) {
                    marker = "-";
                }

                String printCmd = job.command;
                if (printCmd.endsWith("&")) {
                    printCmd = printCmd.substring(0, printCmd.length() - 1).trim();
                }

                String formattedStatus = String.format("%-24s", job.status);
                System.out.println("[" + job.jobNum + "]" + marker + "  " + formattedStatus + printCmd);
            }
        }
        backgroundJobs.removeAll(toRemove);
    }

    private static boolean isBuiltin(String command) {
        return command.equals("exit") || command.equals("echo") || command.equals("type") || command.equals("pwd") || command.equals("cd") || command.equals("jobs");
    }

    // Helper method for resolving system PATH executables
    private static String getPathOfExecutable(String command) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return null;
        }
        String[] directories = pathEnv.split(File.pathSeparator);
        for (String dir : directories) {
            File file = new File(dir, command);
            if (file.exists() && file.isFile() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    // ==========================================
    // MODULE: Quoting & Parsing Helpers
    // ==========================================
    private static List<String> parseCommandLine(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean inArg = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inSingleQuotes) {
                if (c == '\'') {
                    inSingleQuotes = false;
                    inArg = true;
                } else {
                    currentArg.append(c);
                    inArg = true;
                }
            } else if (inDoubleQuotes) {
                if (c == '"') {
                    inDoubleQuotes = false;
                    inArg = true;
                } else if (c == '\\') {
                    if (i + 1 < input.length()) {
                        char nextChar = input.charAt(i + 1);
                        if (nextChar == '"' || nextChar == '\\' || nextChar == '$' || nextChar == '`') {
                            currentArg.append(nextChar);
                            i++; // Skip the escaped character
                        } else {
                            currentArg.append('\\');
                        }
                        inArg = true;
                    } else {
                        currentArg.append('\\');
                        inArg = true;
                    }
                } else {
                    currentArg.append(c);
                    inArg = true;
                }
            } else {
                if (c == '\\') {
                    if (i + 1 < input.length()) {
                        currentArg.append(input.charAt(i + 1));
                        i++; // Skip the escaped character
                        inArg = true;
                    }
                } else if (c == '\'') {
                    inSingleQuotes = true;
                    inArg = true;
                } else if (c == '"') {
                    inDoubleQuotes = true;
                    inArg = true;
                } else if (Character.isWhitespace(c)) {
                    if (inArg) {
                        args.add(currentArg.toString());
                        currentArg.setLength(0);
                        inArg = false;
                    }
                } else {
                    currentArg.append(c);
                    inArg = true;
                }
            }
        }
        if (inArg) {
            args.add(currentArg.toString());
        }
        return args;
    }
}
