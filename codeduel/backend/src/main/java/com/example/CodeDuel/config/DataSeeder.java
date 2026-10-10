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
                        .starterCodePython("import sys\n\ndef two_sum(nums, target):\n    # Write your solution here\n    pass\n\nif __name__ == '__main__':\n    lines = sys.stdin.read().split()\n    if lines:\n        target = int(lines[-1])\n        nums = [int(x) for x in lines[:-1]]\n        res = two_sum(nums, target)\n        if res:\n            print(' '.join(map(str, res)))")
                        .starterCodeJava("import java.util.*;\n\npublic class Solution {\n    public static int[] twoSum(int[] nums, int target) {\n        // Write your solution here\n        return new int[]{};\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        List<Integer> list = new ArrayList<>();\n        while (sc.hasNextInt()) {\n            list.add(sc.nextInt());\n        }\n        if (list.size() > 0) {\n            int target = list.get(list.size() - 1);\n            int[] nums = new int[list.size() - 1];\n            for (int i = 0; i < nums.length; i++) nums[i] = list.get(i);\n            int[] res = twoSum(nums, target);\n            if (res != null && res.length == 2) System.out.println(res[0] + \" \" + res[1]);\n        }\n    }\n}")
                        .starterCodeCpp("#include <iostream>\n#include <vector>\nusing namespace std;\n\nvector<int> twoSum(vector<int>& nums, int target) {\n    // Write your solution here\n    return {};\n}\n\nint main() {\n    vector<int> nums;\n    int val;\n    while (cin >> val) nums.push_back(val);\n    if (!nums.empty()) {\n        int target = nums.back();\n        nums.pop_back();\n        vector<int> res = twoSum(nums, target);\n        if (res.size() == 2) cout << res[0] << \" \" << res[1] << endl;\n    }\n    return 0;\n}")
                        .sampleInput("2 7 11 15 9")
                        .sampleOutput("0 1")
                        .hiddenInputsJson("[\"2 7 11 15 9\", \"3 2 4 6\", \"3 3 6\"]")
                        .hiddenOutputsJson("[\"0 1\", \"1 2\", \"0 1\"]")
                        .build();

                Problem p2 = Problem.builder()
                        .title("Palindrome Number")
                        .difficulty("EASY")
                        .description("Given an integer `x`, return `true` if `x` is a palindrome, and `false` otherwise.\n\n**Example 1:**\nInput: `121`\nOutput: `true` \n\n**Example 2:**\nInput: `-121`\nOutput: `false`")
                        .starterCodePython("import sys\n\ndef is_palindrome(s):\n    # Write your solution here\n    pass\n\nif __name__ == '__main__':\n    lines = sys.stdin.read().split()\n    if lines:\n        val = lines[0]\n        res = is_palindrome(val)\n        print('true' if res else 'false')")
                        .starterCodeJava("import java.util.*;\n\npublic class Solution {\n    public static boolean isPalindrome(String s) {\n        // Write your solution here\n        return false;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        if(sc.hasNext()) {\n            String val = sc.next();\n            boolean res = isPalindrome(val);\n            System.out.println(res ? \"true\" : \"false\");\n        }\n    }\n}")
                        .starterCodeCpp("#include <iostream>\n#include <string>\nusing namespace std;\n\nbool isPalindrome(string s) {\n    // Write your solution here\n    return false;\n}\n\nint main() {\n    string s;\n    if(cin >> s) {\n        bool res = isPalindrome(s);\n        cout << (res ? \"true\" : \"false\");\n    }\n    return 0;\n}")
                        .sampleInput("121")
                        .sampleOutput("true")
                        .hiddenInputsJson("[\"121\", \"-121\", \"10\", \"1221\"]")
                        .hiddenOutputsJson("[\"true\", \"false\", \"false\", \"true\"]")
                        .build();

                Problem p3 = Problem.builder()
                        .title("Valid Anagram")
                        .difficulty("EASY")
                        .description("Given two strings `s` and `t`, return `true` if `t` is an anagram of `s`, and `false` otherwise.\n\n**Example:**\nInput: `anagram nagaram` \nOutput: `true`")
                        .starterCodePython("import sys\n\ndef is_anagram(s, t):\n    # Write your solution here\n    pass\n\nif __name__ == '__main__':\n    lines = sys.stdin.read().split()\n    if len(lines) >= 2:\n        res = is_anagram(lines[0], lines[1])\n        print('true' if res else 'false')")
                        .starterCodeJava("import java.util.*;\n\npublic class Solution {\n    public static boolean isAnagram(String s, String t) {\n        // Write your solution here\n        return false;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        if(sc.hasNext()) {\n            String s = sc.next();\n            String t = sc.next();\n            boolean res = isAnagram(s, t);\n            System.out.println(res ? \"true\" : \"false\");\n        }\n    }\n}")
                        .starterCodeCpp("#include <iostream>\n#include <string>\nusing namespace std;\n\nbool isAnagram(string s, string t) {\n    // Write your solution here\n    return false;\n}\n\nint main() {\n    string s, t;\n    if(cin >> s >> t) {\n        bool res = isAnagram(s, t);\n        cout << (res ? \"true\" : \"false\");\n    }\n    return 0;\n}")
                        .sampleInput("anagram nagaram")
                        .sampleOutput("true")
                        .hiddenInputsJson("[\"anagram nagaram\", \"rat car\", \"listen silent\"]")
                        .hiddenOutputsJson("[\"true\", \"false\", \"true\"]")
                        .build();

                Problem p4 = Problem.builder()
                        .title("Longest Substring Without Repeating Characters")
                        .difficulty("MEDIUM")
                        .description("Given a string `s`, find the length of the longest substring without repeating characters.\n\n**Example:**\nInput: `abcabcbb`\nOutput: `3`")
                        .starterCodePython("import sys\n\ndef length_of_longest_substring(s):\n    # Write your solution here\n    pass\n\nif __name__ == '__main__':\n    lines = sys.stdin.read().split()\n    if lines:\n        print(length_of_longest_substring(lines[0]))")
                        .starterCodeJava("import java.util.*;\n\npublic class Solution {\n    public static int lengthOfLongestSubstring(String s) {\n        // Write your solution here\n        return 0;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        if(sc.hasNext()) {\n            System.out.println(lengthOfLongestSubstring(sc.next()));\n        }\n    }\n}")
                        .starterCodeCpp("#include <iostream>\n#include <string>\nusing namespace std;\n\nint lengthOfLongestSubstring(string s) {\n    // Write your solution here\n    return 0;\n}\n\nint main() {\n    string s;\n    if(cin >> s) {\n        cout << lengthOfLongestSubstring(s) << endl;\n    }\n    return 0;\n}")
                        .sampleInput("abcabcbb")
                        .sampleOutput("3")
                        .hiddenInputsJson("[\"abcabcbb\", \"bbbbb\", \"pwwkew\", \"\"]")
                        .hiddenOutputsJson("[\"3\", \"1\", \"3\", \"0\"]")
                        .build();

                Problem p5 = Problem.builder()
                        .title("Max Consecutive Ones")
                        .difficulty("EASY")
                        .description("Given a binary array `nums`, return the maximum number of consecutive `1`'s in the array.\n\n**Example:**\nInput: `1 1 0 1 1 1`\nOutput: `3`")
                        .starterCodePython("import sys\n\ndef find_max_consecutive_ones(nums):\n    # Write your solution here\n    pass\n\nif __name__ == '__main__':\n    lines = sys.stdin.read().split()\n    if lines:\n        nums = [int(x) for x in lines]\n        print(find_max_consecutive_ones(nums))")
                        .starterCodeJava("import java.util.*;\n\npublic class Solution {\n    public static int findMaxConsecutiveOnes(int[] nums) {\n        // Write your solution here\n        return 0;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        List<Integer> list = new ArrayList<>();\n        while (sc.hasNextInt()) list.add(sc.nextInt());\n        int[] nums = new int[list.size()];\n        for (int i = 0; i < list.size(); i++) nums[i] = list.get(i);\n        System.out.println(findMaxConsecutiveOnes(nums));\n    }\n}")
                        .starterCodeCpp("#include <iostream>\n#include <vector>\nusing namespace std;\n\nint findMaxConsecutiveOnes(vector<int>& nums) {\n    // Write your solution here\n    return 0;\n}\n\nint main() {\n    vector<int> nums;\n    int val;\n    while (cin >> val) nums.push_back(val);\n    cout << findMaxConsecutiveOnes(nums) << endl;\n    return 0;\n}")
                        .sampleInput("1 1 0 1 1 1")
                        .sampleOutput("3")
                        .hiddenInputsJson("[\"1 1 0 1 1 1\", \"1 0 1 1 0 1\", \"0 0 0\", \"1\"]")
                        .hiddenOutputsJson("[\"3\", \"2\", \"0\", \"1\"]")
                        .build();

                repository.saveAll(List.of(p1, p2, p3, p4, p5));
                System.out.println("[DataSeeder] PostgreSQL database initialized with 5 default coding problems.");
            }
        };
    }
}