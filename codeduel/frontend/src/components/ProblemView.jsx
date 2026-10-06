import { Copy } from 'lucide-react';
import ReactMarkdown from 'react-markdown';

const difficulty = { EASY: 'text-success', MEDIUM: 'text-warn', HARD: 'text-danger' };

export default function ProblemView({ problem }) {
  if (!problem) return <div className="panel p-6 text-slate-400">Loading problem…</div>;
  const copy = (value) => navigator.clipboard?.writeText(value);
  return <section className="panel max-h-[55vh] overflow-y-auto p-6">
    <div className="mb-4 flex items-start justify-between gap-3">
      <div><h1 className="text-2xl font-bold">{problem.title}</h1><span className={`text-sm font-semibold ${difficulty[problem.difficulty] || 'text-slate-300'}`}>{problem.difficulty}</span></div>
      <div className="text-right text-xs text-slate-400">Time {problem.timeLimitMs ?? '—'} ms<br />Memory {problem.memoryLimitMb ?? '—'} MB</div>
    </div>
    <div className="prose prose-invert max-w-none text-sm"><ReactMarkdown>{problem.descriptionMd || ''}</ReactMarkdown></div>
    <div className="mt-6 flex gap-2 overflow-x-auto">
      {(problem.samples || []).map((sample, index) => <details key={index} open={index === 0} className="min-w-[240px] rounded-lg bg-slate-900 p-3">
        <summary className="cursor-pointer text-sm font-semibold">Sample {index + 1}</summary>
        <div className="mt-3 grid gap-2 text-xs"><div><div className="mb-1 text-slate-400">Input <button onClick={() => copy(sample.input)} aria-label="Copy input"><Copy size={13} /></button></div><pre className="whitespace-pre-wrap">{sample.input}</pre></div><div><div className="mb-1 text-slate-400">Output <button onClick={() => copy(sample.output)} aria-label="Copy output"><Copy size={13} /></button></div><pre className="whitespace-pre-wrap">{sample.output}</pre></div></div>
      </details>)}
    </div>
  </section>;
}
