import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import axios from 'axios';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export default function BattleArena() {
  const { battleId } = useParams();
  const navigate = useNavigate();

  // Battle & Problem state
  const [loading, setLoading] = useState(true);
  const [problem, setProblem] = useState(null);
  const [opponentId, setOpponentId] = useState('Opponent');
  const [userId, setUserId] = useState('');

  // Editor state
  const [selectedLanguage, setSelectedLanguage] = useState('python');
  const [code, setCode] = useState('');

  // Execution & UI state
  const [submitting, setSubmitting] = useState(false);
  const [activeTab, setActiveTab] = useState('output');
  const [consoleOutput, setConsoleOutput] = useState('Run sample tests or submit your solution to see output here.');
  const [submissionResult, setSubmissionResult] = useState(null);
  const [opponentProgress, setOpponentProgress] = useState(null);

  // Timer state (15 minutes default)
  const [timeLeft, setTimeLeft] = useState(900);

  // 1. Fetch Battle Details & Setup Current User Session
  useEffect(() => {
    const currentUserId = sessionStorage.getItem('userId') || 'player_' + Math.floor(Math.random() * 1000);
    setUserId(currentUserId);

    if (!battleId) return;

    axios.get(`http://localhost:8081/api/battles/${battleId}`)
      .then(res => {
        const data = res.data;
        const prob = data.problem;

        setProblem(prob);
        setCode(prob.starterCodePython || '');

        // Determine opponent ID
        const opp = data.player1Id === currentUserId ? data.player2Id : data.player1Id;
        setOpponentId(opp || 'Opponent');

        setLoading(false);
      })
      .catch(err => {
        console.error("Error fetching battle details:", err);
        setLoading(false);
      });
  }, [battleId]);

  // 2. Battle Timer Countdown Loop
  useEffect(() => {
    if (loading || timeLeft <= 0) return;
    const timer = setInterval(() => {
      setTimeLeft(prev => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [loading, timeLeft]);

  // Format seconds to MM:SS
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // 3. WebSocket Subscription for Live Opponent Progress Updates
  useEffect(() => {
    if (!battleId) return;

    const socket = new SockJS('http://localhost:8081/ws');
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        stompClient.subscribe(`/topic/battle/${battleId}`, (message) => {
          try {
            const payload = JSON.parse(message.body);
            if (payload.type === 'OPPONENT_SUBMISSION' && payload.submitterId !== userId) {
              setOpponentProgress(payload);
            }
          } catch (e) {
            console.error("Error parsing WebSocket message:", e);
          }
        });
      },
    });

    stompClient.activate();

    return () => {
      if (stompClient) stompClient.deactivate();
    };
  }, [battleId, userId]);

  // 4. Handle Programming Language Switch
  const handleLanguageChange = (e) => {
    const lang = e.target.value;
    setSelectedLanguage(lang);
    if (!problem) return;

    if (lang === 'python') setCode(problem.starterCodePython || '');
    else if (lang === 'java') setCode(problem.starterCodeJava || '');
    else if (lang === 'cpp') setCode(problem.starterCodeCpp || '');
  };

  // 5. Handle Solution Submission (to Judge0 via Spring Boot)
  const handleSubmitSolution = async () => {
    setSubmitting(true);
    setActiveTab('output');
    setConsoleOutput("⏳ Submitting solution to Judge0 evaluation sandbox...");
    setSubmissionResult(null);

    try {
      const response = await axios.post('http://localhost:8081/api/submissions', {
        battleId: battleId,
        userId: userId,
        language: selectedLanguage,
        sourceCode: code
      });

      const res = response.data;
      setSubmissionResult(res);

      if (res.verdict === 'ACCEPTED') {
        setConsoleOutput(
          `🎉 ACCEPTED!\n\nPassed: ${res.testsPassed}/${res.totalTests} test cases\nExecution Time: ${res.executionTime}\nMemory Used: ${res.memoryUsed}\n\nLast Output:\n${res.output || 'No stdout'}`
        );
      } else {
        setConsoleOutput(
          `❌ ${res.verdict}\n\nPassed: ${res.testsPassed}/${res.totalTests} test cases\nError Message: ${res.errorMessage || 'Output mismatch'}\n\nOutput:\n${res.output || 'None'}`
        );
      }
    } catch (err) {
      console.error("Submission error:", err);
      setConsoleOutput("⚠️ Submission Failed. Check if Spring Boot backend and Judge0 container are active.");
    } finally {
      setSubmitting(false);
    }
  };

  // 6. Handle Sample Test Run
  const handleRunSampleTests = () => {
    setActiveTab('tests');
    handleSubmitSolution();
  };

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-950 text-slate-100">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-sky-400 border-t-transparent"></div>
          <p className="text-sm font-semibold tracking-wide text-slate-400">Loading Battle Arena...</p>
        </div>
      </div>
    );
  }

  if (!problem) {
    return (
      <div className="flex h-screen flex-col items-center justify-center bg-slate-950 text-slate-100 gap-4">
        <p className="text-lg font-semibold text-rose-400">Battle session or problem not found.</p>
        <button
          onClick={() => navigate('/')}
          className="rounded bg-sky-500 px-4 py-2 text-sm font-semibold hover:bg-sky-400 transition"
        >
          Return to Lobby
        </button>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col bg-slate-950 text-slate-100 font-sans overflow-hidden">
      {/* Navbar Header */}
      <header className="flex h-14 items-center justify-between border-b border-slate-800 bg-slate-900/90 px-6">
        <div className="flex items-center gap-3">
          <span className="text-xl font-black tracking-wider text-sky-400 cursor-pointer" onClick={() => navigate('/')}>
            CodeDuel
          </span>
          <span className="rounded bg-slate-800 px-2 py-0.5 text-xs font-semibold text-slate-400 border border-slate-700">
            1v1 Ranked Duel
          </span>
        </div>

        {/* Live Opponent Status & Countdown Timer */}
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2 text-sm text-slate-300">
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
            </span>
            <span>Opponent: <strong className="text-sky-300">{opponentId}</strong></span>
            {opponentProgress && (
              <span className="ml-2 rounded bg-amber-500/20 px-2 py-0.5 text-xs text-amber-400 border border-amber-500/30">
                Submitted ({opponentProgress.verdict})
              </span>
            )}
          </div>

          <div className={`rounded-md px-3.5 py-1 font-mono text-sm font-bold border ${
            timeLeft < 180 ? 'bg-rose-500/20 text-rose-400 border-rose-500/40 animate-pulse' : 'bg-slate-800 text-amber-400 border-slate-700'
          }`}>
            ⏱️ {formatTime(timeLeft)}
          </div>
        </div>
      </header>

      {/* Main Grid View */}
      <div className="grid flex-1 grid-cols-12 gap-2 p-2 overflow-hidden">
        
        {/* Left Panel: Problem Statement & Output Console */}
        <div className="col-span-5 flex flex-col gap-2 overflow-hidden">
          
          {/* Problem Details Card */}
          <div className="flex-1 rounded-lg border border-slate-800 bg-slate-900/70 p-4 overflow-y-auto">
            <div className="flex items-center justify-between mb-3">
              <h1 className="text-xl font-bold text-slate-100">{problem.title}</h1>
              <span className={`rounded-md px-2.5 py-0.5 text-xs font-bold ${
                problem.difficulty === 'EASY' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' :
                problem.difficulty === 'MEDIUM' ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' :
                'bg-rose-500/20 text-rose-400 border border-rose-500/30'
              }`}>
                {problem.difficulty}
              </span>
            </div>

            <div className="text-sm text-slate-300 whitespace-pre-wrap leading-relaxed border-t border-slate-800 pt-3">
              {problem.description}
            </div>

            {/* Sample Input / Output */}
            {problem.sampleInput && (
              <div className="mt-4 rounded border border-slate-800 bg-slate-950 p-3">
                <span className="text-xs font-semibold text-slate-400 block mb-1">Sample Input</span>
                <pre className="text-xs font-mono text-emerald-400 whitespace-pre-wrap">{problem.sampleInput}</pre>
              </div>
            )}

            {problem.sampleOutput && (
              <div className="mt-2 rounded border border-slate-800 bg-slate-950 p-3">
                <span className="text-xs font-semibold text-slate-400 block mb-1">Sample Output</span>
                <pre className="text-xs font-mono text-emerald-400 whitespace-pre-wrap">{problem.sampleOutput}</pre>
              </div>
            )}
          </div>

          {/* Output / Console Tab */}
          <div className="h-48 rounded-lg border border-slate-800 bg-slate-900/70 flex flex-col overflow-hidden">
            <div className="flex items-center justify-between border-b border-slate-800 bg-slate-900 px-3">
              <div className="flex gap-1">
                <button
                  onClick={() => setActiveTab('output')}
                  className={`px-3 py-1.5 text-xs font-semibold border-b-2 transition ${
                    activeTab === 'output' ? 'border-sky-400 text-sky-400' : 'border-transparent text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Output Console
                </button>
                <button
                  onClick={() => setActiveTab('tests')}
                  className={`px-3 py-1.5 text-xs font-semibold border-b-2 transition ${
                    activeTab === 'tests' ? 'border-sky-400 text-sky-400' : 'border-transparent text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Test Results {submissionResult && `(${submissionResult.testsPassed}/${submissionResult.totalTests})`}
                </button>
              </div>

              {submissionResult && (
                <span className={`text-xs font-bold ${
                  submissionResult.verdict === 'ACCEPTED' ? 'text-emerald-400' : 'text-rose-400'
                }`}>
                  {submissionResult.verdict}
                </span>
              )}
            </div>

            <div className="flex-1 p-3 font-mono text-xs text-slate-300 overflow-y-auto bg-slate-950/80 whitespace-pre-wrap leading-relaxed">
              {consoleOutput}
            </div>
          </div>
        </div>

        {/* Right Panel: Monaco Code Editor */}
        <div className="col-span-7 flex flex-col rounded-lg border border-slate-800 bg-slate-900/70 overflow-hidden">
          
          {/* Editor Header Toolbar */}
          <div className="flex h-11 items-center justify-between border-b border-slate-800 bg-slate-900 px-4">
            <div className="flex items-center gap-3">
              <label className="text-xs text-slate-400 font-medium">Language:</label>
              <select
                value={selectedLanguage}
                onChange={handleLanguageChange}
                className="rounded border border-slate-700 bg-slate-800 px-3 py-1 text-xs text-slate-200 focus:border-sky-400 focus:outline-none"
              >
                <option value="python">Python 3.8</option>
                <option value="java">Java 17</option>
                <option value="cpp">C++ 17</option>
              </select>
            </div>

            <div className="flex items-center gap-2">
              <span className="h-2 w-2 rounded-full bg-sky-400"></span>
              <span className="text-xs text-slate-400">Monaco Editor (VS Dark)</span>
            </div>
          </div>

          {/* Monaco Editor Instance */}
          <div className="flex-1 overflow-hidden">
            <Editor
              height="100%"
              language={selectedLanguage === 'cpp' ? 'cpp' : selectedLanguage}
              theme="vs-dark"
              value={code}
              onChange={(val) => setCode(val || '')}
              options={{
                fontSize: 14,
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                automaticLayout: true,
                padding: { top: 12 },
                tabSize: 4,
              }}
            />
          </div>

          {/* Action Button Footer */}
          <div className="flex h-12 items-center justify-between border-t border-slate-800 bg-slate-900 px-4">
            <span className="text-xs text-slate-500 font-mono">
              Press Submit to execute hidden test cases in Judge0
            </span>

            <div className="flex gap-3">
              <button
                onClick={handleRunSampleTests}
                disabled={submitting}
                className="rounded-md border border-slate-700 bg-slate-800 px-4 py-1.5 text-xs font-semibold text-slate-200 transition hover:bg-slate-700 disabled:opacity-50"
              >
                Run Sample Tests
              </button>

              <button
                onClick={handleSubmitSolution}
                disabled={submitting}
                className="flex items-center gap-2 rounded-md bg-sky-500 px-5 py-1.5 text-xs font-bold text-white shadow-lg transition hover:bg-sky-400 disabled:opacity-50"
              >
                {submitting && (
                  <span className="h-3 w-3 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
                )}
                {submitting ? 'Evaluating...' : 'Submit Solution'}
              </button>
            </div>
          </div>

        </div>

      </div>
    </div>
  );
}