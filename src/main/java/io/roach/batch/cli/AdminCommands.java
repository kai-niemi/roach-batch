package io.roach.batch.cli;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Quit;
import org.springframework.util.FileCopyUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

@ShellComponent
@ShellCommandGroup("Admin Commands")
public class AdminCommands implements Quit.Command {
    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private Console console;

    @ShellMethod(value = "Exit the shell", key = {"q", "quit", "exit"})
    public void quit() {
        applicationContext.close();
        throw new ExitRequest();
    }

    @ShellMethod(value = "Print application YAML config")
    public void printConfig() {
        Resource resource = applicationContext.getResource("classpath:application.yml");
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            System.out.println(FileCopyUtils.copyToString(reader));
            System.out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ShellMethod(value = "Print local system information")
    public void printSystemInfo() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        console.custom( Console.Color.WHITE,">> OS");
        console.info(" Arch: %s | OS: %s | Version: %s", os.getArch(), os.getName(), os.getVersion());
        console.info(" Available processors: %d", os.getAvailableProcessors());
        console.info(" Load avg: %f", os.getSystemLoadAverage());

        RuntimeMXBean r = ManagementFactory.getRuntimeMXBean();
        console.custom( Console.Color.WHITE,">> Runtime");
        console.info(" Uptime: %s", r.getUptime());
        console.info(" VM name: %s | Vendor: %s | Version: %s", r.getVmName(), r.getVmVendor(), r.getVmVersion());

        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        console.custom( Console.Color.WHITE,">> Runtime");
        console.info(" Peak threads: %d", t.getPeakThreadCount());
        console.info(" Thread #: %d", t.getThreadCount());
        console.info(" Total started threads: %d", t.getTotalStartedThreadCount());

        Arrays.stream(t.getAllThreadIds()).sequential().forEach(value -> {
            console.info(" Thread (%d): %s %s", value,
                    t.getThreadInfo(value).getThreadName(),
                    t.getThreadInfo(value).getThreadState().toString()
            );
        });

        MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        console.custom( Console.Color.WHITE,">> Memory");
        console.info(" Heap: %s", m.getHeapMemoryUsage().toString());
        console.info(" Non-heap: %s", m.getNonHeapMemoryUsage().toString());
        console.info(" Pending GC: %s", m.getObjectPendingFinalizationCount());
    }

}