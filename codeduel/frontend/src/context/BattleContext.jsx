import { createContext, useContext, useEffect, useMemo, useReducer } from 'react';
import { websocket } from '../services/websocket';

const defaultRating = 1250;

const getLocalUser = () => {
  const queryUserId = new URLSearchParams(window.location.search).get('userId')?.trim();
  const storedUserId = sessionStorage.getItem('cd_user_id');
  const userId = queryUserId || storedUserId || `player_${Math.floor(Math.random() * 10000)}`;
  sessionStorage.setItem('cd_user_id', userId);
  return { userId, username: userId, rating: defaultRating };
};

const initialState = {
  user: getLocalUser(),
  queue: { status: 'IDLE', startedAt: null, match: null },
  battle: null,
  opponent: null,
  result: null,
  connection: 'DISCONNECTED',
};

const reducer = (state, action) => {
  switch (action.type) {
    case 'QUEUE_SEARCHING': return { ...state, queue: { status: 'SEARCHING', startedAt: Date.now(), match: null } };
    case 'MATCH_FOUND': return { ...state, queue: { ...state.queue, status: 'MATCH_FOUND', match: action.match }, opponent: { username: action.match.opponentName, rating: Number(action.match.opponentElo), presence: 'ONLINE' } };
    case 'IN_GAME': return { ...state, queue: { ...state.queue, status: 'IN_GAME' } };
    case 'QUEUE_IDLE': return { ...state, queue: { status: 'IDLE', startedAt: null, match: null } };
    case 'BATTLE_LOADED': return { ...state, battle: action.battle, opponent: action.battle.opponent || state.opponent };
    case 'EVENT': {
      const { event, userId, payload = {} } = action;
      if (event === 'BATTLE_FINISHED') return { ...state, result: payload, battle: { ...state.battle, status: 'FINISHED' } };
      if (userId === state.user?.userId) return state;
      const opponent = { ...state.opponent };
      if (event === 'PLAYER_CODING') opponent.activity = 'CODING';
      if (event === 'PLAYER_DISCONNECTED') opponent.presence = 'OFFLINE';
      if (event === 'PLAYER_RECONNECTED' || event === 'PLAYER_JOINED') opponent.presence = 'ONLINE';
      if (event === 'PLAYER_SUBMITTED') {
        opponent.activity = 'SUBMITTED';
        opponent.testsPassed = payload.testsPassed;
        opponent.testsTotal = payload.testsTotal;
      }
      if (event === 'PLAYER_ACCEPTED') opponent.activity = 'ACCEPTED';
      return { ...state, opponent };
    }
    case 'CONNECTION': return { ...state, connection: action.value };
    case 'RESET_BATTLE': return { ...state, battle: null, opponent: null, result: null };
    default: return state;
  }
};

const BattleContext = createContext(null);

export function BattleProvider({ children }) {
  const user = useMemo(() => {
    try {
      const localUser = getLocalUser();
      const savedUser = JSON.parse(localStorage.getItem('cd_user') || '{}');
      return { ...localUser, ...savedUser, userId: localUser.userId, username: localUser.username };
    } catch {
      return getLocalUser();
    }
  }, []);
  const [state, dispatch] = useReducer(reducer, { ...initialState, user });

  useEffect(() => {
    const removeListener = websocket.onStateChange((value) => dispatch({ type: 'CONNECTION', value }));
    websocket.connect();
    return removeListener;
  }, []);

  const value = useMemo(() => ({ state, dispatch }), [state]);
  return <BattleContext.Provider value={value}>{children}</BattleContext.Provider>;
}

export const useBattle = () => {
  const context = useContext(BattleContext);
  if (!context) throw new Error('useBattle must be used inside BattleProvider');
  return context;
};
