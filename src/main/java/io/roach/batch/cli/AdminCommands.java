package io.roach.batch.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Quit;

@ShellComponent
@ShellCommandGroup("Admin Commands")
public class AdminCommands implements Quit.Command {
    @Autowired
    private ConfigurableApplicationContext applicationContext;

//    @Autowired
//    private Console console;

    @ShellMethod(value = "Exit the shell", key = {"q", "quit", "exit"})
    public void quit() {
        applicationContext.close();
        throw new ExitRequest();
    }
}