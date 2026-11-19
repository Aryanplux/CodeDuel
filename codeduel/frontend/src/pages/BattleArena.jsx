import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import MatchResultModal from '../components/MatchResultModal';
import { useBattle } from '../context/BattleContext';
import { getApiErrorMessage, getBattle, submitCode } from '../services/api';
import Seo from '../components/Seo';
import { useAuth } from '../context/AuthContext';

export default function BattleArena() {
  const { battleId } = useParams();
  const navigate = useNavigate();
  const { state } = useBattle();
  const { user: authenticatedUser } = useAuth();

  // Battle & Problem state
  const [loading, setLoading] = useState(true);
  const [problem, setProblem] = useState(null);
  const [opponentId, setOpponentId] = useState('Opponent');
  const [userId, setUserId] = useState('');

  // Editor state
  const [selectedLanguage, setSelectedLanguage] = useState('python');
  const [code, setCode] = useState('');
  const editorRef = useRef(null);

  // Execution & UI state
  const [submitting, setSubmitting] = useState(false);
  const [activeTab, setActiveTab] = useState('output');
  const [consoleOutput, setConsoleOutput] = useState('Run sample tests or submit your solution to see output here.');
  const [submissionResult, setSubmissionResult] = useState(null);
  const [opponentProgress, setOpponentProgress] = useState(null);
  const [battleResult, setBattleResult] = useState(null);
  const [testCases, setTestCases] = useState([]);
  const [locked, setLocked] = useState(false);
  const [waitingForOpponent, setWaitingForOpponent] = useState(false);
  const [battlePhase, setBattlePhase] = useState('CODING');
  const [waitingDeadline, setWaitingDeadline] = useState(null);
  const [secondsRemaining, setSecondsRemaining] = useState(0);
  const [connectionState, setConnectionState] = useState('CONNECTING');
  const [connectionError, setConnectionError] = useState('');
  const [loadError, setLoadError] = useState('');
  const [submissionStatus, setSubmissionStatus] = useState({
    current: 'CODING',
    opponent: 'CODING',
  });
  const stompClientRef = useRef(null);

  // Timer state (15 minutes default)
  const [timeLeft, setTimeLeft] = useState(900);
  const [battleEndAt, setBattleEndAt] = useState(null);

  // 1. Fetch Battle Details & Setup Current User Session
  useEffect(() => {
    const currentUserId = authenticatedUser?.id
      || sessionStorage.getItem('cd_user_id')
      || 'player_' + Math.floor(Math.random() * 1000);
    setUserId(currentUserId);

    if (!battleId) return;

    getBattle(battleId)
      .then(res => {
        const data = res.data;
        const prob = data.problem;

        setProblem(prob);
        const starter = prob.starterCodePython || '';
        setCode(starter);
        if (editorRef.current) {
          editorRef.current.setValue(starter);
        }

        // Determine opponent ID
        const opp = data.player1Id === currentUserId ? data.player2Id : data.player1Id;
        setOpponentId(opp || 'Opponent');
        if (data.endTimeEpochMillis || data.endTime) {
          const endAt = data.endTimeEpochMillis || new Date(data.endTime).getTime();
          setBattleEndAt(endAt);
          setTimeLeft(Math.max(0, Math.ceil((endAt - Date.now()) / 1000)));
        }

        setLoading(false);
      })
      .catch(err => {
        console.error("Error fetching battle details:", err);
        setLoadError(getApiErrorMessage(err));
        setLoading(false);
      });
  }, [battleId, authenticatedUser?.id]);

  useEffect(() => {
    if (!battleId || !userId) return undefined;
    const leave = () => {
      if (battleResult) return;
      const payload = JSON.stringify({ userId });
      if (navigator.sendBeacon) {
        const apiBase = import.meta.env.VITE_API_URL || '/api';
        navigator.sendBeacon(`${apiBase}/battles/${encodeURIComponent(battleId)}/leave`,
          new Blob([payload], { type: 'application/json' }));
      }
    };
    window.addEventListener('pagehide', leave);
    return () => window.removeEventListener('pagehide', leave);
  }, [battleId, userId, battleResult]);

  // 2. Battle Timer Countdown Loop
  useEffect(() => {
    if (loading || !battleEndAt) return;
    const timer = setInterval(() => {
      setTimeLeft(Math.max(0, Math.ceil((battleEndAt - Date.now()) / 1000)));
    }, 1000);
    return () => clearInterval(timer);
  }, [loading, battleEndAt]);

  // Format seconds to MM:SS
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // 3. WebSocket Subscription for Live Opponent Progress Updates
  useEffect(() => {
    if (!battleId) return undefined;

    const socket = new SockJS(import.meta.env.VITE_WS_URL || '/ws');
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        setConnectionState('CONNECTED');
        setConnectionError('');
        stompClient.subscribe(`/topic/battle/${battleId}`, (message) => {
          try {
            const payload = JSON.parse(message.body);
            if (payload.type === 'PLAYER_SUBMITTED') {
              setWaitingDeadline(payload.waitingDeadline || null);
              if (payload.submitterId === userId) {
                setLocked(true);
                setSubmissionStatus((status) => ({ ...status, current: 'SUBMITTED' }));
                setBattlePhase(payload.submittedCount < payload.totalPlayers ? 'WAITING' : 'JUDGING');
                setWaitingForOpponent(payload.submittedCount < payload.totalPlayers);
              } else {
                setOpponentProgress(payload);
                setSubmissionStatus((status) => ({ ...status, opponent: 'SUBMITTED' }));
                setBattlePhase((phase) => phase === 'WAITING' && payload.submittedCount === payload.totalPlayers
                  ? 'JUDGING'
                  : phase);
                setWaitingForOpponent((current) =>
                  current && payload.submittedCount < payload.totalPlayers
                );
              }
            }
            if (payload.type === 'PLAYER_SUBMISSION_RESULT' && payload.submitterId !== userId) {
              setOpponentProgress(payload);
            }
            if (payload.type === 'SUBMISSION_TIMEOUT') {
              setBattlePhase('JUDGING');
              setWaitingForOpponent(false);
              setSecondsRemaining(0);
            }
            if (payload.type === 'BATTLE_FINISHED') {
              setBattleResult(payload);
              setBattlePhase('REVEAL');
              setWaitingForOpponent(false);
            }
          } catch (e) {
            console.error("Error parsing WebSocket message:", e);
          }
        });
      },
      onWebSocketClose: () => {
        setConnectionState('DISCONNECTED');
        setConnectionError('Connection lost while waiting for the round update.');
      },
      onStompError: () => {
        setConnectionState('ERROR');
        setConnectionError('Live battle updates are unavailable.');
      },
    });

    stompClientRef.current = stompClient;
    stompClient.activate();

    return () => {
      if (stompClient) stompClient.deactivate();
      stompClientRef.current = null;
    };
  }, [battleId, userId]);

  useEffect(() => {
    if (!waitingDeadline || battlePhase !== 'WAITING') return undefined;
    const updateCountdown = () => {
      setSecondsRemaining(Math.max(0, Math.ceil((waitingDeadline - Date.now()) / 1000)));
    };
    updateCountdown();
    const timer = window.setInterval(updateCountdown, 1000);
    return () => window.clearInterval(timer);
  }, [waitingDeadline, battlePhase]);

  // 4. Handle Programming Language Switch
  const handleLanguageChange = (e) => {
    const lang = e.target.value;
    setSelectedLanguage(lang);
    if (!problem) return;

    let newCode = '';
    if (lang === 'python') newCode = problem.starterCodePython || '';
    else if (lang === 'java') newCode = problem.starterCodeJava || '';
    else if (lang === 'cpp') newCode = problem.starterCodeCpp || '';
    
    setCode(newCode);
    if (editorRef.current) {
      editorRef.current.setValue(newCode);
    }
  };

  // 5. Handle Solution Submission (to Judge0 via Spring Boot)
  const handleSubmitSolution = async ({ sampleRun = false } = {}) => {
    if (submitting) return;
    if (locked && !sampleRun) return;
    // Determine active source code from Monaco Editor instance or React state
    const currentCode = editorRef.current ? editorRef.current.getValue() : code;
    const effectiveCode = (currentCode !== undefined && currentCode !== null) ? currentCode : '';

    if (effectiveCode !== code) {
      setCode(effectiveCode);
    }

    // Client-side guard check: prevent blank source code submission
    if (!effectiveCode.trim()) {
      setActiveTab('output');
      setConsoleOutput("⚠️ Source code cannot be blank. Please write your solution before submitting.");
      return;
    }

    const effectiveUserId = userId || sessionStorage.getItem('cd_user_id') || 'player_' + Math.floor(Math.random() * 1000);
    if (!userId) {
      setUserId(effectiveUserId);
    }

    if (!battleId) {
      setActiveTab('output');
      setConsoleOutput("⚠️ Battle ID is missing.");
      return;
    }

    setSubmitting(true);
    if (!sampleRun) {
      setLocked(true);
      setWaitingForOpponent(true);
      setBattlePhase('WAITING');
      setSubmissionStatus((status) => ({ ...status, current: 'SUBMITTED' }));
    }
    setActiveTab(sampleRun ? 'tests' : 'output');
    setConsoleOutput("⏳ Submitting solution to Judge0 evaluation sandbox...");
    setSubmissionResult(null);

    const submissionPayload = {
      battleId: battleId,
      userId: effectiveUserId,
      language: selectedLanguage,
      sourceCode: effectiveCode,
      sampleRun,
    };

    console.info("[BattleArena] Submitting code payload:", {
      battleId: submissionPayload.battleId,
      userId: submissionPayload.userId,
      language: submissionPayload.language,
      sourceCodeLength: submissionPayload.sourceCode.length
    });

    try {
      const response = await submitCode(submissionPayload);

      const res = response.data;
      setSubmissionResult(res);
      setTestCases(res.testCases || []);
      if (res.locked) {
        setLocked(true);
      }

      if (res.verdict === 'ACCEPTED') {
        const aiReview = res.aiReview?.status === 'COMPLETED'
          ? `\n\nAI Quality Review:\nScore: ${res.aiReview.score}/100\nComplexity: ${res.aiReview.complexity}\nFeedback: ${res.aiReview.feedback}`
          : '';
        setConsoleOutput(
          `🎉 ACCEPTED!\n\nPassed: ${res.testsPassed}/${res.totalTests} test cases\nExecution Time: ${res.executionTime}\nMemory Used: ${res.memoryUsed}\n\nLast Output:\n${res.output || 'No stdout'}`
          + aiReview
        );
      } else {
        setConsoleOutput(
          `❌ ${res.verdict}\n\nPassed: ${res.testsPassed}/${res.totalTests} test cases\nError Message: ${res.errorMessage || 'Output mismatch'}\n\nOutput:\n${res.output || 'None'}`
        );
      }
    } catch (err) {
      console.error("[BattleArena] Submission error:", err);
      const serverMsg = err.response?.data?.errorMessage || err.response?.data?.message;
      if (serverMsg) {
        setConsoleOutput(`❌ Submission Error (${err.response?.status || 400}): ${serverMsg}`);
      } else {
        setConsoleOutput("⚠️ Submission Failed. Check if Spring Boot backend and Judge0 container are active.");
      }
      if (!sampleRun && err.response?.status === 409) {
        setLocked(true);
        setWaitingForOpponent(true);
        setBattlePhase('WAITING');
      } else if (!sampleRun) {
        setLocked(false);
        setWaitingForOpponent(false);
        setBattlePhase('CODING');
        setSubmissionStatus((status) => ({ ...status, current: 'CODING' }));
      }
    } finally {
      setSubmitting(false);
    }
  };

  // 6. Handle Sample Test Run
  const handleRunSampleTests = () => {
    handleSubmitSolution({ sampleRun: true });
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
        <p className="text-lg font-semibold text-rose-400">{loadError || 'Battle session or problem not found.'}</p>
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
    <><Seo title="Coding Arena" description="Compete in a real-time CodeDuel 1v1 coding arena with Monaco editing and Judge0 evaluation." /><div className="flex h-screen flex-col bg-bg text-cream font-sans overflow-hidden">
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

            <div className="flex-1 p-3 text-xs text-slate-300 overflow-y-auto bg-slate-950/80 leading-relaxed">
              {activeTab === 'tests' && testCases.length > 0 ? (
                <div className="space-y-2">
                  {testCases.map((testCase) => (
                    <div key={testCase.caseNumber} className="rounded border border-slate-800 bg-slate-900 p-3">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold">Test case {testCase.caseNumber}</span>
                        <span className={testCase.passed ? 'text-emerald-400' : 'text-rose-400'}>
                          {testCase.passed ? 'PASSED' : 'FAILED'}
                        </span>
                      </div>
                      <div className="mt-2 grid gap-2 font-mono text-xs sm:grid-cols-3">
                        <pre className="rounded bg-slate-950 p-2 whitespace-pre-wrap"><b className="font-sans text-slate-400">Input</b>{`\n${testCase.input || '—'}`}</pre>
                        <pre className="rounded bg-slate-950 p-2 whitespace-pre-wrap"><b className="font-sans text-slate-400">Expected</b>{`\n${testCase.expectedOutput || '—'}`}</pre>
                        <pre className="rounded bg-slate-950 p-2 whitespace-pre-wrap"><b className="font-sans text-slate-400">Actual</b>{`\n${testCase.actualOutput || '—'}`}</pre>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <pre className="whitespace-pre-wrap">{consoleOutput}</pre>
              )}
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
                disabled={locked}
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
              onMount={(editor) => {
                editorRef.current = editor;
              }}
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
                disabled={submitting || locked || timeLeft <= 0}
                className="rounded-md border border-slate-700 bg-slate-800 px-4 py-1.5 text-xs font-semibold text-slate-200 transition hover:bg-slate-700 disabled:opacity-50"
              >
                Run Sample Tests
              </button>

              <button
                onClick={handleSubmitSolution}
                disabled={submitting || locked || timeLeft <= 0}
                className="flex items-center gap-2 rounded-md bg-burgundy px-5 py-1.5 text-xs font-bold text-cream shadow-lg transition hover:bg-burgundy/90 disabled:opacity-50"
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
      {locked && !battleResult && (battlePhase === 'WAITING' || battlePhase === 'JUDGING') && (
        <div className="fixed inset-0 z-10 grid place-items-center bg-slate-950/90 p-6 text-center">
          <div className="w-full max-w-lg">
            <div className="mb-6 flex justify-center gap-2 text-xs font-semibold">
              {['Locking', 'Waiting for players', 'Judging', 'Reveal'].map((step, index) => {
                const phases = ['CODING', 'WAITING', 'JUDGING', 'REVEAL'];
                const activeIndex = phases.indexOf(battlePhase);
                return <span key={step} className={`rounded-full px-3 py-1 ${
                  index <= activeIndex ? 'bg-sky-500/30 text-sky-300' : 'bg-slate-800 text-slate-500'
                }`}>{step}</span>;
              })}
            </div>
            <div className="mx-auto mb-5 h-10 w-10 animate-spin rounded-full border-4 border-sky-400 border-t-transparent" />
            <h2 className="text-xl font-bold">
              {battlePhase === 'WAITING'
                ? 'Please wait for another player to submit.'
                : 'Judging submissions…'}
            </h2>
            <p className="mt-2 text-sm text-slate-400">
              {battlePhase === 'WAITING'
                ? `Your solution is locked and being judged. Auto-judging in ${secondsRemaining}s.`
                : 'All players have submitted. Reveal will appear as soon as judging completes.'}
            </p>
            <div className="mt-5 rounded-lg border border-slate-800 bg-slate-900 p-3 text-sm text-slate-300">
              <span>{state?.user?.username || 'You'} {submissionStatus.current === 'SUBMITTED' ? '✓ submitted' : 'coding…'}</span>
              <span className="mx-2 text-slate-600">·</span>
              <span>{opponentId} {submissionStatus.opponent === 'SUBMITTED' ? '✓ submitted' : 'coding…'}</span>
            </div>
          </div>
        </div>
      )}
      {locked && !battleResult && (connectionState === 'DISCONNECTED' || connectionState === 'ERROR') && (
        <div className="fixed bottom-5 left-1/2 z-20 -translate-x-1/2 rounded-lg border border-rose-500/40 bg-rose-950 px-4 py-3 text-center text-sm text-rose-200 shadow-xl">
          <p>{connectionError || 'Live battle connection unavailable.'}</p>
          <button onClick={() => stompClientRef.current?.activate()} className="mt-2 rounded bg-rose-500 px-3 py-1 font-semibold text-white">
            Retry connection
          </button>
        </div>
      )}
      <MatchResultModal result={battleResult} onClose={() => setBattleResult(null)} />
    </div></>
  );
}