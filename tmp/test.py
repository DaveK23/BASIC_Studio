import subprocess

cp = ':'.join([
    'app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes',
    '/opt/gradle/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.1.10/d3028429e7151d7a7c1a0d63a44704336c537122/kotlin-stdlib-2.1.10.jar',
    '/opt/gradle/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.8.0/ac424f115ce92040053915f0139b8bd591d31068/kotlinx-coroutines-core-jvm-1.8.0.jar'
])

java_code = """
import com.example.basic.compiler.BasicCompiler;
import com.example.basic.model.BytecodeProgram;
import com.example.basic.model.Instruction;

public class TestBasic {
    public static void main(String[] args) throws Exception {
        BasicCompiler compiler = new BasicCompiler();
        BytecodeProgram program = compiler.compile("10 PRINT \\\"A\\\";\\n20 PRINT \\\"B\\\";\\n30 PRINT \\\"C\\\"");
        for (int i = 0; i < program.getInstructions().size(); i++) {
            Instruction inst = program.getInstructions().get(i);
            System.out.println(i + ": " + inst.getOp() + " (line " + inst.getSourceLineNum() + ")");
        }
    }
}
"""
with open('/tmp/TestBasic.java', 'w') as f:
    f.write(java_code)

res = subprocess.run(['javac', '-cp', cp, '/tmp/TestBasic.java'], capture_output=True, text=True)
if res.returncode != 0:
    print('javac error:', res.stderr)
else:
    run_res = subprocess.run(['java', '-cp', f'/tmp:{cp}', 'TestBasic'], capture_output=True, text=True)
    print(run_res.stdout)
