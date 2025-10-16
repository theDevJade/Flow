import com.flowlang.bindings.*;

public class TestReflectionManual {
    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;
        
        System.out.println("============================================================");
        System.out.println("Flow Java Reflection API Tests");
        System.out.println("============================================================\n");
        
        try (FlowRuntime runtime = new FlowRuntime()) {
            
            // Test 1: Function count
            try {
                String code = """
                    func add(a: int, b: int) -> int {
                        return a + b;
                    }
                    func subtract(x: int, y: int) -> int {
                        return x - y;
                    }
                    func multiply(m: int, n: int) -> int {
                        return m * n;
                    }
                    """;
                
                FlowModule module = runtime.compile(code, "test1");
                int count = module.getFunctionCount();
                
                if (count == 3) {
                    System.out.println("✓ test_function_count");
                    passed++;
                } else {
                    System.out.println("✗ test_function_count - expected 3, got " + count);
                    failed++;
                }
                module.close();
            } catch (Exception e) {
                System.out.println("✗ test_function_count - " + e.getMessage());
                failed++;
            }
            
            // Test 2: List functions
            try {
                String code = """
                    func greet(name: string) -> string {
                        return "Hello, " + name;
                    }
                    func square(x: int) -> int {
                        return x * x;
                    }
                    """;
                
                FlowModule module = runtime.compile(code, "test2");
                String[] functions = module.listFunctions();
                
                boolean ok = functions.length == 2;
                for (String f : functions) {
                    if (!f.equals("greet") && !f.equals("square")) {
                        ok = false;
                    }
                }
                
                if (ok) {
                    System.out.println("✓ test_list_functions");
                    passed++;
                } else {
                    System.out.println("✗ test_list_functions");
                    failed++;
                }
                module.close();
            } catch (Exception e) {
                System.out.println("✗ test_list_functions - " + e.getMessage());
                failed++;
            }
            
            // Test 3: Function info with parameters
            try {
                String code = """
                    func add(a: int, b: int) -> int {
                        return a + b;
                    }
                    """;
                
                FlowModule module = runtime.compile(code, "test3");
                FlowModule.FunctionInfo info = module.getFunctionInfo("add");
                
                boolean ok = info.getName().equals("add") &&
                           info.getReturnType().equals("int") &&
                           info.getParameters().length == 2 &&
                           info.getParameters()[0].getName().equals("a") &&
                           info.getParameters()[0].getType().equals("int") &&
                           info.getParameters()[1].getName().equals("b") &&
                           info.getParameters()[1].getType().equals("int");
                
                if (ok) {
                    System.out.println("✓ test_function_info_with_parameters");
                    passed++;
                } else {
                    System.out.println("✗ test_function_info_with_parameters");
                    failed++;
                }
                module.close();
            } catch (Exception e) {
                System.out.println("✗ test_function_info_with_parameters - " + e.getMessage());
                failed++;
            }
            
            // Test 4: Inspect functions
            try {
                String code = """
                    func add(a: int, b: int) -> int {
                        return a + b;
                    }
                    func greet(name: string) -> string {
                        return "Hello";
                    }
                    """;
                
                FlowModule module = runtime.compile(code, "test4");
                String inspection = module.inspect();
                
                boolean ok = inspection.contains("add(a: int, b: int) -> int") &&
                           inspection.contains("greet(name: string) -> string") &&
                           inspection.contains("2 function(s)");
                
                if (ok) {
                    System.out.println("✓ test_inspect_functions");
                    passed++;
                } else {
                    System.out.println("✗ test_inspect_functions");
                    failed++;
                }
                module.close();
            } catch (Exception e) {
                System.out.println("✗ test_inspect_functions - " + e.getMessage());
                failed++;
            }
            
            // Test 5: Functions still callable after reflection
            try {
                String code = """
                    func add(a: int, b: int) -> int {
                        return a + b;
                    }
                    """;
                
                FlowModule module = runtime.compile(code, "test5");
                
                // Do reflection
                int count = module.getFunctionCount();
                FlowModule.FunctionInfo info = module.getFunctionInfo("add");
                
                // Now call the function
                try (FlowValue a = runtime.createInt(10);
                     FlowValue b = runtime.createInt(20);
                     FlowValue result = module.call(runtime, "add", a, b)) {
                    
                    boolean ok = result.asInt() == 30;
                    if (ok) {
                        System.out.println("✓ test_functions_callable_after_reflection");
                        passed++;
                    } else {
                        System.out.println("✗ test_functions_callable_after_reflection");
                        failed++;
                    }
                }
                module.close();
            } catch (Exception e) {
                System.out.println("✗ test_functions_callable_after_reflection - " + e.getMessage());
                failed++;
            }
            
            // Test 6: Empty module
            try {
                String code = "// Just a comment";
                
                FlowModule module = runtime.compile(code, "test6");
                int count = module.getFunctionCount();
                String[] functions = module.listFunctions();
                String inspection = module.inspect();
                
                boolean ok = count == 0 && 
                           functions.length == 0 &&
                           inspection.toLowerCase().contains("no functions");
                
                if (ok) {
                    System.out.println("✓ test_empty_module");
                    passed++;
                } else {
                    System.out.println("✗ test_empty_module");
                    failed++;
                }
                module.close();
            } catch (Exception e) {
                System.out.println("✗ test_empty_module - " + e.getMessage());
                failed++;
            }
            
        } catch (FlowException e) {
            System.out.println("Failed to initialize runtime: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n============================================================");
        System.out.println("Tests passed: " + passed);
        System.out.println("Tests failed: " + failed);
        System.out.println("============================================================");
        
        System.exit(failed > 0 ? 1 : 0);
    }
}

