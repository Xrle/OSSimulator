package com.cd00827.OSSimulator;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.util.*;

public class CPU implements Runnable{
    private PCB process;
    private Scheduler scheduler;
    private double clockSpeed;
    private Mailbox mailbox;
    private ObservableList<String> trace;
    private ObservableList<String> output;
    private Deque<String> dataBuffer;
    private Map<Integer, String> instructionCache;
    private Map<Integer, Map<String, Integer>> varCache;
    private Map<Integer, Map<String, Integer>> labelCache;
    private List<String> mathVars;

    public CPU(Scheduler scheduler, Mailbox mailbox, double clockSpeed, ObservableList<String> trace, ObservableList<String> output) {
        this.scheduler = scheduler;
        this.mailbox = mailbox;
        this.clockSpeed = clockSpeed;
        this.trace = trace;
        this.output = output;
        this.process = null;
        this.dataBuffer = new ArrayDeque<>();
        this.instructionCache = new HashMap<>();
        this.varCache = new HashMap<>();
        this.labelCache = new HashMap<>();
    }

    private void output(String message) {
        Platform.runLater(() -> this.output.add(message));
    }
    private void log(String message) {
        Platform.runLater(() -> this.trace.add(message));
    }


    @Override
    public void run() {
        while (true) {
            //Remove data for any dropped processes
            {
                boolean done = false;
                while (!done) {
                    Message message = this.mailbox.get(Mailbox.CPU);
                    if (message != null) {
                        String[] command = message.getCommand();
                        if (command[0].equals("drop")) {
                            int pid = Integer.parseInt(command[1]);
                            this.instructionCache.remove(pid);
                            this.varCache.remove(pid);
                            this.labelCache.remove(pid);
                            this.output("[CPU] Dropped PID " + pid);
                        }
                    }
                    else{
                        done = true;
                    }
                }
            }

            //Get a reference to running process
            this.process = this.scheduler.getRunning();
            if (this.process != null) {
                int pid = this.process.getPid();
                this.dataBuffer.clear();

                //If there is no instruction cached, try and pull one from the mailbox, otherwise request a new one
                if (!this.instructionCache.containsKey(pid)) {
                    Message message = this.mailbox.get(String.valueOf(pid));
                    if (message == null) {
                        this.mailbox.put(String.valueOf(pid), Mailbox.MMU, "read|" + pid + "|" + this.process.pc + "|true");
                        this.block();
                    }
                    else {
                        this.instructionCache.put(pid, message.getCommand()[1]);
                    }
                }

                //Check that the process wasn't just blocked
                if (this.process != null) {
                    //Load requested data into buffer
                    boolean done = false;
                    while(!done) {
                        Message message = this.mailbox.get(String.valueOf(pid));
                        if (message != null) {
                            String[] command = message.getCommand();
                            if (command[0].equals("data")) {
                                this.dataBuffer.add(command[1]);
                                if (command[2].equals("true")) {
                                    done = true;
                                }
                            }
                        }
                        else {
                            done = true;
                        }
                    }

                    //Split label from instruction
                    String instruction;
                    String[] split = this.instructionCache.get(pid).split(":", 2);
                    if (!this.labelCache.containsKey(pid)) {
                        this.labelCache.put(pid, new HashMap<>());
                    }
                    try {
                        //Will throw exception if there is no label on this line
                        instruction = split[1];
                        this.labelCache.get(pid).put(split[0], this.process.pc);
                    }
                    catch (ArrayIndexOutOfBoundsException e) {
                        instruction = split[0];
                    }

                    //If data was provided execute instruction with it. If not, execution will determine the data needed
                    if (this.dataBuffer.isEmpty()) {
                        this.exec(instruction);
                        this.log("[" + pid + "/NODATA] " + instruction);
                    }
                    else {
                        this.execData(instruction, this.dataBuffer);
                        this.log("[" + pid + "] " + instruction);
                    }
                }
            }

            //Wait for next clock cycle
            try {
                Thread.sleep((long) (clockSpeed * 1000));
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Convert an address referenced by a process to the actual virtual address of that data
     * @param address Address as seen by the process
     * @return The corresponding virtual address
     */
    private int getRealAddress(int address) {
        return address + this.process.getCodeLength();
    }

    private void block() {
        this.scheduler.block(this.process);
        this.process = null;
    }

    private void drop() {
        this.mailbox.put(Mailbox.CPU, Mailbox.SCHEDULER, "drop|" + this.process.getPid());
        this.block();
    }

    /**
     * Execute an instruction.<br>
     * This method takes no data, so this is either for instructions that don't need data, or for determining what data is required.
     * @param instruction Instruction to execute
     */
    private void exec(String instruction) {
        try {
            int pid = this.process.getPid();
            String[] tokens = instruction.split("\\s");
            switch (tokens[0]) {
                //var [name] [address] {value}
                case "var": {
                    //Create a varCache entry for this process
                    if (!this.varCache.containsKey(pid)) {
                        this.varCache.put(pid, new HashMap<>());
                    }
                    this.varCache.get(pid).put(tokens[1], this.getRealAddress(Integer.parseInt(tokens[2])));
                    //Optionally assign a value to the variable
                    if (tokens.length == 4) {
                        this.mailbox.put(Mailbox.CPU, Mailbox.MMU, "write|" + pid + "|" + this.varCache.get(pid).get(tokens[1]) +  "|" + tokens[3] + "|true");
                        this.instructionCache.remove(pid);
                        this.process.pc++;
                        this.block();
                    }
                    else {
                        this.instructionCache.remove(pid);
                        this.process.pc++;
                    }
                }
                break;

                //alloc [blocks]
                case "alloc": {
                    this.mailbox.put(Mailbox.CPU, Mailbox.MMU, "allocate|" + pid + "|" + tokens[1] + "|false");
                    this.instructionCache.remove(pid);
                    this.process.pc++;
                    this.block();
                }
                break;

                //free [blocks]
                case "free": {
                    this.mailbox.put(Mailbox.CPU, Mailbox.MMU, "free|" + pid + "|" + tokens[1] + "|false");
                    this.instructionCache.remove(pid);
                    this.process.pc++;
                }
                break;

                //exit
                case "exit": {
                    this.drop();
                }
                break;

                //jump [label]
                case "jump": {
                    if (this.labelCache.containsKey(pid)) {
                        if (this.labelCache.get(pid).containsKey(tokens[1])) {
                            this.process.pc = this.labelCache.get(pid).get(tokens[1]);
                            this.instructionCache.remove(pid);
                        }
                        else {
                            throw new IllegalArgumentException("Label not defined");
                        }
                    }
                    else {
                        throw new IllegalArgumentException("Label not defined");
                    }
                }
                break;

                //set [var] [value]
                case "set": {
                    if (this.varCache.containsKey(pid)) {
                        if (this.varCache.get(pid).containsKey(tokens[1])) {
                            this.mailbox.put(Mailbox.CPU, Mailbox.MMU, "write|" + pid + "|" + this.varCache.get(pid).get(tokens[1]) +  "|" + tokens[2] + "|true");
                            this.instructionCache.remove(pid);
                            this.process.pc++;
                            this.block();
                        }
                        else {
                            throw new IllegalArgumentException("Variable not defined");
                        }
                    }
                    else {
                        throw new IllegalArgumentException("Variable not defined");
                    }
                }
                break;

                //out [var]
                case "out": {
                    if (this.varCache.containsKey(pid)) {
                        if (this.varCache.get(pid).containsKey(tokens[1])) {
                            this.mailbox.put(String.valueOf(pid), Mailbox.MMU, "read|" + pid + "|" + this.varCache.get(pid).get(tokens[1]) +  "|true");
                            this.block();
                        }
                        else {
                            throw new IllegalArgumentException("Variable not defined");
                        }
                    }
                    else {
                        throw new IllegalArgumentException("Variable not defined");
                    }
                }
                break;

                //math [expression]
                case "math": {
                    this.mathVars = new ArrayList<>();

                    //Merge tokens back into one string
                    StringBuilder builder = new StringBuilder();
                    for (int i = 1; i < tokens.length; i++) {
                        builder.append(tokens[i]);
                    }
                    String expression = builder.toString().replaceAll("\\s", "");

                    //Split at brackets and operators
                    String[] split = expression.split("[()+\\-*/%=]");

                    //Find variables, start at index 1 as index 0 will be the variable to output to
                    for (int i = 1; i < split.length; i++) {
                        if (this.varCache.get(pid).containsKey(split[i])) {
                            this.mathVars.add(split[i]);
                        }
                    }

                    //Request data
                    for (int i = 0; i < this.mathVars.size(); i++) {
                        if (i == this.mathVars.size() - 1) {
                            this.mailbox.put(String.valueOf(pid), Mailbox.MMU, "read|" + pid + "|" + this.varCache.get(pid).get(this.mathVars.get(i)) +  "|true");
                        }
                        else {
                            this.mailbox.put(String.valueOf(pid), Mailbox.MMU, "read|" + pid + "|" + this.varCache.get(pid).get(this.mathVars.get(i)) +  "|false");
                        }
                    }
                    this.block();
                }
                break;

                default: {
                    throw new IllegalArgumentException("Invalid instruction");
                }
            }
        }
        catch (Exception e) {
            //Output exception caused by process and drop it
            this.output("[CPU/ERROR] " + e.getClass().getSimpleName() + " in PID " + this.process.getPid() + " at '" + instruction + "': " + e.getMessage());
            this.drop();
        }
    }

    /**
     * Execute an instruction with data.<br>
     * The code to run once an instruction has requested the required data goes here.
     * @param instruction Instruction to execute
     * @param data Data to use in the execution
     */
    private void execData(String instruction, Deque<String> data) {
        try {
            int pid = this.process.getPid();
            String[] tokens = instruction.split("\\s");
            switch (tokens[0]) {
                //out [var]
                case "out": {
                    this.output("[" + pid + "] " + data.poll());
                    this.instructionCache.clear();
                    this.process.pc++;
                }
                break;

                case "math": {
                    //Merge tokens back into one string
                    StringBuilder builder = new StringBuilder();
                    for (int i = 1; i < tokens.length; i++) {
                        builder.append(tokens[i]);
                    }
                    String expression = builder.toString().replaceAll("\\s", "");
                    String target;
                    {
                        String[] split = expression.split("=");
                        target = split[0];
                        expression = split[1];
                    }

                    //Sub in data
                    for (String var : this.mathVars) {
                        expression = expression.replaceAll(var, Objects.requireNonNull(this.dataBuffer.poll()));
                    }

                    //Add brackets to list in order they must be evaluated in - inner brackets followed by outer brackets
                    List<String> operations = new ArrayList<>();
                    {
                        boolean done = false;
                        while (!done) {
                            //Find the first closing bracket
                            int close = expression.indexOf(")");
                            int open;
                            boolean found = false;
                            int i = 1;
                            //Go backwards to find the opening bracket
                            while (!found) {
                                if (expression.substring(close - i, (close - i) + 1).equals("(")) {
                                    open = close - i;
                                    //Add contents of brackets to list
                                    operations.add(expression.substring(open + 1, close));
                                    //Replace bracket with b:n where n is the index of the bracket in the list
                                    expression = expression.replace(expression.substring(open, close + 1), "b:"+ (operations.size() - 1));
                                    found = true;
                                }
                                i++;
                            }
                            //If there are no more brackets, break loop
                            if (!expression.contains("(")) {
                                done = true;
                            }
                        }
                    }

                    //Add expression as final operation
                    operations.add(expression);

                    //Evaluate
                    for (int i = 0; i < operations.size(); i++) {
                        //Extract operators
                        Deque<String> operators = new ArrayDeque<>();
                        for (String s : operations.get(i).split("[^+\\-*/%]")) {
                            if (!s.equals("")) {
                                operators.add(s);
                            }
                        }

                        //Split at operators
                        String[] split = operations.get(i).split("[+\\-*/%]");
                        //Sub in previous operations
                        for (int j = 0; j < split.length; j++) {
                            if (split[j].matches("b:[0-9]+")) {
                                split[j] = operations.get(Integer.parseInt(split[j].split(":")[1]));
                            }
                        }

                        //Calculate result
                        double result = Double.parseDouble(split[0]);
                        for (int j = 1; j < split.length; j++) {
                            switch (Objects.requireNonNull(operators.poll())) {
                                case "+":
                                    result = result + Double.parseDouble(split[j]);
                                    break;

                                case "-":
                                    result = result - Double.parseDouble(split[j]);
                                    break;

                                case "*":
                                    result = result * Double.parseDouble(split[j]);
                                    break;

                                case "/":
                                    result = result / Double.parseDouble(split[j]);
                                    break;

                                case "%":
                                    result = result % Double.parseDouble(split[j]);
                                    break;
                            }
                        }
                        operations.set(i, String.valueOf(result));
                    }

                    //Write result to target
                    this.mailbox.put(Mailbox.CPU, Mailbox.MMU, "write|" + pid + "|" + this.varCache.get(pid).get(target) +  "|" + operations.get(operations.size() - 1) + "|true");
                    this.instructionCache.remove(pid);
                    this.process.pc++;
                    this.block();
                }
                break;
            }
        }
        catch (Exception e) {
            //Output exception caused by process and drop it
            this.output("[CPU/ERROR] " + e.getClass().getSimpleName() + " in PID " + this.process.getPid() + " at '" + instruction + "': " + e.getMessage());
            this.drop();
        }
    }
}
