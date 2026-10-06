import Editor from '@monaco-editor/react';

const fallback = {
  python: 'def solve():\n    # Write your solution here\n    pass\n\nsolve()\n',
  java: 'import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        // Write your solution here\n    }\n}\n',
  cpp: '#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    // Write your solution here\n    return 0;\n}\n',
};

export default function CodeEditor({ language, problem, value, onChange, disabled }) {
  const starter = problem?.starterCode?.[language] || fallback[language];
  return <div className="h-full min-h-[420px] overflow-hidden rounded-lg border border-slate-700">
    <Editor height="100%" language={language === 'cpp' ? 'cpp' : language} theme="vs-dark"
      value={value ?? starter} onChange={(next) => onChange(next || '')} options={{
        readOnly: disabled, minimap: { enabled: false }, automaticLayout: true, fontSize: 14,
        lineNumbers: 'on', padding: { top: 12 },
      }} />
  </div>;
}
