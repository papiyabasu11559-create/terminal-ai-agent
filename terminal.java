import com.google.genai.Chat;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import java.awt.Desktop;
import java.io.Console;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Scanner;

public class terminal {
    private static final String MODEL = "gemini-flash-latest";
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String GRAY = "\u001B[90m";

    public static void main(String[] args) {
        String apiKey = System.getenv("GOOGLE_API_KEY");

        try (Scanner input = new Scanner(System.in)) {
            if (apiKey == null || apiKey.isBlank()) {
                apiKey = promptForApiKey(input);
            }
            if (apiKey == null || apiKey.isBlank()) {
                System.err.println("No API key was provided. Gemini chat cannot start.");
                return;
            }

            try (Client client = Client.builder().apiKey(apiKey).build()) {
                Chat chat = client.chats.create(MODEL);
                printWelcome();

                while (input.hasNextLine()) {
                    System.out.print(GREEN + "You > " + RESET);
                    String prompt = input.nextLine();
                    String command = prompt.trim();
                    if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("/exit")) {
                        System.out.println("Goodbye!");
                        break;
                    }
                    if (prompt.isBlank()) {
                        System.out.println(GRAY + "Type a message or /help for commands." + RESET);
                        continue;
                    }
                    if (command.equalsIgnoreCase("/help")) {
                        printHelp();
                        continue;
                    }
                    if (command.equalsIgnoreCase("/clear")) {
                        clearScreen();
                        printWelcome();
                        continue;
                    }
                    if (command.equalsIgnoreCase("/new")) {
                        chat = client.chats.create(MODEL);
                        System.out.println(GRAY + "Started a new conversation." + RESET);
                        continue;
                    }
                    if (command.equalsIgnoreCase("/flow")) {
                        openGoogleFlowWithConfirmation(input);
                        continue;
                    }
                    if (command.equalsIgnoreCase("/youtuber")) {
                        runYoutuberMode(chat, input);
                        continue;
                    }

                    try {
                        System.out.println(CYAN + "Gemini > " + RESET + askGemini(chat, prompt));
                    } catch (RuntimeException e) {
                        System.err.println("Gemini request failed: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static String promptForApiKey(Scanner input) {
        System.out.println("Setup: enter your Google Gemini API key to use the AI.");
        System.out.println("The key will only be used for this run and will not be saved.");

        Console console = System.console();
        if (console != null) {
            char[] key = console.readPassword("Google API key: ");
            if (key == null) {
                return null;
            }
            try {
                return new String(key).trim();
            } finally {
                Arrays.fill(key, ' ');
            }
        }

        System.out.println("Input cannot be hidden in this terminal. Avoid entering the key on a shared screen.");
        System.out.print("Google API key: ");
        return input.hasNextLine() ? input.nextLine().trim() : null;
    }

    private static void printWelcome() {
        System.out.println(CYAN + "====================================");
        System.out.println("          GEMINI TERMINAL CHAT");
        System.out.println("====================================" + RESET);
        System.out.println(GRAY + "Chat naturally. Type /help to see commands." + RESET);
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  /new   Start a fresh conversation");
        System.out.println("  /clear Clear the terminal screen");
        System.out.println("  /flow  Ask before opening Google Flow");
        System.out.println("  /youtuber Create a video and image prompt package for Google Flow");
        System.out.println("  /help  Show this help");
        System.out.println("  exit   Quit the chat");
    }

    private static void runYoutuberMode(Chat chat, Scanner input) {
        System.out.println("Youtuber mode: describe the video you want to create.");
        System.out.print("Content idea > ");
        if (!input.hasNextLine()) {
            System.out.println("No idea entered. Leaving Youtuber mode.");
            return;
        }

        String idea = input.nextLine().trim();
        if (idea.isBlank()) {
            System.out.println("No idea entered. Leaving Youtuber mode.");
            return;
        }

        String task = """
                You are a video pre-production assistant. Use the user's idea below once to create a
                complete, practical production package for making a short video with Google Flow.
                Do not ask follow-up questions; make reasonable, clearly stated assumptions for
                missing details.

                Return:
                1. A concise concept, suggested title, target audience, aspect ratio, and approximate duration.
                2. A short narration or dialogue script, if suitable.
                3. A numbered scene-by-scene plan. For each scene include its duration, what happens,
                   a detailed still-image generation prompt, and a separate image-to-video generation
                   prompt describing subject movement, camera movement, and motion.
                4. Continuity notes to keep characters, clothing, setting, lighting, and visual style
                   consistent between generated assets.
                5. Suggested sound and editing notes.

                Make the image and video prompts ready to copy into Google Flow. Clearly label them.
                Do not claim to have generated images or video or to have operated Google Flow.

                User's video idea:
                """ + idea;

        System.out.println(CYAN + "\nPreparing your video package..." + RESET);
        try {
            System.out.println(CYAN + "\n" + askGemini(chat, task) + RESET);
            openGoogleFlowWithConfirmation(input);
        } catch (RuntimeException e) {
            System.err.println("Could not create the video package: " + e.getMessage());
        }
    }

    private static void clearScreen() {
        System.out.print("\u001B[2J\u001B[H");
        System.out.flush();
    }

    private static void openGoogleFlowWithConfirmation(Scanner input) {
        System.out.print("Open Google Flow in your browser? Type yes to confirm: ");
        if (!input.nextLine().trim().equalsIgnoreCase("yes")) {
            System.out.println("Cancelled. Google Flow was not opened.");
            return;
        }

        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            System.out.println("Opening a browser is not supported on this device.");
            return;
        }

        try {
            Desktop.getDesktop().browse(URI.create("https://labs.google/fx/tools/flow"));
            System.out.println("Google Flow opened in your browser.");
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            System.err.println("Could not open Google Flow: " + e.getMessage());
        }
    }

    private static String askGemini(Chat chat, String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be empty.");
        }

        GenerateContentResponse response = chat.sendMessage(prompt);
        String answer = response.text();
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("Gemini returned an empty response.");
        }
        return answer;
    }
}
