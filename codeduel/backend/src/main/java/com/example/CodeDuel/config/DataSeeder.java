package com.example.CodeDuel.config;

import com.example.CodeDuel.model.Problem;
import com.example.CodeDuel.repository.ProblemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner initDatabase(ProblemRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                Problem p1 = Problem.builder()
                        .title("Two Sum")
                        .difficulty("EASY")
                        .description("Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.\n\n**Example:**\nInput: `nums = [2, 7, 11, 15]`, `target = 9`\nOutput: `0 1`")
                        .starterCodePython("import sys\n\ndef two_sum(nums, target):\n    # Write your solution here\n    pass\n\nif __name__ == '__main__':\n    lines = sys.stdin.read().split()\n    if lines:\n        target = int(lines[-1])\n        nums = [int(x) for x in lines[:-1]]\n        # Call two_sum and print result")
                        .starterCodeJava("import java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // Implement Two Sum logic here\n    }\n}")
                        .starterCodeCpp("#include <iostream>\n#include <vector>\nusing namespace std;\n\nint main() {\n    // Implement Two Sum logic here\n    return 0;\n}")
                        .sampleInput("2 7 11 15 9")
                        .sampleOutput("0 1")
                        .hiddenInputsJson("[\"2 7 11 15 9\", \"3 2 4 6\"]")
                        .hiddenOutputsJson("[\"0 1\", \"1 2\"]")
                        .build();

                Problem p2 = Problem.builder()
                        .title("Palindrome Number")
                        .difficulty("EASY")
                        .description("Given an integer `x`, return `true` if `x` is a palindrome, and `false` otherwise.\n\n**Example 1:**\nInput: `121`\nOutput: `true` \n\n**Example 2:**\nInput: `-121`\nOutput: `false`")
                        .starterCodePython("import sys\n\nlines = sys.stdin.read().split()\nif lines:\n    val = lines[0]\n    print('true' if val == val[::-1] else 'false')")
                        .starterCodeJava("import java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        if(sc.hasNext()) {\n            String val = sc.next();\n            String rev = new StringBuilder(val).reverse().toString();\n            System.out.println(val.equals(rev) ? \"true\" : \"false\");\n        }\n    }\n}")
                        .starterCodeCpp("#include <iostream>\n#include <string>\n#include <algorithm>\nusing namespace std;\n\nint main() {\n    string s;\n    if(cin >> s) {\n        string r = s;\n        reverse(r.begin(), r.end());\n        cout << (s == r ? \"true\" : \"false\");\n    }\n    return 0;\n}")
                        .sampleInput("121")
                        .sampleOutput("true")
                        .hiddenInputsJson("[\"121\", \"-121\", \"10\"]")
                        .hiddenOutputsJson("[\"true\", \"false\", \"false\"]")
                        .build();

                Problem p3 = Problem.builder()
                        .title("Valid Anagram")
                        .difficulty("EASY")
                        .description("Given two strings `s` and `t`, return `true` if `t` is an anagram of `s`, and `false` otherwise.\n\n**Example:**\nInput: `anagram nagaram` \nOutput: `true`")
                        .starterCodePython("import sys\n\nlines = sys.stdin.read().split()\nif len(lines) >= 2:\n    s, t = lines[0], lines[1]\n    print('true' if sorted(s) == sorted(t) else 'false')")
                        .starterCodeJava("import java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        if(sc.hasNext()) {\n            char[] s = sc.next().toCharArray();\n            char[] t = sc.next().toCharArray();\n            Arrays.sort(s);\n            Arrays.sort(t);\n            System.out.println(Arrays.equals(s, t) ? \"true\" : \"false\");\n        }\n    }\n}")
                        .starterCodeCpp("#include <iostream>\n#include <string>\n#include <algorithm>\nusing namespace std;\n\nint main() {\n    string s, t;\n    if(cin >> s >> t) {\n        sort(s.begin(), s.end());\n        sort(t.begin(), t.end());\n        cout << (s == t ? \"true\" : \"false\");\n    }\n    return 0;\n}")
                        .sampleInput("anagram nagaram")
                        .sampleOutput("true")
                        .hiddenInputsJson("[\"anagram nagaram\", \"rat car\"]")
                        .hiddenOutputsJson("[\"true\", \"false\"]")
                        .build();

                repository.saveAll(List.of(p1, p2, p3));
                System.out.println("[DataSeeder] PostgreSQL database initialized with 3 default coding problems.");
            }
        };
    }
}