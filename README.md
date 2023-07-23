# Operating System Simulator
Simulates a Process Scheduler and Memory Management Unit on top of a simulated CPU's instruction set.

Includes a GUI written using the OpenJFX library.

Sample input programs are in the `input` folder, with the expected output in <code>sampleoutput</code>.

Use `bash gradlew run` (Linux) or `./gradlew.bat run` (Windows) to launch the simulator. Java 11 or later is required, versions other than 11 are untested.

# Usage
## Booting
The interface looks like this:

![image](https://user-images.githubusercontent.com/20211754/117718535-1b0dbb80-b1d4-11eb-97b7-19404983f280.png)

To start the simulator, the following fields must be filled out:
- Page size – The size (in blocks) of each page in memory. One block represents one physical address that can be used. There is no limit imposed on the size of data stored at a single address.
- Page number – The number of pages to create. The actual size of the physical memory that will be created is calculated as `page size * page number`, as allowing for a memory size that is not divisible by the page size would result in wasted leftover memory space
- Memory clock – The number of operations the MMU should perform per second.
- Quantum – The number of scheduler cycles before switching process.
- Scheduler clock – The number of operations the scheduler should perform per second.
- CPU clock – The number of instructions the CPU will execute per second

The clock speeds may be decimal values, however all other fields must be integers.

Once the fields have been filled out, pressing Boot will start the simulator with those parameters. If you want to change them, press Shutdown, enter the new values, and then press Boot again.

## Input Files:
The left box is where input files can be added. The simulator comes with some test files in the `input` folder, as well as some tests that will cause errors in the `input/illegal` folder. Pressing Add will open a window allowing one or more files to be selected.

![image](https://user-images.githubusercontent.com/20211754/117718724-5b6d3980-b1d4-11eb-8cba-6bd8f7418acc.png)

To remove a file, click on it a press Remove. You can select multiple using SHIFT + Left Click.

Once you are happy with the selected files, press Execute to pass them to the simulator. Additional input files may be added at any time while the simulator is running.

Attempting to execute files before booting the simulator will place them on hold, and they will execute once the simulator has been booted.

## Output Logs:
While running, the simulator will look something like this:

![image](https://user-images.githubusercontent.com/20211754/117718766-6f18a000-b1d4-11eb-9d42-00fa8e9192e1.png)

There are three main output logs:
- Mailbox – This is where you can see the internal commands being passed between subsystems, in the format `[Sender => Receiver] command | args`. Commands closer to the top will be executed first. Where the sender or receiver is a number, this is the process id (PID) of the process requesting or receiving data from the MMU.
- Execution trace – This is a log of all instructions executed by the CPU. The number at the start of each line is the PID the instruction belongs to. Where the number is followed by `/DATA`, this means that the instruction was executed using data retrieved from memory.
- Output – This is the output log, any events or errors in the subsystems are logged here.

Pressing the Autoscroll button will jump to the end of these logs and enable them to scroll automatically as they update. Manually scrolling to a different point in a log will disable its autoscroll until the button is pressed again.

## Input File Format:
An input file consists of a list of instructions that the CPU will execute. Each instruction should be on its own line. Empty lines are allowed, however they will use up a CPU cycle.

Each instruction may be prefixed with `label:` to define a label at that point in the program. For example, `loop:set count 0` defines `loop` as a label at this instruction. Labels are used as an argument to `jump` and `jumpif` instructions.

## Instruction set:
Arguments in `[]` are required, and arguments in `{}` are optional:

### `null`
Do nothing. Blank lines in the input file are treated as this.

### `alloc [blocks]`
Allocate `blocks` blocks to this process if available.

### `free [blocks]`
Free `blocks` blocks from this process. Take care not to free more memory than you explicitly allocate, or you could start freeing memory that contains instructions.

### `var [name] [address] {value}`
Define the variable `name`, pointing to `address`. Addresses may be any integer >= 0, however ensure to allocate enough memory beforehand to prevent errors. Optionally, `value` can be provided, which will initialise the variable with this value. You are automatically prevented from defining variables at addresses containing instructions, as the given address will be translated to one past the end of the instructions. Be aware that although a variable can be set to a string or Boolean value, string values may not contain spaces.

### `set [var] [value]`
Set variable `var` to `value`. `value` can be another variable.

### `inc [var]`
Increment variable `var`.

### `dec [var]`
Decrement variable `var`.

### `jump [label]`
Jump to `label` unconditionally.

### `jumpif [var1] [comparator] [var2] [label]`
Jump to `label` if the specified condition is true. `var1` must be a defined variable, `var2` can either be a defined variable or any other value that is not a variable name. `comparator` can be any of the following: ==, !=, >, <, >=, <=. Only == and != can be used if comparing string or Boolean values.

### `math [expression]`
Evaluate an equation of format `target = equation` where `target` is the variable to write the result to. The equation can be composed of both numbers and defined variables (which will be substituted in). Brackets are supported and will be evaluated first. The supported operations are +, -, *, /, %. Order of operations is not supported and operations are performed from right to left, so `x + 3 * 5` will give the same result as `(x + 3) * 5`. Brackets should be used where a specific order of operations is required. 

An example equation in this format is:

`math a = (x/y) - 5 + ((10*z)%(x+y))`

which evaluates to `-2.5999999999999996` when x=2, y=5 and z=10.

### `out [var]`
Write the value of variable `var` to the output log. Additionally, a file will be created at `output/filename.txt` where `filename` is the name of the input file, containing all this process’ output. This file will not be saved until `exit` is called. If a file of that name already exists, `(n)` will be appended to the end of the file name, with `n` incrementing until a unique file name is found.

### `exit`
End the process, drop it from the simulator. All processes must end with this instruction.
