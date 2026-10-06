import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const destinationKey = (destination) => destination;
let client;
let state = 'DISCONNECTED';
let retryDelay = 3000;
const subscriptions = new Map();
const stateListeners = new Set();

const notify = (next) => {
  state = next;
  stateListeners.forEach((listener) => listener(next));
};

const ensureClient = () => {
  if (client) return client;
  client = new Client({
    webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_URL || '/ws'),
    reconnectDelay: retryDelay,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      retryDelay = 3000;
      notify('CONNECTED');
      subscriptions.forEach((entry) => entry.activate());
    },
    onWebSocketClose: () => {
      retryDelay = Math.min(retryDelay * 2, 15000);
      client.reconnectDelay = retryDelay;
      notify('RECONNECTING');
    },
    onStompError: () => notify('RECONNECTING'),
  });
  return client;
};

export const websocket = {
  connect() {
    const socket = ensureClient();
    if (!socket.active) {
      notify(state === 'DISCONNECTED' ? 'RECONNECTING' : state);
      socket.activate();
    }
  },
  disconnect() {
    if (client?.active) client.deactivate();
    subscriptions.clear();
    notify('DISCONNECTED');
  },
  subscribe(destination, handler) {
    const key = destinationKey(destination);
    const existing = subscriptions.get(key);
    if (existing) {
      existing.handlers.add(handler);
      return () => {
        existing.handlers.delete(handler);
        if (!existing.handlers.size) subscriptions.delete(key);
      };
    }
    const entry = {
      handlers: new Set([handler]),
      subscription: null,
      activate() {
        if (!client?.connected) return;
        entry.subscription?.unsubscribe();
        entry.subscription = client.subscribe(destination, (message) => {
          let body;
          try {
            body = JSON.parse(message.body);
          } catch {
            body = message.body;
          }
          entry.handlers.forEach((listener) => listener(body));
        });
      },
    };
    subscriptions.set(key, entry);
    ensureClient();
    entry.activate();
    return () => {
      entry.handlers.delete(handler);
      if (!entry.handlers.size) {
        entry.subscription?.unsubscribe();
        subscriptions.delete(key);
      }
    };
  },
  publish(destination, body) {
    if (!client?.connected) return;
    client.publish({ destination, body: JSON.stringify(body) });
  },
  onStateChange(listener) {
    stateListeners.add(listener);
    listener(state);
    return () => stateListeners.delete(listener);
  },
};
