import { useEffect, useRef, useState } from 'react';

const colors = { ACCEPTED: 'text-success', WRONG_ANSWER: 'text-danger', COMPILATION_ERROR: 'text-danger', TIME_LIMIT: 'text-warn', RUNTIME_ERROR: 'text-danger' };

export default function ConsoleOutput({ result, onClear }) {
  const [tab, setTab] = useState('Output');
  const ref = useRef(null);
  useEffect(() => { ref.current?.scrollTo({ top: ref.current.scrollHeight }); }, [result]);
  return <section className="panel flex h-44 flex-col overflow-hidden">
    <div className="flex items-center justify-between border-b border-slate-700 px-4 py-2 text-xs"><div className="flex gap-4">{['Output', 'Tests'].map((item) => <button key={item} onClick={() => setTab(item)} className={tab === item ? 'text-sky-400' : 'text-slate-400'}>{item}</button>)}</div><button onClick={onClear} className="text-slate-400 hover:text-white">Clear</button></div>
    <div ref={ref} className="overflow-auto p-4 font-mono text-xs">
      {!result ? <span className="text-slate-500">Run or submit your code to see results.</span> : tab === 'Tests' ? <div><div className={colors[result.verdict] || 'text-slate-300'}>{result.verdict || result.status}</div><div>{result.testsPassed ?? 0}/{result.testsTotal ?? 0} tests passed</div><div>Time: {result.timeMs ?? '—'} ms · Memory: {result.memoryKb ?? '—'} KB</div></div> : <div className="space-y-2"><div className={colors[result.verdict] || 'text-slate-300'}>{result.verdict || result.status}</div>{result.compileOutput && <pre className="text-danger">{result.compileOutput}</pre>}{result.stderr && <pre className="text-danger">{result.stderr}</pre>}{result.stdout && <pre>{result.stdout}</pre>}</div>}
    </div>
  </section>;
}
