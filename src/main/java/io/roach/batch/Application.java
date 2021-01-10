package io.roach.batch;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.util.StringUtils;

@Configuration
@SpringBootApplication
public class Application implements PromptProvider {
    @Autowired
    @Lazy
    private History history;

    @Autowired
    private Shell shell;

    @Autowired
    private ConfigurableEnvironment env;

    public static void main(String[] args) {
        List<String> argsList = new ArrayList<>(Arrays.asList(args));

        if (argsList.remove("--online") || argsList.remove("--proxy")) {
            // Skip embedded servlet container
            new SpringApplicationBuilder(Application.class)
                    .web(WebApplicationType.SERVLET)
                    .logStartupInfo(true)
                    .profiles("proxy")
                    .run(argsList.toArray(new String[] {}));
        } else {
            new SpringApplicationBuilder(Application.class)
                    .web(WebApplicationType.NONE)
                    .headless(true)
                    .logStartupInfo(true)
                    .run(argsList.toArray(new String[] {}));
        }
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return new ProvidedCommandLineRunner(shell);
    }

    @Bean
    @Lazy
    public History history(LineReader lineReader, @Value("${history.file}") String historyPath) {
        if (!"disabled".equals(historyPath)) {
            lineReader.setVariable(LineReader.HISTORY_FILE, Paths.get(historyPath));
        }
        return new DefaultHistory(lineReader);
    }

    @EventListener
    public void onContextClosedEvent(ContextClosedEvent event) throws IOException {
        history.save();
    }

    @Override
    public AttributedString getPrompt() {
        boolean offline = Arrays.stream(env.getActiveProfiles()).anyMatch(p -> p.equals("offline"));
        if (offline) {
            return new AttributedString("offline:$ ", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        } else {
            return new AttributedString("online:$ ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        }
    }
}

@Order(InteractiveShellApplicationRunner.PRECEDENCE - 2)
class ProvidedCommandLineRunner implements CommandLineRunner {
    private final Shell shell;

    public ProvidedCommandLineRunner(Shell shell) {
        this.shell = shell;
    }

    @Override
    public void run(String... args) throws Exception {
        List<String> argsList = new ArrayList<>();

        Arrays.stream(args).forEach(arg -> {
            String[] parts = arg.split(" ");
            argsList.addAll(Arrays.asList(parts));
        });

        List<String> block = new ArrayList<>();
        List<List<String>> commandBlocks = new ArrayList<>();

        argsList.forEach(arg -> {
            if ("&&".equals(arg) && !block.isEmpty()) {
                commandBlocks.add(new ArrayList<>(block));
                block.clear();
            } else if (!arg.startsWith("@")) {
                block.add(arg);
            }
        });

        if (!block.isEmpty()) {
            commandBlocks.add(block);
        }

        for (List<String> commandBlock : commandBlocks) {
            shell.run(new StringInputProvider(commandBlock));
        }
    }
}

class StringInputProvider implements InputProvider {
    private final List<String> words;

    private boolean done;

    public StringInputProvider(List<String> words) {
        this.words = words;
    }

    @Override
    public Input readInput() {
        if (!done) {
            done = true;
            return new Input() {
                @Override
                public List<String> words() {
                    return words;
                }

                @Override
                public String rawText() {
                    return StringUtils.collectionToDelimitedString(words, " ");
                }
            };
        } else {
            return null;
        }
    }
}
