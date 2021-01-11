package io.roach.batch;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
import org.springframework.shell.jline.FileInputProvider;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.PromptProvider;

@Configuration
@SpringBootApplication
public class Application implements PromptProvider {
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

    @Autowired
    @Lazy
    private History history;

    @Autowired
    private ConfigurableEnvironment env;

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
        boolean onlineMode = Arrays.stream(env.getActiveProfiles())
                .anyMatch(p -> p.equals("proxy") || p.equals("online"));
        if (onlineMode) {
            return new AttributedString("online:$ ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        } else {
            return new AttributedString("offline:$ ", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        }
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return new ScriptShellApplicationRunner();
    }
}

@Order(InteractiveShellApplicationRunner.PRECEDENCE - 100)
class ScriptShellApplicationRunner implements ApplicationRunner {
    @Autowired
    private Shell shell;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<File> scriptsToRun = args.getNonOptionArgs().stream()
                .filter(s -> s.startsWith("@"))
                .map(s -> new File(s.substring(1)))
                .collect(Collectors.toList());

        if (!scriptsToRun.isEmpty()) {
            for (File file : scriptsToRun) {
                try (Reader reader = new FileReader(file);
                     FileInputProvider inputProvider = new FileInputProvider(reader, new DefaultParser())) {
                    shell.run(inputProvider);
                }
            }
        } else if (args.getSourceArgs().length > 0) {
            List<String> argsList = new ArrayList<>();

            Arrays.stream(args.getSourceArgs()).forEach(arg -> {
                String[] parts = arg.split(" ");
                argsList.addAll(Arrays.asList(parts));
            });

            List<String> stack = new ArrayList<>();
            List<List<String>> blocks = new ArrayList<>();

            argsList.forEach(arg -> {
                if ("&&".equals(arg) && !stack.isEmpty()) {
                    blocks.add(new ArrayList<>(stack));
                    stack.clear();
                } else {
                    stack.add(arg);
                }
            });

            if (!stack.isEmpty()) {
                blocks.add(stack);
            }

            for (List<String> command : blocks) {
                String input = String.join(" ", command);

                shell.run(new InputProvider() {
                    boolean done;

                    @Override
                    public Input readInput() {
                        if (!done) {
                            done = true;
                            return () -> input;
                        } else {
                            return null;
                        }
                    }
                });
            }
        }
    }
}

