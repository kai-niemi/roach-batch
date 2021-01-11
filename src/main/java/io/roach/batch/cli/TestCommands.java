package io.roach.batch.cli;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@ShellCommandGroup("Commands for Testing")
public class TestCommands {
    @Autowired
    private FlatFileCommands delegate;

    @ShellMethod(value = "Pipe a source file to a destination (TEST 0)")
    public void t0() throws IOException {
        delegate.flatPipe("classpath:samples/test_sm.txt", "(stdout)", false);
    }


    @ShellMethod(value = "Convert a flat file to CSV (TEST 1)")
    public void t1() throws IOException {
        delegate.flatToCSV("classpath:samples/test_sm.txt", "classpath:samples/test.json", 0, "(stdout)", 256, false);
    }

    @ShellMethod(value = "Convert a flat file to CSV (TEST 2)")
    public void t2() throws IOException {
        delegate.flatToCSV("classpath:samples/test_sm.txt", "classpath:samples/test.json", 0, "test.csv", 256, false);
    }


    @ShellMethod(value = "Convert a flat file to SQL inserts (TEST 3)")
    public void t3() throws IOException {
        delegate.flatToSQL("classpath:samples/test_sm.txt", "classpath:samples/test.json", 0, 256,
                "jdbc:postgresql://192.168.1.99:26300/roach_batch?sslmode=disable", "root", "", false);
    }

    @ShellMethod(value = "Convert a flat file to SQL inserts (TEST 4)")
    public void t4() throws IOException {
        delegate.flatToSQL("classpath:samples/employee.csv", "classpath:samples/employee.json", 0, 256,
                "jdbc:postgresql://192.168.1.99:26300/roach_batch?sslmode=disable", "root", "", false);
    }
}
